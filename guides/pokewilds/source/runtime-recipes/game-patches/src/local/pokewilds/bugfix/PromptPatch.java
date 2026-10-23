// SPDX-License-Identifier: MIT
package local.pokewilds.bugfix;

import java.util.HashMap;
import java.util.Map;

import local.pokewilds.bugfix.asm.ClassReader;
import local.pokewilds.bugfix.asm.ClassVisitor;
import local.pokewilds.bugfix.asm.ClassWriter;
import local.pokewilds.bugfix.asm.Label;
import local.pokewilds.bugfix.asm.MethodVisitor;
import local.pokewilds.bugfix.asm.Opcodes;

/** Controller-oriented wording and configured-Start confirmation for the two text editors. */
final class PromptPatch {
    private static final String INPUT = "com/badlogic/gdx/Input";
    private static final String CONTROLLER_CONFIRM = "local/pokewilds/bugfix/ControllerConfirm";

    private PromptPatch() { }

    private static Map<String, String> replacements(String className) {
        Map<String, String> result = new HashMap<String, String>();
        if (className.equals("com/pkmngen/game/DrawControls")) {
            result.put("Arrows  - Movement", "D-pad   - Movement");
            result.put("Z       - A button", "A       - Action");
            result.put("X       - B button", "B       - Cancel");
            result.put("Enter   - Menu", "Start   - Menu");
            result.put("Hold X to run", "Hold B to run");
        } else if (className.equals("com/pkmngen/game/DrawUseTossMenu")) {
            result.put("Press Z to cast the line.", "Press A to cast the line.");
            result.put("Press Z to plant seeds.", "Press A to plant seeds.");
            result.put("Press Z to fertilize saplings and small trees.", "Press A to fertilize saplings and small trees.");
        } else if (className.equals("com/pkmngen/game/DrawPokemonMenu$SelectedMenu")) {
            result.put(" used DIG! Press C and V to select terrain.", " used DIG! Press L1 and R1 to select terrain.");
            result.put(" used BUILD! Press C and V to select tiles.", " used BUILD! Press L1 and R1 to select tiles.");
            result.put(" used PAINT! Press C and V to select designs.", " used PAINT! Press L1 and R1 to select designs.");
            result.put(" is using POWER! Power machinery by pressing Z.", " is using POWER! Power machinery by pressing A.");
        } else if (className.equals("com/pkmngen/game/DrawItemMenu$DrawGuideText")) {
            result.put("Stand still while holding X to stop using a Field Move.",
                    "Stand still while holding B to stop using a Field Move.");
        } else if (className.equals("com/pkmngen/game/TrainerTipsTile")) {
            result.put("Stand still while holding X to stop using a Field Move.",
                    "Stand still while holding B to stop using a Field Move.");
        } else if (className.equals("com/pkmngen/game/Tile")) {
            result.put("Arrow left or right to change clothes color.",
                    "Use D-pad left or right to change clothes color.");
            result.put("Arrow left or right to change appearance.",
                    "Use D-pad left or right to change appearance.");
        } else if (className.equals("com/pkmngen/game/Pokemon$SetNickname")) {
            result.put("Press enter to set", "Press Start to set");
        } else if (className.equals("com/pkmngen/game/Tile$SetSignText")) {
            result.put("Press Enter to set text", "Press Start to set text");
        }
        return result;
    }

    static byte[] transform(String className, byte[] original, java.util.List<String> applied) {
        Map<String, String> rewrites = replacements(className);
        boolean editor = className.equals("com/pkmngen/game/Pokemon$SetNickname")
                || className.equals("com/pkmngen/game/Tile$SetSignText");
        if (rewrites.isEmpty()) return null;

        final int[] literalHits = {0};
        final int[] startHooks = {0};
        final Label[] nicknameContinue = {null};
        final boolean[] enterKeycode = {false};
        final boolean[] awaitEditorBranch = {false};
        ClassReader cr = new ClassReader(original);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int access, String methodName, String desc, String sig, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, methodName, desc, sig, exceptions);
                if (!editor || !methodName.equals("step")) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitLdcInsn(Object value) {
                            if (value instanceof String && rewrites.containsKey(value)) {
                                super.visitLdcInsn(rewrites.get(value));
                                literalHits[0]++;
                            } else super.visitLdcInsn(value);
                        }
                    };
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override public void visitLdcInsn(Object value) {
                        if (value instanceof String && rewrites.containsKey(value)) {
                            super.visitLdcInsn(rewrites.get(value));
                            literalHits[0]++;
                        } else super.visitLdcInsn(value);
                    }

                    @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        if (opcode == Opcodes.INVOKEINTERFACE && owner.equals(INPUT)
                                && name.equals("isKeyJustPressed") && descriptor.equals("(I)Z") && enterKeycode[0]) {
                            // Preserve stock Enter and accept a one-shot controller request for this editor.
                            super.visitVarInsn(Opcodes.ALOAD, 0);
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, CONTROLLER_CONFIRM, "consume",
                                    "(ZLjava/lang/Object;)Z", false);
                            startHooks[0]++;
                            awaitEditorBranch[0] = true;
                        }
                        enterKeycode[0] = false;
                    }

                    @Override public void visitIntInsn(int opcode, int operand) {
                        super.visitIntInsn(opcode, operand);
                        enterKeycode[0] = opcode == Opcodes.BIPUSH && operand == 66;
                    }

                    @Override public void visitJumpInsn(int opcode, Label label) {
                        super.visitJumpInsn(opcode, label);
                        if (awaitEditorBranch[0] && opcode == Opcodes.IFEQ && nicknameContinue[0] == null) {
                            nicknameContinue[0] = label;
                            awaitEditorBranch[0] = false;
                        }
                    }

                    @Override public void visitLabel(Label label) {
                        if (className.equals("com/pkmngen/game/Pokemon$SetNickname") && label == nicknameContinue[0]) {
                            // Confirmation disables the editor; stop this frame before a simultaneous
                            // character press can also be appended as a nickname character.
                            super.visitInsn(Opcodes.RETURN);
                        }
                        super.visitLabel(label);
                    }
                };
            }
        }, 0);
        int expectedLiterals = expectedLiterals(className);
        int expectedHooks = editor ? 1 : 0;
        if (literalHits[0] != expectedLiterals || startHooks[0] != expectedHooks) return null;
        if (applied != null) applied.add("prompts:" + className);
        return cw.toByteArray();
    }

    private static int expectedLiterals(String className) {
        if (className.equals("com/pkmngen/game/DrawControls")) return 5;
        if (className.equals("com/pkmngen/game/DrawUseTossMenu")) return 3;
        if (className.equals("com/pkmngen/game/DrawPokemonMenu$SelectedMenu")) return 4;
        if (className.equals("com/pkmngen/game/DrawItemMenu$DrawGuideText")) return 1;
        if (className.equals("com/pkmngen/game/TrainerTipsTile")) return 2;
        if (className.equals("com/pkmngen/game/Tile")) return 2;
        if (className.equals("com/pkmngen/game/Pokemon$SetNickname")
                || className.equals("com/pkmngen/game/Tile$SetSignText")) return 1;
        return 0;
    }
}
