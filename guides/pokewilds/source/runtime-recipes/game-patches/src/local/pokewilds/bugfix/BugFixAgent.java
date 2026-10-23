// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Divinakra
package local.pokewilds.bugfix;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import local.pokewilds.bugfix.asm.ClassReader;
import local.pokewilds.bugfix.asm.ClassVisitor;
import local.pokewilds.bugfix.asm.ClassWriter;
import local.pokewilds.bugfix.asm.FieldVisitor;
import local.pokewilds.bugfix.asm.MethodVisitor;
import local.pokewilds.bugfix.asm.Opcodes;

/**
 * Fixes for PokeWilds 0.8.11, applied as bytecode patches when the classes load. Four patches:
 * <ol>
 * <li>sprites: Ride/Cut/Build monsters face the wrong way with per-species mod sprites;</li>
 * <li>ho_oh: missing base-species entry makes monsters near Ho-Oh throw every frame;</li>
 * <li>floors: every floor gets its own monster map instead of one map shared by all floors (see {@link Hooks});</li>
 * <li>eggs: an egg laid on an upper floor is saved as belonging to the first floor;</li>
 * <li>(part of floors) saves retain exact floor placements in a versioned ZIP entry.</li>
 * </ol>
 * The original fixes can be switched off with -Dbugfix.NAME=false (sprites, hooh, floors, eggs).
 * The controller prompt patch is opt-in with -Dbugfix.prompts=true.
 * The floors patch also needs the exact PokeWilds 0.8.11 jar (checked by hash) so that it is applied completely or not at all.
 *
 * Sprites: DrawPlayerUpper/Lower force the drawn region's Y to Player.spriteOffsetY.
 * That is only right when every frame shares one Y (master sheet). Per-species mod sheets
 * (mods/pokemon/NAME/overworld.png) stack frames vertically and never set it, so Ride/Cut/Build
 * always draw the top row. This agent makes those draw calls use the sprite's own region Y.
 */
public final class BugFixAgent {
    static final String UPPER = "com/pkmngen/game/DrawPlayerUpper";
    static final String LOWER = "com/pkmngen/game/DrawPlayerLower";
    static final String PLAYER = "com/pkmngen/game/Player";
    static final String POKEMON = "com/pkmngen/game/Pokemon";
    static final String SPRITE = "com/badlogic/gdx/graphics/g2d/Sprite";
    static final String PKMN_MAP = "com/pkmngen/game/PkmnMap";
    static final String SAVE = "com/pkmngen/game/util/Save";
    static final String MAP_SAVE_DATA = "com/pkmngen/game/Network$MapSaveData";
    static final String GAME_PKG = "com/pkmngen/game/";
    static final String POKEMON_DATA_V07 = "com/pkmngen/game/Network$PokemonDataV07";
    static final String HOOKS = "local/pokewilds/bugfix/Hooks";
    static final String MAP_DESC = "Ljava/util/Map;";
    /** SHA-256 of com/pkmngen/game/PkmnMap.class and Pokemon.class in the PokeWilds 0.8.11 jar. */
    static final String PKMN_MAP_SHA256 = "7e7317c4f1b6373fbb1129c43a027230e7943d2ea87762a2c8a8d4776a0a8dc9";
    static final String POKEMON_SHA256 = "84daae5ce3b6a53341ef20a213c32b7d25991c4a2fa878c1896a129a59bdc6e4";

