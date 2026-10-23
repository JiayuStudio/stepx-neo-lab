// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Divinakra
package local.pokewilds.bugfix;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime support for the "floors" fix. Public because patched game classes call it.
 *
 * PokeWilds keeps every monster (overworld and every floor of every interior) in ONE map, PkmnMap.pokemon,
 * keyed only by position, although each floor has its own tile map. Monsters on different floors that share
 * coordinates therefore block, scan, overwrite and hide each other.
 *
 * This replaces that map with one real map per floor (a {@link FloorMap} for each tile map, all owned by one
 * {@link Registry}). The game's own field PkmnMap.pokemon is always switched to the map of the floor the player
 * is on, so all player, drawing and UI code runs unchanged on a normal single-floor map. Code that belongs to a
 * monster is routed to that monster's own floor ({@link #viewOwner}). A monster is registered in exactly one
 * place; putting it somewhere new removes its previous registration.
 *
 * Setup falls back to the original map when the runtime does not match. Save and load errors fail closed so a
 * damaged exact-placement record cannot be silently replaced with a lossy legacy projection.
 */
public final class Hooks {
    private Hooks() {}

    // ------------------------------------------------------------------ reflection helpers

    static Field open(Class<?> c, String name) throws NoSuchFieldException {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("bugfix.debug"));
    private static volatile boolean disabled;

    private static Class<?> pokemonClass;
    private static Field mapTilesField, interiorIndexField, nicknameField, positionField;

    private static synchronized boolean bindPokemon(Class<?> c) {
        if (pokemonClass == c) return mapTilesField != null;
        try {
            mapTilesField = open(c, "mapTiles");
            interiorIndexField = open(c, "interiorIndex");
        } catch (Throwable t) {
            mapTilesField = null;
            interiorIndexField = null;
            System.err.println("[bugfix] floors: cannot read Pokemon.mapTiles (" + t + "), using original behavior");
            disabled = true;
        }
        pokemonClass = c;
        return mapTilesField != null;
    }

    /** The floor (tile map) a monster lives on, or null if unknown. */
    static Object floorOf(Object pokemon) {
        if (pokemon == null) return null;
        if (pokemonClass != pokemon.getClass() && !bindPokemon(pokemon.getClass())) return null;
        try { return mapTilesField.get(pokemon); } catch (Throwable t) { return null; }
    }

    // ------------------------------------------------------------------ the per-floor maps

    /** All per-floor maps of one PkmnMap. */
    static final class Registry {
        final Object pkmnMap;
        final IdentityHashMap<Object, FloorMap> byFloor = new IdentityHashMap<Object, FloorMap>();
        /** monsters -> {FloorMap, key}: where each monster is registered (at most one place). */
        final IdentityHashMap<Object, Object[]> where = new IdentityHashMap<Object, Object[]>();
        final Field fPokemon, fTiles, fOverworld, fRefreshDrawn, fRefreshTiles;
        int nextSeq;
        long mod;                       // bumped on every change, so cached save views can be reused
        SaveView cachedSave;
        long cachedSaveMod = -1;

        Registry(Object pkmnMap) throws NoSuchFieldException {
            this.pkmnMap = pkmnMap;
            Class<?> c = pkmnMap.getClass();
            fPokemon = open(c, "pokemon");
            fTiles = open(c, "tiles");
            fOverworld = open(c, "overworldTiles");
            fRefreshDrawn = open(c, "refreshOnscreenPokemon");
            fRefreshTiles = open(c, "refreshCache");
        }

        Object overworld() { try { return fOverworld.get(pkmnMap); } catch (Throwable t) { return null; } }
        Object currentTiles() { try { return fTiles.get(pkmnMap); } catch (Throwable t) { return null; } }

        FloorMap floorMap(Object floor) {
            if (floor == null) floor = currentTiles();
            FloorMap m = byFloor.get(floor);
            if (m == null) { m = new FloorMap(this, floor); byFloor.put(floor, m); }
            return m;
        }

