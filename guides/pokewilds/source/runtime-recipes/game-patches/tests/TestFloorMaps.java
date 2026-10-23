package local.pokewilds.bugfix;

import java.util.*;

/** Logic tests of the per-floor maps (Hooks) with a fake PkmnMap, no game needed. */
public class TestFloorMaps {
    public static class Pt { public float x, y; public Pt(float x, float y) { this.x = x; this.y = y; }
        public boolean equals(Object o) { return o instanceof Pt && ((Pt) o).x == x && ((Pt) o).y == y; }
        public int hashCode() { return Float.hashCode(x) * 31 + Float.hashCode(y); } public String toString() { return "(" + x + "," + y + ")"; } }
    public static class FakeTile { public boolean isSolid; public FakeTile(boolean s) { isSolid = s; } }
    /** stands in for com.pkmngen.game.PkmnMap: only the fields Hooks touches */
    public static class FakeMap { public Object pokemon; public Object tiles; public Object overworldTiles; public boolean refreshOnscreenPokemon, refreshCache; }
    public static class Mon { public Object mapTiles; public int interiorIndex = 100; public String nickname; public Pt position; Mon(String n, Object f) { nickname = n; mapTiles = f; } public String toString() { return nickname; } }
    static int fail = 0;
    static void check(boolean ok, String msg) { System.out.println((ok ? "PASS " : "FAIL ") + msg); if (!ok) fail++; }
    static Pt p(int x, int y) { return new Pt(x * 16, y * 16); }
    static Map<Object, Object> tiles(int w, int h) { Map<Object, Object> m = new HashMap<>(); for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) m.put(p(x, y), new FakeTile(false)); return m; }
    static Map field(FakeMap fm) { return (Map) fm.pokemon; }

    public static void main(String[] a) {
        final Map<Object, Object> OW = tiles(8, 8), F2 = tiles(8, 8), F10 = tiles(8, 8), F5 = tiles(8, 8);
        final List<Object> layers = new ArrayList<Object>(Arrays.asList(null, null, F2, F5, F10));
        Hooks.env = new Hooks.Env() { public List<?> interiorLayers() { return layers; } };

        FakeMap gm = new FakeMap(); gm.overworldTiles = OW; gm.tiles = OW;
        gm.pokemon = Hooks.newPokemonMap(new HashMap(), gm);
        check(gm.pokemon instanceof Hooks.FloorMap, "constructor hook installs a per-floor map for the overworld");

        Mon g2 = new Mon("ghastly", F2), h10 = new Mon("hooh", F10), ow = new Mon("wild", OW);
        // 1. registration goes to the monster's own floor, whichever map the caller holds
        field(gm).put(p(1, 1), g2);
        field(gm).put(p(1, 1), h10);          // same coordinates, another floor: no collision
        field(gm).put(p(1, 1), ow);
        check(field(gm).get(p(1, 1)) == ow && field(gm).size() == 1, "overworld map holds only the overworld Pokemon");
        check(Hooks.viewOwner(field(gm), g2).get(p(1, 1)) == g2 && Hooks.viewOwner(field(gm), h10).get(p(1, 1)) == h10, "each floor keeps its own Pokemon at the shared coordinates");
        check(Hooks.viewOwner(field(gm), g2).get(p(2, 2)) == null, "an empty tile is empty");

        // 2. the Ho-Oh case: floor 2's ghastly must not see floor 10's monsters
        Mon far10 = new Mon("far", F10); Hooks.viewOwner(field(gm), far10).put(p(3, 3), far10);
        check(!Hooks.viewOwner(field(gm), g2).containsKey(p(3, 3)), "floor 2 does not see floor 10's Pokemon");
        check(Hooks.viewOwner(field(gm), far10).containsKey(p(3, 3)), "floor 10 sees it");

        // 3. the player's field follows the floor
        gm.tiles = F2; Hooks.tilesChanged(gm);
        check(gm.pokemon == Hooks.viewOwner(field(gm), g2) && field(gm).get(p(1, 1)) == g2, "after the player goes to floor 2 the game's field IS floor 2's map");
        check(gm.refreshOnscreenPokemon && gm.refreshCache, "drawn list and tile cache are asked to rebuild on a floor change");
        check(field(gm).get(p(3, 3)) == null, "the player on floor 2 sees no Pokemon from floor 10 (no invisible collision)");
        gm.refreshOnscreenPokemon = gm.refreshCache = false; Hooks.tilesChanged(gm);
        check(!gm.refreshOnscreenPokemon, "no needless rebuild when the floor did not change");
        gm.tiles = OW; Hooks.tilesChanged(gm);
        check(field(gm).get(p(1, 1)) == ow, "back outside: the overworld map again");

        // 4. at most one registration per monster: a leftover trail is removed
        Mon t = new Mon("trail", F2); gm.tiles = F2; Hooks.tilesChanged(gm);
        field(gm).put(p(4, 4), t); field(gm).put(p(5, 4), t);                 // registered twice in the original game (no remove in between)
        check(!field(gm).containsKey(p(4, 4)) && field(gm).get(p(5, 4)) == t, "registering a Pokemon again removes its old registration (no trail)");
        check(Hooks.viewOwner(field(gm), t).size() == 2, "floor 2 holds exactly its 2 Pokemon (ghastly and the re-registered one) " + Hooks.viewOwner(field(gm), t).size());
        // moving between floors
        Mon mover = new Mon("mover", F2); field(gm).put(p(6, 6), mover);
        mover.mapTiles = F10; field(gm).put(p(6, 6), mover);
        check(!Hooks.viewOwner(field(gm), g2).containsKey(p(6, 6)) && Hooks.viewOwner(field(gm), h10).get(p(6, 6)) == mover, "a Pokemon that changes floor leaves its old floor's map");

        // 5. same floor replace behaves like the original
        Mon r1 = new Mon("r1", F5), r2 = new Mon("r2", F5);
        Hooks.viewOwner(field(gm), r1).put(p(0, 0), r1); Object prev = Hooks.viewOwner(field(gm), r2).put(p(0, 0), r2);
        check(prev == r1 && Hooks.viewOwner(field(gm), r2).get(p(0, 0)) == r2, "same-floor replace returns the old entry, like HashMap");
        check(Hooks.viewOwner(field(gm), r1).remove(p(0, 0)) == r2 && !Hooks.viewOwner(field(gm), r1).containsKey(p(0, 0)), "remove works per floor");
        check(Hooks.viewOwner(field(gm), r1).remove(p(0, 0)) == null, "removing twice is harmless");

        // 6. unknown-floor monsters land in the map that was asked
        Mon u = new Mon("unknown", null); Hooks.viewOwner(field(gm), u).put(p(7, 7), u);
        check(field(gm).get(p(7, 7)) == u, "a Pokemon with no floor is registered where the caller is");

        // 7. world-level views
        check(Hooks.viewOverworld(field(gm)).get(p(1, 1)) == ow && Hooks.viewOverworld(field(gm)).get(p(3, 3)) == null, "viewOverworld always reads the overworld");
        Collection<Object> every = Hooks.viewAllFloors(field(gm)).values();
        check(every.contains(g2) && every.contains(h10) && every.contains(ow) && every.contains(far10), "viewAllFloors().values() covers every floor");

        // 8. saving
        Map sv = Hooks.viewSave(field(gm));
        Set<Object> vals = new HashSet<Object>(sv.values());
        int expected = 0; for (Object k : Hooks.viewAllFloors(field(gm)).values()) expected++;
        check(sv.size() == expected && vals.size() == expected, "save view holds all " + expected + " Pokemon exactly once");
        check(new HashSet<Object>(sv.keySet()).size() == sv.size(), "save keys are unique");
        boolean ok = true;
        for (Object k : new ArrayList<Object>(sv.keySet())) {
            Object mon = sv.get(k); Object fl = ((Mon) mon).mapTiles;
            if (fl == null) continue;
            Object tl = ((Map) fl).get(k);
            if (tl == null || ((FakeTile) tl).isSolid) ok = false;
        }
        check(ok, "every saved position is an existing, walkable tile of that Pokemon's own floor");
        check(Hooks.viewSave(field(gm)) == sv, "save view is reused while nothing changes (consistent between calls)");
        field(gm).put(p(2, 6), new Mon("late", OW));
        check(Hooks.viewSave(field(gm)) != sv, "and rebuilt after a change");

        // 9. loading: clear() empties every floor, puts then route by floor
        field(gm).clear();
        check(Hooks.viewAllFloors(field(gm)).values().isEmpty(), "clear() empties every floor");
        field(gm).put(p(1, 1), g2); field(gm).put(p(1, 1), h10);
        check(Hooks.viewOwner(field(gm), g2).get(p(1, 1)) == g2 && Hooks.viewOwner(field(gm), h10).get(p(1, 1)) == h10, "after clear, loading puts each Pokemon on its floor");

        // 10. floor numbers for saving eggs
        Mon egg = new Mon("egg", F10);
        check(Hooks.floorIndex(egg) == 4, "an egg on layer 4 is saved as floor 4 (was 100)");
        Mon o2 = new Mon("o", OW); o2.interiorIndex = 100; check(Hooks.floorIndex(o2) == 100, "overworld Pokemon keep the stored index");
        check(Hooks.floorIndex(null) == 0, "floorIndex is null-safe");

        // 11. fail-safe: something that is not a FloorMap is passed through untouched
        Map plain = new HashMap();
        check(Hooks.viewOwner(plain, g2) == plain && Hooks.viewOverworld(plain) == plain && Hooks.viewAllFloors(plain) == plain && Hooks.viewSave(plain) == plain, "plain maps pass through untouched (no-op if the wiring did not apply)");

        // 12. randomized run of the game's move protocol, with 20% of guards skipped (trails), 3 floors, tiny grid
        Random rnd = new Random(42);
        Object[] fl = {OW, F2, F10};
        FakeMap sm = new FakeMap(); sm.overworldTiles = OW; sm.tiles = OW; sm.pokemon = Hooks.newPokemonMap(new HashMap(), sm);
        List<Mon> mons = new ArrayList<Mon>();
        for (int i = 0; i < 30; i++) { Mon m = new Mon("m" + i, fl[i % 3]); m.position = p(rnd.nextInt(4), rnd.nextInt(4)); mons.add(m); }
        for (Mon m : mons) { Map v = Hooks.viewOwner(field(sm), m); if (!v.containsKey(m.position)) v.put(m.position, m); else { m.position = null; } }
        mons.removeIf(m -> m.position == null);
        boolean good = true; String why = "";
        for (int it = 0; it < 200000 && good; it++) {
            Mon m = mons.get(rnd.nextInt(mons.size()));
            Pt np = p(rnd.nextInt(4), rnd.nextInt(4));
            Map v = Hooks.viewOwner(field(sm), m);
            if (v.containsKey(np) && v.get(np) != m) continue;
            if (rnd.nextInt(5) != 0 && v.get(m.position) == m) v.remove(m.position);   // 1 in 5: the original's guard misses, leaving a trail
            m.position = np; v.put(np, m);
            if (it % 40 == 0) { sm.tiles = fl[rnd.nextInt(3)]; Hooks.tilesChanged(sm); }
            if (it % 101 == 0) {
                int reg = 0;
                for (Object f : fl) reg += Hooks.viewOwner(field(sm), new Mon("q", f)).size();
                if (reg != mons.size()) { good = false; why = "registered " + reg + " for " + mons.size() + " Pokemon at step " + it; }
                for (Mon x : mons) if (Hooks.viewOwner(field(sm), x).get(x.position) != x) { good = false; why = x + " not at its position on its floor"; break; }
                if (field(sm) != Hooks.viewOwner(field(sm), new Mon("q", sm.tiles))) { good = false; why = "field is not the current floor's map"; }
            }
        }
        check(good, "randomized protocol run with trails injected: every Pokemon registered exactly once, on its own floor, field follows the player" + (good ? "" : " - " + why));
        System.out.println(fail == 0 ? "ALL OK" : "FAILURES: " + fail);
        System.exit(fail == 0 ? 0 : 1);
    }
}
