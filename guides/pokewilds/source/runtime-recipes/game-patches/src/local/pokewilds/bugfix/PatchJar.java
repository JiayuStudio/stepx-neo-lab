// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Divinakra
package local.pokewilds.bugfix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import local.pokewilds.bugfix.asm.ClassReader;
import local.pokewilds.bugfix.asm.ClassVisitor;
import local.pokewilds.bugfix.asm.MethodVisitor;
import local.pokewilds.bugfix.asm.Opcodes;

/**
 * Offline patcher: writes a copy of the official PokeWilds 0.8.11 jar with all BugFix patches applied, so the game
 * needs no -javaagent. It applies exactly the same patches as the agent (BugFixAgent.transformClass), adds the
 * runtime support classes (Hooks), and refuses to write anything unless every patch applied as expected.
 *
 * Usage:
 *   java -jar bugfix.jar pokewilds.jar               patch in place; the original is kept as pokewilds-original.jar.bak
 *   java -jar bugfix.jar in.jar out.jar              write a patched copy, leave the input alone
 *   java -jar bugfix.jar --restore pokewilds.jar     put the original back
 * Options: --no-sprites --no-hooh --no-floors --no-eggs
 */
public final class PatchJar {
    private PatchJar() {}

    /** Fingerprint of all patches applied to the official 0.8.11 jar (independent of jar compression). */
    // The local lossless floor-save extension intentionally differs from the upstream v1.0 bytes.
    static final String REFERENCE_FINGERPRINT = "TBD";

    public static void main(String[] args) throws Exception {
        boolean sprites = true, hooh = true, floors = true, eggs = true, prompts = false, restore = false;
        List<String> files = new ArrayList<String>();
        for (String a : args) {
            if (a.equals("--no-sprites")) sprites = false;
            else if (a.equals("--no-hooh")) hooh = false;
            else if (a.equals("--no-floors")) floors = false;
            else if (a.equals("--no-eggs")) eggs = false;
            else if (a.equals("--prompts")) prompts = true;
            else if (a.equals("--no-prompts")) prompts = false;
            else if (a.equals("--restore")) restore = true;
            else files.add(a);
        }
        String usage = "Usage:\n  java -jar bugfix.jar <pokewilds.jar>              patch in place (the original is kept as a backup)\n"
                + "  java -jar bugfix.jar <in.jar> <out.jar>           write a patched copy, leave the input alone\n"
                + "  java -jar bugfix.jar --restore <pokewilds.jar>    put the original back\n"
                + "Options: --no-sprites --no-hooh --no-floors --no-eggs --prompts --no-prompts";
        try {
            if (restore) {
                if (files.size() != 1) { System.err.println(usage); System.exit(1); }
                restore(new File(files.get(0)));
            } else if (files.size() == 1) {
                patchInPlace(new File(files.get(0)), sprites, hooh, floors, eggs, prompts);
            } else if (files.size() == 2) {
                File in = new File(files.get(0)), out = new File(files.get(1));
                if (in.getCanonicalFile().equals(out.getCanonicalFile())) fail("input and output must be different files (to patch in place, give only one file)");
                patch(in, out, sprites, hooh, floors, eggs, prompts);
            } else {
                System.err.println(usage);
                System.exit(1);
            }
        } catch (PatchException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(2);
        }
    }

    /** pokewilds.jar -> pokewilds-original.jar.bak (the official jar), pokewilds.jar (patched). */
    static File backupOf(File jar) {
        String n = jar.getName();
        if (n.toLowerCase().endsWith(".jar")) n = n.substring(0, n.length() - 4);
        return new File(jar.getAbsoluteFile().getParentFile(), n + "-original.jar.bak");
    }

    static boolean isPatched(File jar) throws IOException {
        try (ZipFile z = new ZipFile(jar)) { return z.getEntry("META-INF/BUGFIX.txt") != null; }
    }