        /** Points the game's PkmnMap.pokemon at the map of the floor the player is on. True if it changed. */
        boolean syncField() {
            try {
                FloorMap cur = floorMap(currentTiles());
                if (fPokemon.get(pkmnMap) == cur) return false;
                fPokemon.set(pkmnMap, cur);
                return true;
            } catch (Throwable t) { return false; }
        }

        void requestRedraw() {
            try { fRefreshDrawn.setBoolean(pkmnMap, true); fRefreshTiles.setBoolean(pkmnMap, true); } catch (Throwable t) { /* ignore */ }
        }

        void clearAll() {
            for (FloorMap m : byFloor.values()) m.rawClear();
            where.clear();
            mod++;
        }

        List<FloorMap> inOrder() {
            ArrayList<FloorMap> l = new ArrayList<FloorMap>(byFloor.values());
            Collections.sort(l, new Comparator<FloorMap>() { public int compare(FloorMap a, FloorMap b) { return a.seq - b.seq; } });
            return l;
        }
    }

    /** The registrations of one floor. Behaves like the game's original HashMap, plus the rules described above. */
    static final class FloorMap extends HashMap<Object, Object> {
        private static final long serialVersionUID = 1L;
        final transient Registry reg;
        final transient Object floor;
        final int seq;

        FloorMap(Registry reg, Object floor) { this.reg = reg; this.floor = floor; this.seq = reg.nextSeq++; }

        void rawClear() { super.clear(); }
        Object rawRemove(Object k) { return super.remove(k); }

        @Override public Object put(Object k, Object v) {
            if (v == null) return super.put(k, v);
            Object vf = floorOf(v);
            if (vf != null && vf != floor) return reg.floorMap(vf).put(k, v);        // a monster belongs on its own floor
            Object[] w = reg.where.get(v);
            if (w != null && (w[0] != this || !w[1].equals(k))) {                    // it was registered somewhere else: drop that
                FloorMap wm = (FloorMap) w[0];
                if (wm.get(w[1]) == v) wm.rawRemove(w[1]);
                if (DEBUG) log("TRAIL removed: " + describe(v) + " was also registered at " + w[1] + " (now " + k + ")");
            }
            Object prev = super.put(k, v);
            if (prev != null && prev != v) reg.where.remove(prev);                   // replaced a same-floor entry, like the original
            reg.where.put(v, new Object[] {this, k});
            reg.mod++;
            return prev;
        }

        @Override public void putAll(Map<?, ?> m) { for (Map.Entry<?, ?> e : m.entrySet()) put(e.getKey(), e.getValue()); }

        @Override public Object remove(Object k) {
            Object r = super.remove(k);
            if (r != null) {
                Object[] w = reg.where.get(r);
                if (w != null && w[0] == this) reg.where.remove(r);
                reg.mod++;
            }
            return r;
        }

        /** The game only clears the map when it (re)loads the whole world, so this clears every floor. */
        @Override public void clear() { reg.clearAll(); }
    }

    // ------------------------------------------------------------------ entry points called by patched game code

    /** Replaces the initialisation of PkmnMap.pokemon in PkmnMap's constructor. */
    public static Map newPokemonMap(Map original, Object pkmnMap) {
        if (disabled) return original;
        try {
            Registry r = new Registry(pkmnMap);
            FloorMap m = new FloorMap(r, r.overworld());
            r.byFloor.put(r.overworld(), m);
            return m;
        } catch (Throwable t) {
            System.err.println("[bugfix] floors: could not set up per-floor maps (" + t + "), using original behavior");
            disabled = true;
            return original;
        }
    }

    private static Registry registryOf(Object pkmnMap) {
        try {
            Object m = open(pkmnMap.getClass(), "pokemon").get(pkmnMap);
            return m instanceof FloorMap ? ((FloorMap) m).reg : null;
        } catch (Throwable t) { return null; }
    }

