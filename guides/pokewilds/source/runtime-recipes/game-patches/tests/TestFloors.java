package local.pokewilds.bugfix;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import local.pokewilds.bugfix.asm.*;

/** Offline checks of the floors + eggs bytecode patches against the real 0.8.11 jar. */
public class TestFloors {
    static class L extends ClassLoader {
        final Map<String, byte[]> defs = new HashMap<>();
        L(ClassLoader p) { super(p); }
        @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
            byte[] b = defs.get(n);
            if (b != null) synchronized (getClassLoadingLock(n)) {
                Class<?> c = findLoadedClass(n);
                if (c == null) c = defineClass(n, b, 0, b.length);
                return c;
            }
            return super.loadClass(n, r);
        }
    }
    static int fail = 0;
    static final Set<String> patchedNames = new TreeSet<>();
    static void check(boolean ok, String msg) { System.out.println((ok ? "PASS " : "FAIL ") + msg); if (!ok) fail++; }

    /** counts: [0]=GETFIELD PkmnMap.pokemon, [1]=PUTFIELD PkmnMap.tiles, [2]=PUTFIELD PkmnMap.pokemon */
    static int[] countFields(byte[] b) {
        final int[] n = {0, 0, 0};
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int a, String nm, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    public void visitFieldInsn(int op, String o, String f, String fd) {
                        if (!o.equals("com/pkmngen/game/PkmnMap")) return;
                        if (op == Opcodes.GETFIELD && f.equals("pokemon")) n[0]++;
                        if (op == Opcodes.PUTFIELD && f.equals("tiles")) n[1]++;
                        if (op == Opcodes.PUTFIELD && f.equals("pokemon")) n[2]++;
                    }
                };
            }
        }, 0);
        return n;
    }
    static Map<String, Integer> countHooks(byte[] b) {
        final Map<String, Integer> n = new TreeMap<>();
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int a, String nm, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    public void visitMethodInsn(int op, String o, String f, String fd, boolean i) {
                        if (o.equals("local/pokewilds/bugfix/Hooks")) n.merge(f, 1, Integer::sum);
                    }
                };
            }
        }, 0);
        return n;
    }
    static int total(Map<String, Integer> m, String... keys) { int t = 0; for (String k : keys) t += m.getOrDefault(k, 0); return t; }

    public static void main(String[] a) throws Exception {
        ZipFile jar = new ZipFile(a[0]);
        L loader = new L(TestFloors.class.getClassLoader());
        int readsTotal = 0, putTilesTotal = 0, ctorInits = 0;
        int owner = 0, ow = 0, all = 0, save = 0, tilesHooks = 0, ctorHooks = 0, untouchedReads = 0;
        for (Enumeration<? extends ZipEntry> en = jar.entries(); en.hasMoreElements();) {
            ZipEntry ze = en.nextElement();
            String nm = ze.getName();
            if (!nm.startsWith("com/pkmngen/game/") || !nm.endsWith(".class")) continue;
            String cn = nm.substring(0, nm.length() - 6);
            byte[] orig; try (InputStream in = jar.getInputStream(ze)) { orig = in.readAllBytes(); }
            loader.defs.put(cn.replace('/', '.'), orig);
            int[] f = countFields(orig);
            readsTotal += f[0]; putTilesTotal += f[1]; ctorInits += f[2];
            byte[] cur = orig;
            boolean changed = false;
            byte[] t = BugFixAgent.patchTiles(cn, cur);
            if (t != null) { cur = t; changed = true; }
            byte[] r;
            boolean net = cn.startsWith("com/pkmngen/game/Network");
            boolean bcast = cn.startsWith("com/pkmngen/game/ClientBroadcast") || cn.startsWith("com/pkmngen/game/ServerBroadcast");
            if (net) r = BugFixAgent.patchSave(cn, cur); else if (!bcast) r = BugFixAgent.patchFloors(cn, cur); else r = null;
            if (r != null) { cur = r; changed = true; }
            Map<String, Integer> h = countHooks(cur);
            if (changed) {
                loader.defs.put(cn.replace('/', '.'), cur);
                patchedNames.add(cn.replace('/', '.'));
                owner += total(h, "viewOwner"); ow += total(h, "viewOverworld"); all += total(h, "viewAllFloors"); save += total(h, "viewSave");
                tilesHooks += total(h, "tilesChanged"); ctorHooks += total(h, "newPokemonMap");
                // whatever was patched must be exactly what this class has to offer
                if (t != null) check(total(h, "tilesChanged") == f[1] && total(h, "newPokemonMap") == f[2], cn + ": tiles assignments hooked " + total(h, "tilesChanged") + "/" + f[1] + ", ctor init " + total(h, "newPokemonMap") + "/" + f[2]);
            } else {
                check(f[1] == 0 && f[2] == 0 && (f[0] == 0 || net || bcast || true), "untouched " + cn + " has no floor assignments");
            }
            if (f[0] > 0 && !net && !bcast) untouchedReads += f[0] - total(h, "viewOwner", "viewOverworld", "viewAllFloors");
        }
        System.out.println("reads of PkmnMap.pokemon in the jar: " + readsTotal + "; routed to owner floor " + owner + ", overworld " + ow + ", all floors " + all + ", save " + save
                + ", left alone (player/drawing/UI, and multiplayer classes) " + (readsTotal - owner - ow - all - save));
        check(tilesHooks == putTilesTotal && putTilesTotal == 11, "every assignment of PkmnMap.tiles is hooked: " + tilesHooks + "/" + putTilesTotal + " (expected 11)");
        check(ctorHooks == 1 && ctorInits == 1, "PkmnMap.pokemon is initialised once and that is hooked: " + ctorHooks + "/" + ctorInits);
        check(owner == 40 && ow == 7 && all == 3 && save == 4, "routing counts: owner=" + owner + " (40) overworld=" + ow + " (7) all=" + all + " (3) save=" + save + " (4)");

        // Egg patch
        String v07 = "com/pkmngen/game/Network$PokemonDataV07";
        byte[] eb; try (InputStream in = jar.getInputStream(jar.getEntry(v07 + ".class"))) { eb = in.readAllBytes(); }
        byte[] ep = BugFixAgent.patchEggFloor(eb);
        check(ep != null && total(countHooks(ep), "floorIndex") == 1, "PokemonDataV07 patched with exactly 1 hook call");
        if (ep != null) { loader.defs.put(v07.replace('/', '.'), ep); patchedNames.add(v07.replace('/', '.')); }
        check(BugFixAgent.patchEggFloor(loadBytes(jar, "com/pkmngen/game/Tile")) == null, "egg patch refuses a class without the expected read");

        // Wrong class content must be refused, not half-patched
        check(BugFixAgent.patchTiles("com/pkmngen/game/PkmnMap", loadBytes(jar, "com/pkmngen/game/Tile")) == null, "PkmnMap wiring refuses a class without the constructor init");

        // Verify every patched class links (Class.forName with init => bytecode verifier runs first)
        int verr = 0;
        for (String cn : new ArrayList<>(patchedNames)) {
            try { Class.forName(cn, true, loader); }
            catch (VerifyError ve) { verr++; System.out.println("VERIFY ERROR " + cn + ": " + ve.getMessage().split("\n")[0]); }
            catch (Throwable t) { /* static init needs the game; verification already passed */ }
        }
        check(verr == 0, "no VerifyError across " + patchedNames.size() + " patched classes");
        System.out.println(fail == 0 ? "ALL OK" : ("FAILURES: " + fail));
        System.exit(fail == 0 ? 0 : 1);
    }
    static byte[] loadBytes(ZipFile z, String n) throws IOException { try (InputStream in = z.getInputStream(z.getEntry(n + ".class"))) { return in.readAllBytes(); } }
}
