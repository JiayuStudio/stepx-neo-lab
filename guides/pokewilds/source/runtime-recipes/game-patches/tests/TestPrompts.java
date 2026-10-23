package local.pokewilds.bugfix;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import local.pokewilds.bugfix.asm.ClassReader;
import local.pokewilds.bugfix.asm.ClassVisitor;
import local.pokewilds.bugfix.asm.MethodVisitor;
import local.pokewilds.bugfix.asm.Opcodes;

/** Verifies the opt-in controller wording and configured-Start editor hooks on 0.8.11 classes. */
public final class TestPrompts {
    private static int failures;

    private static byte[] bytes(ZipFile jar, String name) throws Exception {
        ZipEntry entry = jar.getEntry(name + ".class");
        if (entry == null) throw new Exception("missing class " + name);
        try (InputStream in = jar.getInputStream(entry)) { return in.readAllBytes(); }
    }

    private static void check(boolean ok, String what) {
        if (!ok) { failures++; System.out.println("FAIL " + what); }
    }

    private static Set<String> strings(byte[] bytes) {
        final Set<String> result = new HashSet<String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitLdcInsn(Object value) { if (value instanceof String) result.add((String)value); }
                };
            }
        }, 0);
        return result;
    }

    private static int countControllerHooks(byte[] bytes) {
        final int[] count = {0};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op, String owner, String method, String desc, boolean itf) {
                        if (op == Opcodes.INVOKESTATIC && owner.equals("local/pokewilds/bugfix/ControllerConfirm")
                                && method.equals("consume") && desc.equals("(ZLjava/lang/Object;)Z")) count[0]++;
                    }
                };
            }
        }, 0);
        return count[0];
    }

    private static List<String> patch(String name, byte[] input, boolean prompts) {
        List<String> labels = new ArrayList<String>();
        byte[] output = BugFixAgent.transformClass(name, input, false, false, false, false, prompts, labels);
        if (prompts) check(output != null, name + " transformed");
        else check(output == null, name + " unchanged with prompt patch disabled");
        return labels;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: TestPrompts official-pokewilds.jar");
        String[] targets = {
            "com/pkmngen/game/DrawControls", "com/pkmngen/game/DrawUseTossMenu",
            "com/pkmngen/game/DrawPokemonMenu$SelectedMenu", "com/pkmngen/game/DrawItemMenu$DrawGuideText",
            "com/pkmngen/game/TrainerTipsTile", "com/pkmngen/game/Pokemon$SetNickname",
            "com/pkmngen/game/Tile$SetSignText", "com/pkmngen/game/Tile"
        };
        String[][] expectedStrings = {
            {"D-pad   - Movement", "A       - Action", "B       - Cancel", "Start   - Menu", "Hold B to run"},
            {"Press A to cast the line.", "Press A to plant seeds.", "Press A to fertilize saplings and small trees."},
            {" used DIG! Press L1 and R1 to select terrain.", " used BUILD! Press L1 and R1 to select tiles.",
                " used PAINT! Press L1 and R1 to select designs.", " is using POWER! Power machinery by pressing A."},
            {"Stand still while holding B to stop using a Field Move."},
            {"Stand still while holding B to stop using a Field Move."},
            {"Press Start to set"}, {"Press Start to set text"},
            {"Use D-pad left or right to change clothes color.", "Use D-pad left or right to change appearance."}
        };
        String glyphs = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 <>_?!.,éÉ-'";
        try (ZipFile jar = new ZipFile(args[0])) {
            for (String name : targets) {
                byte[] original = bytes(jar, name);
                patch(name, original, false);
                List<String> applied = patch(name, original, true);
                check(applied.contains("prompts:" + name), name + " has prompt patch label");
                byte[] output = BugFixAgent.transformClass(name, original, false, false, false, false, true, null);
                Set<String> rewrittenStrings = strings(output);
                for (String expected : expectedStrings[Arrays.asList(targets).indexOf(name)]) {
                    check(rewrittenStrings.contains(expected), name + " contains " + expected);
                    for (int i = 0; i < expected.length(); i++)
                        check(glyphs.indexOf(expected.charAt(i)) >= 0, name + " uses a glyph supported by Game.initTextDict");
                    for (String word : expected.split(" "))
                        check(word.length() < 19, name + " has no unbreakable word wider than the 18-character DisplayText line");
                    if (name.endsWith("DrawControls"))
                        check(expected.length() <= 20, name + " overlay row fits within 20 characters");
                }
                if (name.endsWith("SetNickname") || name.endsWith("SetSignText")) {
                    check(countControllerHooks(output) == 1, name + " calls the controller confirmation hook exactly once");
                    check(strings(output).contains(name.endsWith("SetNickname") ? "Press Start to set" : "Press Start to set text"), name + " prompt names Start");
                }
            }
            byte[] displayText = bytes(jar, "com/pkmngen/game/DisplayText");
            check(BugFixAgent.transformClass("com/pkmngen/game/DisplayText", displayText, false, false, false, false, true, null) == null,
                    "unlisted DisplayText strings remain unchanged");
        }
        Object editorA = new Object(), editorB = new Object();
        ControllerConfirm.request(editorA);
        check(!ControllerConfirm.consume(false, editorB), "a controller request cannot confirm another editor");
        check(ControllerConfirm.consume(false, editorA), "the matching editor consumes a controller request");
        check(!ControllerConfirm.consume(false, editorA), "controller confirmation is one-shot");
        check(ControllerConfirm.consume(true, editorA), "stock Enter confirmation remains accepted");
        Object remappedLetterEditor = new Object();
        check(!ControllerConfirm.consume(false, remappedLetterEditor), "a remapped letter key does not confirm without a bridge request");
        if (failures == 0) System.out.println("PASS prompt replacements, opt-out, and controller editor confirmations");
        else System.out.println("FAILURES: " + failures);
        System.exit(failures == 0 ? 0 : 1);
    }
}