    static void patchInPlace(File jar, boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean prompts) throws Exception {
        if (!jar.isFile()) throw new PatchException("file not found: " + jar);
        File backup = backupOf(jar);
        if (isPatched(jar)) {
            throw new PatchException(jar.getName() + " is already patched by BugFix." + (backup.exists() ? "\nYour original is saved as " + backup.getName()
                    + ". To undo, run: java -jar bugfix.jar --restore " + jar.getName() : ""));
        }
        File tmp = new File(jar.getAbsolutePath() + ".bugfix-tmp");
        try {
            patch(jar, tmp, sprites, hooh, floors, eggs, prompts);
            if (backup.exists()) {
                // An earlier backup is only reused if it is the very same jar, so nothing is ever overwritten.
                if (!sha256(backup).equals(sha256(jar)))
                    throw new PatchException("the backup file " + backup.getName() + " already exists and is a different jar. Move it away first, so it is not overwritten.");
            } else {
                java.nio.file.Files.move(jar.toPath(), backup.toPath());
            }
            try {
                java.nio.file.Files.move(tmp.toPath(), jar.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (!jar.exists() && backup.exists()) java.nio.file.Files.move(backup.toPath(), jar.toPath());   // roll back
                throw new PatchException("could not replace " + jar.getName() + " (" + e.getMessage() + "). Is the game still running? Close it and try again.");
            }
        } catch (IOException e) {
            throw new PatchException("could not write next to " + jar.getName() + " (" + e.getMessage() + "). Is the game running, or is the folder protected? Close the game, or use: java -jar bugfix.jar <in.jar> <out.jar>");
        } finally {
            if (tmp.exists()) tmp.delete();
        }
        System.out.println();
        System.out.println("Done. " + jar.getName() + " is now the patched game.");
        System.out.println("Your original is saved as " + backup.getName() + ". To go back:  java -jar bugfix.jar --restore " + jar.getName());
    }

    static void restore(File jar) throws Exception {
        File backup = backupOf(jar);
        if (!backup.isFile()) throw new PatchException("no backup found (" + backup.getName() + " next to " + jar.getName() + ")");
        if (jar.exists()) {
            File keep = new File(jar.getAbsoluteFile().getParentFile(), jar.getName().replaceAll("(?i)\\.jar$", "") + "-bugfix.jar");
            if (keep.exists() && !keep.delete()) throw new PatchException("cannot replace " + keep.getName());
            java.nio.file.Files.move(jar.toPath(), keep.toPath());
            System.out.println("The patched jar was kept as " + keep.getName() + ".");
        }
        java.nio.file.Files.move(backup.toPath(), jar.toPath());
        System.out.println("Restored the original " + jar.getName() + ".");
    }

    static final class PatchException extends Exception { PatchException(String m) { super(m); } }
    private static void fail(String m) { System.err.println("ERROR: " + m); System.exit(1); }

    static void patch(File inFile, File outFile, boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean prompts) throws Exception {
        List<String> applied = new ArrayList<String>();
        Map<String, byte[]> patchedBytes = new TreeMap<String, byte[]>();
        Map<String, Integer> hookCalls = new TreeMap<String, Integer>();
        int classes = 0, patchedClasses = 0;
        try (ZipFile zin = new ZipFile(inFile)) {
            // 1. only the exact, unpatched PokeWilds 0.8.11 class files are accepted
            if (zin.getEntry("META-INF/BUGFIX.txt") != null || zin.getEntry("local/pokewilds/bugfix/Hooks.class") != null)
                throw new PatchException("this jar is already patched by BugFix; run the patcher on the official, unmodified pokewilds.jar");
            for (String[] c : new String[][] {{BugFixAgent.PKMN_MAP, BugFixAgent.PKMN_MAP_SHA256}, {BugFixAgent.POKEMON, BugFixAgent.POKEMON_SHA256}}) {
                ZipEntry e = zin.getEntry(c[0] + ".class");
                if (e == null) throw new PatchException("this is not a PokeWilds jar (" + c[0] + ".class is missing)");
                if (!c[1].equals(sha256(read(zin, e)))) throw new PatchException(c[0] + ".class does not match PokeWilds 0.8.11. This patcher only supports the official 0.8.11 release.");
            }
            for (Enumeration<? extends ZipEntry> en = zin.entries(); en.hasMoreElements();) {
                String n = en.nextElement().getName();
                if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC")))
                    throw new PatchException("the jar is signed (" + n + "); patching would invalidate the signature");
            }
            // 2. copy every entry, patching the game classes
            System.out.println("The jar is valid PokeWilds 0.8.11. Patching now: the whole jar is rewritten, which can take a minute or two...");
            final int total = zin.size();
            int done = 0;
            File tmp = new File(outFile.getPath() + ".tmp");
            try (ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmp))) {
                for (Enumeration<? extends ZipEntry> en = zin.entries(); en.hasMoreElements();) {
                    ZipEntry e = en.nextElement();
                    if (++done % 10000 == 0) System.out.println("  " + done + " of " + total + " files copied...");
                    ZipEntry ne = new ZipEntry(e.getName());
                    ne.setTime(e.getTime());
                    if (e.isDirectory()) { zout.putNextEntry(ne); zout.closeEntry(); continue; }
                    String n = e.getName();
                    if (!(n.startsWith("com/pkmngen/game/") && n.endsWith(".class"))) {
                        // Everything else (sounds, images, ...) is copied as a stream, so memory use stays small.
                        zout.putNextEntry(ne);
                        try (InputStream in = zin.getInputStream(e)) {
                            byte[] buf = new byte[65536];
                            for (int r; (r = in.read(buf)) > 0; ) zout.write(buf, 0, r);
                        }
                        zout.closeEntry();
                        continue;
                    }
                    byte[] data = read(zin, e);
                    {
                        classes++;
                        String cn = n.substring(0, n.length() - 6);
                        byte[] r = BugFixAgent.transformClass(cn, data, sprites, hooh, floors, eggs, prompts, applied);
                        if (r != null) {
                            data = r;
                            patchedClasses++;
                            patchedBytes.put(cn, r);
                            for (Map.Entry<String, Integer> h : countHooks(r).entrySet()) {
                                Integer old = hookCalls.get(h.getKey());
                                hookCalls.put(h.getKey(), (old == null ? 0 : old) + h.getValue());
                            }
                        }
                    }
                    zout.putNextEntry(ne);
                    zout.write(data);
                    zout.closeEntry();
                }
                // 3. runtime support classes
                if (floors || eggs || prompts) {
                    int supportClasses = 0;
                    File self = new File(PatchJar.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    try (ZipFile zself = new ZipFile(self)) {
                        for (Enumeration<? extends ZipEntry> en = zself.entries(); en.hasMoreElements();) {
                            ZipEntry e = en.nextElement();
                            boolean hookSupport = (floors || eggs) && e.getName().startsWith("local/pokewilds/bugfix/Hooks");
                            boolean promptSupport = prompts && e.getName().equals("local/pokewilds/bugfix/ControllerConfirm.class");
                            if (!hookSupport && !promptSupport) continue;
                            ZipEntry ne = new ZipEntry(e.getName());
                            ne.setTime(inFile.lastModified());
                            zout.putNextEntry(ne);
                            zout.write(read(zself, e));
                            zout.closeEntry();
                            supportClasses++;
                        }
                    }
                    if (supportClasses == 0) throw new PatchException("internal error: the runtime support classes were not found next to the patcher");
                }
                ZipEntry note = new ZipEntry("META-INF/BUGFIX.txt");
                note.setTime(inFile.lastModified());
                zout.putNextEntry(note);
                zout.write(describe(applied).getBytes(StandardCharsets.UTF_8));
                zout.closeEntry();
            }
            // 4. every patch must have applied exactly as expected, or nothing is written
            check(applied, hookCalls, sprites, hooh, floors, eggs, prompts);
            if (outFile.exists() && !outFile.delete()) throw new PatchException("cannot replace " + outFile);
            if (!tmp.renameTo(outFile)) throw new PatchException("cannot write " + outFile);
        }
        System.out.println("Patched " + patchedClasses + " of " + classes + " game classes.");
        String fp = fingerprint(patchedBytes);
        boolean all = sprites && hooh && floors && eggs;
        System.out.println("Patch fingerprint: " + fp + (all && !REFERENCE_FINGERPRINT.equals("TBD")
                ? (fp.equals(REFERENCE_FINGERPRINT) ? "  (identical to the reference build)" : "  (DIFFERS from the reference build)") : ""));
        if (!outFile.getName().endsWith(".bugfix-tmp"))
            System.out.println("Wrote " + outFile + "  (file sha256 " + sha256(outFile) + ")");
    }