    /** True if the game jar on the class path is PokeWilds 0.8.11 (so the floors patch can be applied completely). */
    static boolean gameJarIs0811() {
        try {
            String cp = System.getProperty("java.class.path", "");
            String jar = cp.split(java.io.File.pathSeparator)[0];
            if (!jar.endsWith(".jar")) return false;
            try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(jar)) {
                return PKMN_MAP_SHA256.equals(sha256(z, PKMN_MAP + ".class")) && POKEMON_SHA256.equals(sha256(z, POKEMON + ".class"));
            }
        } catch (Throwable t) {
            System.err.println("[bugfix] floors: could not check the game jar (" + t + ")");
            return false;
        }
    }

    private static String sha256(java.util.zip.ZipFile z, String entry) throws Exception {
        java.util.zip.ZipEntry e = z.getEntry(entry);
        if (e == null) return "";
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        try (java.io.InputStream in = z.getInputStream(e)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static boolean enabled(String patch) {
        return !"false".equalsIgnoreCase(System.getProperty("bugfix." + patch));
    }

    private static volatile Boolean hooksReachable;

    /** The patched game classes call Hooks, so the game's class loader must be able to see it. */
    static boolean hooksVisibleFrom(ClassLoader l) {
        Boolean known = hooksReachable;
        if (known != null) return known;
        boolean ok;
        try {
            Class.forName(HOOKS.replace('/', '.'), false, l == null ? ClassLoader.getSystemClassLoader() : l);
            ok = true;
        } catch (Throwable t) {
            ok = false;
            System.err.println("[bugfix] floors/eggs: Hooks not visible to the game's class loader (" + t + ") - skipped");
        }
        hooksReachable = ok;
        return ok;
    }

    public static void premain(String args, Instrumentation inst) {
        final boolean sprites = enabled("sprites"), hooh = enabled("hooh"), eggs = enabled("eggs");
        final boolean prompts = "true".equalsIgnoreCase(System.getProperty("bugfix.prompts", "false"));
        final boolean floors;
        if (!enabled("floors")) {
            floors = false;
        } else if (gameJarIs0811()) {
            floors = true;
            System.err.println("[bugfix] floors: game jar is PokeWilds 0.8.11, per-floor Pokemon maps enabled");
        } else {
            floors = false;
            System.err.println("[bugfix] floors: game jar is not the known PokeWilds 0.8.11 - floors patch disabled");
        }
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader l, String name, Class<?> c, ProtectionDomain pd, byte[] buf) {
                if (name == null || !name.startsWith(GAME_PKG)) return null;
                try {
                    // The floors and eggs patches call Hooks, so the game's class loader must be able to see it.
                    boolean hooks = (floors || eggs) && hooksVisibleFrom(l);
                    return transformClass(name, buf, sprites, hooh, floors && hooks, eggs && hooks, prompts, null);
                } catch (Throwable t) {
                    System.err.println("[bugfix] failed on " + name + ", left unpatched: " + t);
                    return null;
                }
            }
        });
        System.err.println("[bugfix] agent installed");
    }

    /**
     * Applies every enabled patch to one game class. Shared by the agent and the offline patcher ({@link PatchJar}).
     * Returns the new bytes, or null if nothing changed. {@code applied} (may be null) collects a label per patch.
     */
    public static byte[] transformClass(String name, byte[] buf, boolean sprites, boolean hooh, boolean floors, boolean eggs, java.util.List<String> applied) {
        return transformClass(name, buf, sprites, hooh, floors, eggs, false, applied);
    }

    /** Same as the legacy overload, with the controller-prompt patch independently selectable. */
    public static byte[] transformClass(String name, byte[] buf, boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean prompts, java.util.List<String> applied) {
        byte[] cur = buf;
        boolean changed = false;
        if (sprites && (name.equals(UPPER) || name.equals(LOWER))) {
            byte[] r = patch(name, cur);
            if (r != null) { cur = r; changed = true; if (applied != null) applied.add("sprites:" + name); }
        }
        if (hooh && name.equals(POKEMON)) {
            byte[] r = patchPokemon(cur);
            if (r != null) { cur = r; changed = true; if (applied != null) applied.add("hooh:" + name); }
        }
        if (floors) {
            byte[] r = patchTiles(name, cur);                 // PkmnMap constructor + every "player changed floor"
            if (r != null) { cur = r; changed = true; if (applied != null) applied.add("floors-wiring:" + name); }
            if (name.equals(PKMN_MAP)) {
                r = patchLoadExact(cur);
                if (r != null) { cur = r; changed = true; if (applied != null) applied.add("floors-exact-load:" + name); }
            }
            if (name.equals(SAVE)) {
                r = patchJsonSave(cur);
                if (r != null) { cur = r; changed = true; if (applied != null) applied.add("floors-exact-save:" + name); }
            }
            if (name.startsWith("com/pkmngen/game/Network")) r = patchSave(name, cur);
            else if (!name.startsWith("com/pkmngen/game/ClientBroadcast") && !name.startsWith("com/pkmngen/game/ServerBroadcast")) r = patchFloors(name, cur);
            else r = null;
            if (r != null) { cur = r; changed = true; if (applied != null) applied.add("floors-routing:" + name); }
        }
        if (eggs && name.equals(POKEMON_DATA_V07)) {
            byte[] r = patchEggFloor(cur);
            if (r != null) { cur = r; changed = true; if (applied != null) applied.add("eggs:" + name); }
        }
        if (prompts) {
            byte[] r = PromptPatch.transform(name, cur, applied);
            if (r != null) { cur = r; changed = true; }
        }
        return changed ? cur : null;
    }

    /** Returns patched bytes, or null if the class doesn't have exactly the expected shape. */
    public static byte[] patch(final String className, byte[] original) {
        final int expected = className.equals(UPPER) ? 5 : 1;
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                final boolean instance = (access & Opcodes.ACC_STATIC) == 0;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fname, String fdesc) {
                        if (instance && op == Opcodes.GETFIELD && owner.equals(PLAYER)
                                && fname.equals("spriteOffsetY") && fdesc.equals("I")) {
                            count[0]++;
                            super.visitInsn(Opcodes.POP);                    // drop the Player reference
                            super.visitVarInsn(Opcodes.ALOAD, 0);            // this
                            super.visitFieldInsn(Opcodes.GETFIELD, className, "spritePart", "L" + SPRITE + ";");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, SPRITE, "getRegionY", "()I", false);
                        } else {
                            super.visitFieldInsn(op, owner, fname, fdesc);
                        }
                    }
                };
            }
        }, 0);
        if (count[0] != expected) {
            System.err.println("[bugfix] " + className + ": found " + count[0] + " site(s), expected "
                    + expected + " - NOT patching");
            return null;
        }
        System.err.println("[bugfix] patched " + className + ": " + count[0] + " site(s)");
        return cw.toByteArray();
    }

    /**
     * PokeWilds 0.8.11: Pokemon.baseSpecies is built from "HoOhEvosAttacks:" so it holds the key
     * "hooh", but the species is named "ho_oh" everywhere else. baseSpecie() then returns null for
     * Ho-Oh and every monster that scans a nearby Ho-Oh throws a NullPointerException each frame.
     * This registers ho_oh -> ho_oh at the end of monster's static initializer.
     */
    public static byte[] patchPokemon(byte[] original) {
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                if (!n.equals("<clinit>")) return mv;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitInsn(int op) {
                        if (op == Opcodes.RETURN) {
                            count[0]++;
                            super.visitFieldInsn(Opcodes.GETSTATIC, POKEMON, "baseSpecies", "Ljava/util/HashMap;");
                            super.visitLdcInsn("ho_oh");
                            super.visitLdcInsn("ho_oh");
                            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "putIfAbsent",
                                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                            super.visitInsn(Opcodes.POP);
                        }
                        super.visitInsn(op);
                    }
                };
            }
        }, 0);
        if (count[0] != 1) {
            System.err.println("[bugfix] Pokemon.<clinit>: found " + count[0] + " return(s), expected 1 - NOT patching");
            return null;
        }
        System.err.println("[bugfix] patched com/pkmngen/game/Pokemon: ho_oh base-species entry");
        return cw.toByteArray();
    }
    /**
     * Floors, wiring: (1) in PkmnMap's constructor the new HashMap for the "pokemon" field is replaced by the per-floor
     * registry's map; (2) after every assignment to PkmnMap.tiles (the player changed floor, or a world was loaded) the
     * game's "pokemon" field is switched to the map of the new floor. Returns null if the class has neither.
     */
    public static byte[] patchTiles(final String className, byte[] original) {
        final int[] count = {0, 0};   // constructor init, tiles assignments
        final boolean isMap = className.equals(PKMN_MAP);
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, final String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                final boolean ctor = n.equals("<init>");
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fname, String fdesc) {
                        if (op == Opcodes.PUTFIELD && owner.equals(PKMN_MAP) && fname.equals("pokemon") && fdesc.equals(MAP_DESC) && isMap && ctor) {
                            count[0]++;
                            super.visitVarInsn(Opcodes.ALOAD, 0);                       // stack: this, HashMap, this
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "newPokemonMap",
                                    "(" + MAP_DESC + "Ljava/lang/Object;)" + MAP_DESC, false);
                            super.visitFieldInsn(op, owner, fname, fdesc);
                        } else if (op == Opcodes.PUTFIELD && owner.equals(PKMN_MAP) && fname.equals("tiles") && fdesc.equals(MAP_DESC)) {
                            count[1]++;
                            super.visitInsn(Opcodes.DUP2);                              // map, tiles, map, tiles
                            super.visitFieldInsn(op, owner, fname, fdesc);              // map, tiles
                            super.visitInsn(Opcodes.POP);                               // map
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "tilesChanged", "(Ljava/lang/Object;)V", false);
                        } else {
                            super.visitFieldInsn(op, owner, fname, fdesc);
                        }
                    }
                };
            }
        }, 0);
        if (isMap && count[0] != 1) {
            System.err.println("[bugfix] floors: PkmnMap constructor: found " + count[0] + " init(s) of the pokemon field, expected 1 - NOT patching");
            return null;
        }
        if (count[0] + count[1] == 0) return null;
        System.err.println("[bugfix] floors: patched " + className + " (wiring): " + count[0] + " init, " + count[1] + " floor change(s)");
        return cw.toByteArray();
    }

    /**
     * Floors: reads of PkmnMap.pokemon by code that belongs to a monster go to that monster's own floor
     * (Hooks.viewOwner); world-level code (day/night spawning, world generation) goes to the overworld or, for
     * regeneration, all floors. Player, drawing and UI code is left alone: the game's field already points at the
     * map of the floor the player is on. {@code owner} is the enclosing monster (Pokemon itself, or an inner class
     * with a this$N field of type Pokemon, or an inner class constructor's first argument).
     * Returns null if the class has no reads to route.
     */
    public static byte[] patchFloors(final String className, byte[] original) {
        final int[] count = {0, 0, 0};   // to the owner's floor, to the overworld, to all floors
        final String[] outerField = {null};
        final String[] methodName = {""};
        final boolean isPokemon = className.equals(POKEMON);
        final boolean overworld = className.startsWith("com/pkmngen/game/CycleDayNight") || className.startsWith("com/pkmngen/game/GenIsland1");
        final boolean allFloors = className.startsWith("com/pkmngen/game/GenerateWorld");
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public FieldVisitor visitField(int access, String n, String d, String sig, Object value) {
                if (n.startsWith("this$") && d.equals("L" + POKEMON + ";")) outerField[0] = n;
                return super.visitField(access, n, d, sig, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                methodName[0] = n;
                final boolean instance = (access & Opcodes.ACC_STATIC) == 0 && !n.equals("<init>") && !n.equals("<clinit>");
                // An inner class constructor receives the enclosing monster as its first argument.
                final boolean ctorOuter = !isPokemon && outerField[0] != null && n.equals("<init>") && d.startsWith("(L" + POKEMON + ";");
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fname, String fdesc) {
                        super.visitFieldInsn(op, owner, fname, fdesc);
                        if (op != Opcodes.GETFIELD || !owner.equals(PKMN_MAP) || !fname.equals("pokemon") || !fdesc.equals(MAP_DESC)) return;
                        if (overworld) {
                            count[1]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewOverworld", "(" + MAP_DESC + ")" + MAP_DESC, false);
                        } else if (allFloors) {
                            count[2]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewAllFloors", "(" + MAP_DESC + ")" + MAP_DESC, false);
                        } else if (instance && isPokemon) {
                            count[0]++;
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewOwner", "(" + MAP_DESC + "Ljava/lang/Object;)" + MAP_DESC, false);
                        } else if (ctorOuter) {
                            count[0]++;
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewOwner", "(" + MAP_DESC + "Ljava/lang/Object;)" + MAP_DESC, false);
                        } else if (instance && outerField[0] != null) {
                            count[0]++;
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitFieldInsn(Opcodes.GETFIELD, className, outerField[0], "L" + POKEMON + ";");
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewOwner", "(" + MAP_DESC + "Ljava/lang/Object;)" + MAP_DESC, false);
                        }
                        // else: player / drawing / UI code - untouched
                    }
                };
            }
        }, 0);
        if (count[0] + count[1] + count[2] == 0) return null;
        System.err.println("[bugfix] floors: patched " + className + ": " + count[0] + " read(s) to the Pokemon's floor, "
                + count[1] + " to the overworld, " + count[2] + " to all floors");
        return cw.toByteArray();
    }

    /** The legacy map remains readable; exact floor placements live in a second entry of the same ZIP. */
    public static byte[] patchJsonSave(byte[] original) {
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                if (!n.equals("saveJson") || !d.equals("(Ljava/lang/Object;Ljava/lang/String;)V")) return mv;
                count[0]++;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "writeJsonZip", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                        super.visitInsn(Opcodes.RETURN);
                    }
                    @Override public void visitInsn(int op) { }
                    @Override public void visitVarInsn(int op, int var) { }
                    @Override public void visitFieldInsn(int op, String owner, String name, String desc) { }
                    @Override public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) { }
                    @Override public void visitTypeInsn(int op, String type) { }
                    @Override public void visitIntInsn(int op, int operand) { }
                    @Override public void visitLdcInsn(Object value) { }
                    @Override public void visitJumpInsn(int op, local.pokewilds.bugfix.asm.Label label) { }
                    @Override public void visitLabel(local.pokewilds.bugfix.asm.Label label) { }
                    @Override public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) { }
                    @Override public void visitTryCatchBlock(local.pokewilds.bugfix.asm.Label start, local.pokewilds.bugfix.asm.Label end, local.pokewilds.bugfix.asm.Label handler, String type) { }
                    @Override public void visitLocalVariable(String name, String desc, String signature, local.pokewilds.bugfix.asm.Label start, local.pokewilds.bugfix.asm.Label end, int index) { }
                    @Override public void visitLineNumber(int line, local.pokewilds.bugfix.asm.Label start) { }
                    @Override public void visitIincInsn(int var, int increment) { }
                    @Override public void visitMultiANewArrayInsn(String desc, int dims) { }
                    @Override public void visitInvokeDynamicInsn(String name, String desc, local.pokewilds.bugfix.asm.Handle bsm, Object... args) { }
                    @Override public void visitMaxs(int maxStack, int maxLocals) { super.visitMaxs(2, 2); }
                };
            }
        }, 0);
        return count[0] == 1 ? cw.toByteArray() : null;
    }

    /** At the legacy map read, let the exact entry restore all Pokémon and skip the lossy projection. */
    public static byte[] patchLoadExact(byte[] original) {
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                if (!n.equals("loadMapFromFile")) return mv;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override public void visitFieldInsn(int op, String owner, String field, String desc) {
                        if (op == Opcodes.GETFIELD && owner.equals(MAP_SAVE_DATA) && field.equals("overworldPokemon") && desc.equals("Ljava/util/HashMap;")) super.visitInsn(Opcodes.DUP);
                        super.visitFieldInsn(op, owner, field, desc);
                        if (op == Opcodes.GETFIELD && owner.equals(MAP_SAVE_DATA) && field.equals("overworldPokemon") && desc.equals("Ljava/util/HashMap;")) {
                            count[0]++;
                            super.visitInsn(Opcodes.SWAP);
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitVarInsn(Opcodes.ALOAD, 1);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "restoreExact", "(Ljava/util/HashMap;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/HashMap;", false);
                        }
                    }
                };
            }
        }, 0);
        return count[0] == 2 ? cw.toByteArray() : null;
    }

    /**
     * Floors, legacy saving: MapSaveData still gets its position-keyed projection for old readers.
     * The complete placement list is written separately by writeJsonZip.
     */
    public static byte[] patchSave(final String className, byte[] original) {
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fname, String fdesc) {
                        super.visitFieldInsn(op, owner, fname, fdesc);
                        if (op == Opcodes.GETFIELD && owner.equals(PKMN_MAP) && fname.equals("pokemon") && fdesc.equals(MAP_DESC)) {
                            count[0]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "viewSave", "(" + MAP_DESC + ")" + MAP_DESC, false);
                        }
                    }
                };
            }
        }, 0);
        if (count[0] == 0) return null;
        System.err.println("[bugfix] floors: patched " + className + " (save): " + count[0] + " read(s)");
        return cw.toByteArray();
    }

    /**
     * Eggs: Network.PokemonDataV07(Pokemon) saves pokemon.interiorIndex, which is only maintained for
     * monsters the player dropped. Read the floor from the monster's real tile map instead.
     */
    public static byte[] patchEggFloor(byte[] original) {
        final int[] count = {0};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String n, String d, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(access, n, d, sig, ex);
                if (!n.equals("<init>") || !d.equals("(L" + POKEMON + ";)V")) return mv;
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitFieldInsn(int op, String owner, String fname, String fdesc) {
                        if (op == Opcodes.GETFIELD && owner.equals(POKEMON) && fname.equals("interiorIndex") && fdesc.equals("I")) {
                            count[0]++;
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "floorIndex", "(Ljava/lang/Object;)I", false);
                        } else {
                            super.visitFieldInsn(op, owner, fname, fdesc);
                        }
                    }
                };
            }
        }, 0);
        if (count[0] != 1) {
            System.err.println("[bugfix] PokemonDataV07: found " + count[0] + " interiorIndex read(s), expected 1 - NOT patching");
            return null;
        }
        System.err.println("[bugfix] eggs: patched " + POKEMON_DATA_V07 + " floor index");
        return cw.toByteArray();
    }
}