    /** Called right after PkmnMap.tiles is assigned (the player changed floor, or a map was loaded). */
    public static void tilesChanged(Object pkmnMap) {
        if (disabled || pkmnMap == null) return;
        Registry r = registryOf(pkmnMap);
        if (r == null) return;
        if (r.syncField()) {
            r.requestRedraw();
            if (DEBUG) { log("player is now on " + label(r, r.currentTiles()) + " (" + ((FloorMap) r.floorMap(r.currentTiles())).size() + " registered Pokemon here)"); audit(r); }
        }
    }

    /** For code that belongs to a monster: the map of that monster's own floor. {@code m} is what the code read from the field. */
    public static Map viewOwner(Map m, Object owner) {
        if (!(m instanceof FloorMap) || owner == null) return m;
        Object f = floorOf(owner);
        if (f == null) return m;
        FloorMap fm = (FloorMap) m;
        return fm.floor == f ? fm : fm.reg.floorMap(f);
    }

    /** For world-level code (day/night spawning, world generation): always the overworld. */
    public static Map viewOverworld(Map m) {
        if (!(m instanceof FloorMap)) return m;
        FloorMap fm = (FloorMap) m;
        Object ow = fm.reg.overworld();
        return ow == null ? m : fm.reg.floorMap(ow);
    }

    /** For world regeneration: clear() and values() cover every floor; everything else is the overworld. */
    public static Map viewAllFloors(Map m) {
        if (!(m instanceof FloorMap)) return m;
        return new AllFloors(((FloorMap) m).reg);
    }

    static final class AllFloors extends AbstractMap<Object, Object> {
        final Registry reg;
        AllFloors(Registry reg) { this.reg = reg; }
        private Map<Object, Object> ow() { return reg.floorMap(reg.overworld()); }
        @Override public Object get(Object k) { return ow().get(k); }
        @Override public boolean containsKey(Object k) { return ow().containsKey(k); }
        @Override public Object put(Object k, Object v) { return ow().put(k, v); }
        @Override public Object remove(Object k) { return ow().remove(k); }
        @Override public void clear() { reg.clearAll(); }
        @Override public Set<Map.Entry<Object, Object>> entrySet() { return ow().entrySet(); }
        @Override public Collection<Object> values() {
            ArrayList<Object> all = new ArrayList<Object>();
            for (FloorMap m : reg.inOrder()) all.addAll(m.values());
            return all;
        }
    }

    /**
     * Replaces the read of Pokemon.interiorIndex when saving a Pokemon. The field is only kept up to date for
     * monsters the player dropped, so e.g. an egg laid on floor 5 was saved as floor 100 (the first floor).
     * The floor is derived from the tile map the monster is really on.
     */
    public static int floorIndex(Object pokemon) {
        int stored = 0;
        try {
            if (pokemon == null) return 0;
            if (pokemonClass != pokemon.getClass() && !bindPokemon(pokemon.getClass())) return 0;
            stored = interiorIndexField.getInt(pokemon);
            Object tiles = mapTilesField.get(pokemon);
            List<?> layers = env.interiorLayers();
            if (tiles == null || layers == null) return stored;
            for (int i = 0; i < layers.size(); i++) if (layers.get(i) == tiles) return i;
        } catch (Throwable t) { /* fall through */ }
        return stored;
    }

    // ------------------------------------------------------------------ saving

    /**
     * For the save code: every monster exactly once, under a unique position. The save file is keyed by position
     * only, but monsters on different floors share coordinates, so a monster whose position is already taken is
     * written to the nearest free tile of its own floor instead (walkable if possible). This is only a legacy
     * projection; writeJsonZip also records every exact placement in the same save ZIP.
     */
    public static Map viewSave(Map m) {
        if (!(m instanceof FloorMap)) return m;
        try {
            Registry r = ((FloorMap) m).reg;
            if (r.cachedSave == null || r.cachedSaveMod != r.mod) { r.cachedSave = new SaveView(r); r.cachedSaveMod = r.mod; }
            return r.cachedSave;
        } catch (Throwable t) {
            throw new IllegalStateException("[bugfix] floors: cannot create legacy save projection", t);
        }
    }

    static final class SaveView extends AbstractMap<Object, Object> {
        private final LinkedHashMap<Object, Object> out = new LinkedHashMap<Object, Object>();