    /** What a complete patch of PokeWilds 0.8.11 looks like. */
    static void check(List<String> applied, Map<String, Integer> hooks, boolean sprites, boolean hooh, boolean floors, boolean eggs, boolean prompts) throws PatchException {
        List<String> problems = new ArrayList<String>();
        if (sprites) {
            need(applied, "sprites:" + BugFixAgent.UPPER, problems);
            need(applied, "sprites:" + BugFixAgent.LOWER, problems);
        }
        if (hooh) need(applied, "hooh:" + BugFixAgent.POKEMON, problems);
        if (eggs) { need(applied, "eggs:" + BugFixAgent.POKEMON_DATA_V07, problems); expect(hooks, "floorIndex", 1, problems); }
        else expect(hooks, "floorIndex", 0, problems);
        if (floors) {
            for (String c : new String[] {"PkmnMap", "DrawMiniMap", "EnterBuilding", "EscapeRope", "Game"}) need(applied, "floors-wiring:com/pkmngen/game/" + c, problems);
            need(applied, "floors-exact-load:com/pkmngen/game/PkmnMap", problems);
            need(applied, "floors-exact-save:com/pkmngen/game/util/Save", problems);
            expect(hooks, "newPokemonMap", 1, problems);
            expect(hooks, "tilesChanged", 11, problems);
            expect(hooks, "viewOwner", 40, problems);
            expect(hooks, "viewOverworld", 7, problems);
            expect(hooks, "viewAllFloors", 3, problems);
            expect(hooks, "viewSave", 4, problems);
            expect(hooks, "restoreExact", 2, problems);
            expect(hooks, "writeJsonZip", 1, problems);
        } else {
            for (String k : new String[] {"newPokemonMap", "tilesChanged", "viewOwner", "viewOverworld", "viewAllFloors", "viewSave", "restoreExact", "writeJsonZip"}) expect(hooks, k, 0, problems);
        }
        if (prompts) {
            for (String c : new String[] {"DrawControls", "DrawUseTossMenu", "DrawPokemonMenu$SelectedMenu", "DrawItemMenu$DrawGuideText", "TrainerTipsTile", "Pokemon$SetNickname", "Tile$SetSignText"})
                need(applied, "prompts:com/pkmngen/game/" + c, problems);
        }
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder("the patches did not apply as expected, nothing was written:");
            for (String p : problems) sb.append("\n  - ").append(p);
            throw new PatchException(sb.toString());
        }
    }

    private static void need(List<String> applied, String label, List<String> problems) { if (!applied.contains(label)) problems.add("missing patch " + label); }
    private static void expect(Map<String, Integer> hooks, String name, int n, List<String> problems) {
        int got = hooks.containsKey(name) ? hooks.get(name) : 0;
        if (got != n) problems.add("Hooks." + name + ": " + got + " call(s) inserted, expected " + n);
    }

    private static String describe(List<String> applied) {
        StringBuilder sb = new StringBuilder("This jar was patched by BugFix for PokeWilds 0.8.11.\n"
                + "Patches: sprites (Cut/Ride/Build facing), hooh (Ho-Oh NullPointerException), floors (one Pokemon map per floor), eggs (egg floor when saving), prompts (controller-oriented prompts).\n\nApplied:\n");
        List<String> sorted = new ArrayList<String>(applied);
        java.util.Collections.sort(sorted);
        for (String s : sorted) sb.append("  ").append(s).append('\n');
        return sb.toString();
    }

    private static Map<String, Integer> countHooks(byte[] b) {
        final Map<String, Integer> n = new TreeMap<String, Integer>();
        new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String nm, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op, String o, String f, String fd, boolean i) {
                        if (o.equals(BugFixAgent.HOOKS)) { Integer c = n.get(f); n.put(f, c == null ? 1 : c + 1); }
                    }
                };
            }
        }, 0);
        return n;
    }

    private static byte[] read(ZipFile z, ZipEntry e) throws IOException {
        try (InputStream in = z.getInputStream(e)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            for (int r; (r = in.read(buf)) > 0; ) bo.write(buf, 0, r);
            return bo.toByteArray();
        }
    }

    /** Hash of the patched classes' names and bytes: the same on every machine, whatever compresses the jar. */
    static String fingerprint(Map<String, byte[]> patched) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Map.Entry<String, byte[]> e : patched.entrySet()) {
            md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            int n = e.getValue().length;
            md.update(new byte[] {(byte) (n >>> 24), (byte) (n >>> 16), (byte) (n >>> 8), (byte) n});
            md.update(e.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** SHA-256 of a file, read as a stream (the jar is far too big to hold in memory on small devices). */
    static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(f)) {
            byte[] buf = new byte[65536];
            for (int r; (r = in.read(buf)) > 0; ) md.update(buf, 0, r);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static String sha256(byte[] data) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
