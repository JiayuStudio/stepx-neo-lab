import java.nio.file.*;
import java.util.zip.*;
import local.pokewilds.bugfix.asm.*;

/** Suppress only asset-loading static initializers in the headless test fixtures. */
public final class PrepareHeadlessGame {
    public static void main(String[] args) throws Exception {
        try (ZipFile jar = new ZipFile(args[0])) {
            for (String name : new String[]{"Player", "Battle", "PkmnMap"}) {
                String entry = "com/pkmngen/game/" + name + ".class";
                ClassReader reader = new ClassReader(jar.getInputStream(jar.getEntry(entry)));
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                        if (!name.equals("<clinit>")) return mv;
                        mv.visitCode(); mv.visitInsn(Opcodes.RETURN); mv.visitMaxs(0, 0); mv.visitEnd();
                        return null;
                    }
                }, 0);
                Path output = Paths.get(args[1], entry);
                Files.createDirectories(output.getParent());
                Files.write(output, writer.toByteArray());
            }
        }
    }
}