        SaveView(Registry reg) throws Exception {
            List<FloorMap> maps = reg.inOrder();
            for (FloorMap fm : maps) {
                for (Map.Entry<Object, Object> e : fm.entrySet()) {
                    Object key = e.getKey(), p = e.getValue();
                    if (!out.containsKey(key)) { out.put(key, p); continue; }
                    Object moved = nearestFree(key, fm.floor);
                    if (moved != null) out.put(moved, p);
                    else System.err.println("[bugfix] no free legacy tile for " + describe(p) + "; exact floor save still retains it");
                }
            }
        }

        private Object nearestFree(Object key, Object floor) throws Exception {
            Class<?> vc = key.getClass();
            Field fx = vc.getField("x"), fy = vc.getField("y");
            float x = fx.getFloat(key), y = fy.getFloat(key);
            java.lang.reflect.Constructor<?> ctor = vc.getConstructor(float.class, float.class);
            Field solid = null;
            boolean haveTiles = floor instanceof Map && !((Map) floor).isEmpty();
            Object firstSolidTile = null, firstVoid = null;
            for (int r = 1; r <= 16; r++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                        Object cand = ctor.newInstance(x + dx * 16, y + dy * 16);
                        if (out.containsKey(cand)) continue;
                        if (!haveTiles) return cand;
                        Object tile = ((Map) floor).get(cand);
                        if (tile == null) { if (firstVoid == null) firstVoid = cand; continue; }
                        if (solid == null) solid = open(tile.getClass(), "isSolid");
                        if (!solid.getBoolean(tile)) return cand;
                        if (firstSolidTile == null) firstSolidTile = cand;
                    }
                }
                if (r >= 8 && firstSolidTile != null) break;
            }
            return firstSolidTile != null ? firstSolidTile : firstVoid;
        }

        @Override public Set<Map.Entry<Object, Object>> entrySet() { return out.entrySet(); }
        @Override public Object get(Object key) { return out.get(key); }
        @Override public boolean containsKey(Object key) { return out.containsKey(key); }
        @Override public int size() { return out.size(); }
        @Override public Set<Object> keySet() { return out.keySet(); }
        @Override public Collection<Object> values() { return out.values(); }
    }

    // ------------------------------------------------------------------ lossless map save format

    /** An extra entry in map*.json.zip. Old game versions ignore it and read the legacy data.json projection. */
    public static final String EXACT_ENTRY = "floor-pokemon.v1";
    private static final java.util.WeakHashMap<Object, Boolean> restoredData = new java.util.WeakHashMap<Object, Boolean>();

    private static Object gson() throws Exception {
        Class<?> json = Class.forName("com.pkmngen.game.util.Json", true, Hooks.class.getClassLoader());
        return json.getField("gson").get(null);
    }

    private static Class<?> gameClass(String name) throws Exception {
        return Class.forName("com.pkmngen.game." + name, true, Hooks.class.getClassLoader());
    }

    private static int floorNumber(Registry reg, Object floor, List<?> layers) {
        if (floor == reg.overworld()) return -1;
        for (int i = 0; i < layers.size(); i++) if (floor == layers.get(i)) return i;
        throw new IllegalStateException("Pokemon belongs to a floor absent from this map");
    }

    static final class ExactRecord {
        final int floor;
        final Object key;
        final Object data;
        ExactRecord(int floor, Object key, Object data) { this.floor = floor; this.key = key; this.data = data; }
    }

    private static List<ExactRecord> exactRecords(Registry reg) throws Exception {
        List<?> layers = (List<?>) open(reg.pkmnMap.getClass(), "interiorTiles").get(reg.pkmnMap);
        Class<?> pokemon = gameClass("Pokemon"), data = gameClass("Network$PokemonData");
        Constructor<?> makeData = data.getConstructor(pokemon);
        ArrayList<ExactRecord> records = new ArrayList<ExactRecord>();
        for (FloorMap floor : reg.inOrder()) for (Map.Entry<Object, Object> entry : floor.entrySet()) {
            Object key = entry.getKey(), mon = entry.getValue();
            if (key == null || mon == null || floorOf(mon) != floor.floor) throw new IllegalStateException("Inconsistent Pokemon floor registration");
            int index = floorNumber(reg, floor.floor, layers);
            Object datum = makeData.newInstance(mon);
            open(data, "position").set(datum, key);
            open(data, "isInterior").setBoolean(datum, index >= 0);
            if (index >= 0) open(data, "interiorIndex").setInt(datum, index);
            records.add(new ExactRecord(index, key, datum));
        }
        return records;
    }

    private static boolean overlaps(List<ExactRecord> records) {
        HashSet<Object> keys = new HashSet<Object>();
        for (ExactRecord record : records) if (!keys.add(record.key)) return true;
        return false;
    }

    private static void writeExact(ZipOutputStream zip, List<ExactRecord> records, Object gson) throws Exception {
        zip.putNextEntry(new ZipEntry(EXACT_ENTRY));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8));
        Method toJson = gson.getClass().getMethod("toJson", Object.class);
        writer.write("1\n");
        writer.write(Integer.toString(records.size())); writer.write('\n');
        for (ExactRecord record : records) {
            Field fx = record.key.getClass().getField("x"), fy = record.key.getClass().getField("y");
            String json = (String) toJson.invoke(gson, record.data);
            writer.write(Integer.toString(record.floor)); writer.write('\t');
            writer.write(Float.toString(fx.getFloat(record.key))); writer.write('\t');
            writer.write(Float.toString(fy.getFloat(record.key))); writer.write('\t');
            writer.write(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
            writer.write('\n');
        }
        writer.flush();
        zip.closeEntry();
    }

    /** Replaces Save.saveJson. The normal JSON entry is unchanged; an atomic ZIP replace keeps both entries together. */
    public static void writeJsonZip(Object data, String path) {
        Path target = Paths.get(path), temporary = null;
        try {
            List<ExactRecord> exact = null;
            if (data.getClass().getName().equals("com.pkmngen.game.Network$MapSaveData")) {
                Object map = ((GameEnv) env).mapObject();
                Registry reg = registryOf(map);
                if (reg == null) throw new IllegalStateException("floor maps unavailable while saving");
                List<ExactRecord> all = exactRecords(reg);
                if (overlaps(all)) exact = all;
            }
            Object gson = gson();
            Path parent = target.toAbsolutePath().getParent();
            temporary = Files.createTempFile(parent, "map-save-", ".zip.tmp");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                zip.putNextEntry(new ZipEntry("data.json"));
                OutputStreamWriter writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
                gson.getClass().getMethod("toJson", Object.class, Appendable.class).invoke(gson, data, writer);
                writer.flush();
                zip.closeEntry();
                if (exact != null) writeExact(zip, exact, gson);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            if (exact != null) System.err.println("[bugfix] floors: saved " + exact.size() + " exact floor placements in " + path);
        } catch (Throwable t) {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Throwable ignored) { }
            throw saveFailure(t, path);
        }
    }

    private static RuntimeException saveFailure(Throwable cause, String path) {
        Throwable exception;
        try {
            Class<?> format = Class.forName("com.pkmngen.game.util.Save$Format", true, Hooks.class.getClassLoader());
            Object gsonFormat = Enum.valueOf((Class) format, "GSON");
            Class<?> failure = Class.forName("com.pkmngen.game.util.Save$SaveFailure", true, Hooks.class.getClassLoader());
            exception = (Throwable) failure.getConstructor(Throwable.class, String.class, format).newInstance(cause, path, gsonFormat);
        }
        catch (Throwable reflectionFailure) { return new IllegalStateException("Failed to save " + path, cause); }
        return Hooks.<RuntimeException>sneakyThrow(exception);
    }

    @SuppressWarnings("unchecked") private static <T extends Throwable> RuntimeException sneakyThrow(Throwable t) throws T { throw (T) t; }

    private static List<ExactRecord> readExact(ZipFile zip, Object map) throws Exception {
        ZipEntry entry = zip.getEntry(EXACT_ENTRY);
        if (entry == null) return null;
        List<?> layers = (List<?>) open(map.getClass(), "interiorTiles").get(map);
        Class<?> vector = Class.forName("com.badlogic.gdx.math.Vector2", true, Hooks.class.getClassLoader());
        Class<?> pokemonData = gameClass("Network$PokemonData");
        Constructor<?> point = vector.getConstructor(float.class, float.class);
        Object gson = gson();
        Method fromJson = gson.getClass().getMethod("fromJson", String.class, Class.class);
        ArrayList<ExactRecord> records = new ArrayList<ExactRecord>();
        HashSet<String> occupied = new HashSet<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
            if (!"1".equals(reader.readLine())) throw new IllegalStateException("Unknown floor save version");
            int count = Integer.parseInt(reader.readLine());
            if (count < 0 || count > 1000000) throw new IllegalStateException("Invalid floor save count: " + count);
            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                if (line == null) throw new IllegalStateException("Truncated floor save");
                String[] parts = line.split("\\t", 4);
                if (parts.length != 4) throw new IllegalStateException("Malformed floor save record");
                int floor = Integer.parseInt(parts[0]);
                if (floor < -1 || floor >= layers.size() || floor >= 0 && layers.get(floor) == null) throw new IllegalStateException("Invalid floor index: " + floor);
                float x = Float.parseFloat(parts[1]), y = Float.parseFloat(parts[2]);
                if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalStateException("Invalid Pokemon position");
                Object key = point.newInstance(x, y);
                if (!occupied.add(floor + ":" + x + ":" + y)) throw new IllegalStateException("Duplicate Pokemon on one floor");
                String json = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
                Object datum = fromJson.invoke(gson, json, pokemonData);
                if (datum == null) throw new IllegalStateException("Missing Pokemon data");
                open(pokemonData, "position").set(datum, key);
                open(pokemonData, "isInterior").setBoolean(datum, floor >= 0);
                if (floor >= 0) open(pokemonData, "interiorIndex").setInt(datum, floor);
                records.add(new ExactRecord(floor, key, datum));
            }
            if (reader.readLine() != null) throw new IllegalStateException("Unexpected data after floor save records");
        }
        return records;
    }

    /** Called after map tiles are rebuilt and the legacy Pokémon map has been cleared. */
    public static java.util.HashMap restoreExact(java.util.HashMap legacy, Object mapData, Object map, Object game) {
        String path = null;
        try {
            path = open(map.getClass(), "id").get(map) + ".sav/map" + open(map.getClass(), "currMapId").get(map) + ".json.zip";
            Registry reg = registryOf(map);
            if (reg == null) throw new IllegalStateException("floor maps unavailable while loading");
            // The stock loader reads this field twice: once for keys, once for each value.
            if (restoredData.containsKey(mapData)) return new java.util.HashMap();
            try (ZipFile zip = new ZipFile(path)) {
                List<ExactRecord> records = readExact(zip, map);
                if (records == null) return legacy;
                if (records.size() < legacy.size()) throw new IllegalStateException("Exact floor save has fewer Pokemon than the legacy projection");
                Class<?> pokemon = gameClass("Pokemon"), base = gameClass("Network$PokemonDataBase");
                Constructor<?> makePokemon = pokemon.getConstructor(base);
                Class<?> standing = gameClass("Pokemon$Standing"), action = gameClass("Action");
                Constructor<?> makeStanding = standing.getConstructor(pokemon);
                Method insert = game.getClass().getMethod("insertAction", action);
                List<?> layers = (List<?>) open(map.getClass(), "interiorTiles").get(map);
                ArrayList<Object[]> ready = new ArrayList<Object[]>();
                for (ExactRecord record : records) {
                    Object mon = makePokemon.newInstance(record.data);
                    Object floor = record.floor < 0 ? reg.overworld() : layers.get(record.floor);
                    if (floorOf(mon) != floor) throw new IllegalStateException("Pokemon floor disagrees with exact save");
                    open(pokemon, "position").set(mon, record.key);
                    Object act = makeStanding.newInstance(mon);
                    ready.add(new Object[] {floor, record.key, mon, act});
                }
                for (Object[] row : ready) {
                    reg.floorMap(row[0]).put(row[1], row[2]);
                    insert.invoke(game, row[3]);
                }
                restoredData.put(mapData, Boolean.TRUE);
                System.err.println("[bugfix] floors: restored " + ready.size() + " exact floor placements from " + path);
                return new java.util.HashMap();
            }
        } catch (Throwable t) {
            throw new IllegalStateException("Cannot safely load exact floor placements from " + path, t);
        }
    }

    // ------------------------------------------------------------------ Game state access (floor numbers, diagnostics)

    /** Access to game state used for floor numbers; replaced in tests. */
    public interface Env {
        /** game.map.interiorTiles, or null. */
        List<?> interiorLayers();
    }

    static final class GameEnv implements Env {
        private Field staticGame, map, interiorTiles;
        private boolean resolved, broken;

        Object mapObject() throws Exception {
            if (!resolved) {
                resolved = true;
                ClassLoader cl = Hooks.class.getClassLoader();
                Class<?> game = Class.forName("com.pkmngen.game.Game", false, cl);
                Class<?> pkmnMap = Class.forName("com.pkmngen.game.PkmnMap", false, cl);
                staticGame = open(game, "staticGame");
                map = open(game, "map");
                interiorTiles = open(pkmnMap, "interiorTiles");
            }
            Object g = staticGame.get(null);
            return g == null ? null : map.get(g);
        }

        @Override public List<?> interiorLayers() {
            if (broken) return null;
            try {
                Object m = mapObject();
                Object l = m == null ? null : interiorTiles.get(m);
                return l instanceof List ? (List<?>) l : null;
            } catch (Throwable t) { broken = true; return null; }
        }
    }

    static Env env = new GameEnv();

    // ------------------------------------------------------------------ diagnostics (-Dbugfix.debug=true)

    private static final java.util.HashSet<String> LOGGED = new java.util.HashSet<String>();
    private static long lastClear;

    static void log(String s) {
        long now = System.currentTimeMillis();
        if (now - lastClear > 20000) { LOGGED.clear(); lastClear = now; }
        if (LOGGED.size() < 5000 && LOGGED.add(s)) System.err.println("[bugfix:debug] " + s);
    }

    static String label(Registry r, Object floor) {
        if (floor == null) return "unknown";
        if (floor == r.overworld()) return "overworld";
        List<?> layers = env.interiorLayers();
        if (layers != null) for (int i = 0; i < layers.size(); i++) if (layers.get(i) == floor) return "interior#" + i;
        return "other";
    }

    static String describe(Object p) {
        if (p == null) return "null";
        try {
            if (nicknameField == null) { nicknameField = open(p.getClass(), "nickname"); positionField = open(p.getClass(), "position"); }
            return nicknameField.get(p) + "@" + positionField.get(p);
        } catch (Throwable t) { return p.getClass().getSimpleName(); }
    }

    /** Lists registrations of the current floor whose monster is not near its registered tile (possible phantoms). */
    static void audit(Registry r) {
        try {
            FloorMap cur = r.floorMap(r.currentTiles());
            if (positionField == null) return;
            for (Map.Entry<Object, Object> e : cur.entrySet()) {
                Object k = e.getKey(), p = e.getValue();
                Object pos = positionField.get(p);
                double d = Math.max(Math.abs(((Number) pos.getClass().getField("x").get(pos)).doubleValue() - ((Number) k.getClass().getField("x").get(k)).doubleValue()),
                        Math.abs(((Number) pos.getClass().getField("y").get(pos)).doubleValue() - ((Number) k.getClass().getField("y").get(k)).doubleValue()));
                if (d > 32) log("AUDIT: " + describe(p) + " is registered at " + k + " which is " + (int) d + "px from where it stands");
            }
        } catch (Throwable t) { /* diagnostics only */ }
    }
}
