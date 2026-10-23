import java.nio.file.*;import java.util.jar.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public class PatchNeo {
 public static void main(String[]args)throws Exception{
 try(JarFile in=new JarFile(args[0]);JarOutputStream out=new JarOutputStream(Files.newOutputStream(Paths.get(args[1])))){
  var entries=in.entries();while(entries.hasMoreElements()){var e=entries.nextElement();if(e.isDirectory())continue;byte[] b=in.getInputStream(e).readAllBytes();
   if(e.getName().equals("com/pkmngen/game/Game.class")){ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);for(MethodNode m:c.methods)if(m.name.equals("render")&&m.desc.equals("()V")){InsnList l=new InsnList();l.add(new VarInsnNode(Opcodes.ALOAD,0));l.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/pkmngen/game/NeoBridge","tick","(Lcom/pkmngen/game/Game;)V",false));m.instructions.insert(l);}ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();}
   if(e.getName().equals("com/pkmngen/game/DrawSetupMenu.class")||e.getName().equals("com/pkmngen/game/InputProcessor.class")){
    ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);
    for(MethodNode m:c.methods)if(m.name.equals("step")&&m.desc.equals("(Lcom/pkmngen/game/Game;)V")){
     if(c.name.endsWith("DrawSetupMenu")){
      InsnList hook=new InsnList();hook.add(new VarInsnNode(Opcodes.ALOAD,0));hook.add(new VarInsnNode(Opcodes.ALOAD,1));hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/pkmngen/game/NeoBridge","prepareLoad","(Lcom/pkmngen/game/DrawSetupMenu;Lcom/pkmngen/game/Game;)V",false));m.instructions.insert(hook);
     }else for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==Opcodes.RETURN)m.instructions.insertBefore(n,new MethodInsnNode(Opcodes.INVOKESTATIC,"com/pkmngen/game/NeoBridge","refreshInputEdges","()V",false));
    }
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
   }
   if(e.getName().equals("com/pkmngen/game/DrawControls.class")){
    ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);int wrap=0,position=0;
    for(MethodNode m:c.methods)if(m.name.equals("step"))for(AbstractInsnNode n:m.instructions.toArray()){
     // 19 glyphs at x=4..148 leave four logical pixels on both edges of the 160px canvas.
     if(n instanceof IntInsnNode i&&i.operand==20){i.operand=18;wrap++;}
     if(position==0&&n instanceof MethodInsnNode call&&call.owner.equals("com/badlogic/gdx/graphics/g2d/Sprite")&&call.name.equals("setPosition")){
      InsnList offset=new InsnList();offset.add(new InsnNode(Opcodes.SWAP));offset.add(new LdcInsnNode(4f));offset.add(new InsnNode(Opcodes.FADD));offset.add(new InsnNode(Opcodes.SWAP));m.instructions.insertBefore(call,offset);position++;
     }
    }
    if(wrap!=2||position!=1)throw new IllegalStateException("Unexpected trainer-tip layout: "+wrap+"/"+position);
    ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();
   }
   if(e.getName().startsWith("com/pkmngen/game/desktop/DesktopLauncher$")&&e.getName().endsWith(".class")){
    ClassNode c=new ClassNode();new ClassReader(b).accept(c,0);boolean changed=false;
    for(MethodNode m:c.methods)if(m.name.equals("closeRequested")&&m.desc.equals("()Z")){
     m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();
     m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"com/pkmngen/game/ZhDialogs","closeRequested","()Z",false));m.instructions.add(new InsnNode(Opcodes.IRETURN));changed=true;
    }
    if(changed){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);b=w.toByteArray();}
   }
   if(e.getName().equals("com/pkmngen/game/ZhDialogs.class"))b=Files.readAllBytes(Paths.get(args[2]).resolveSibling("ZhDialogs.class"));
   out.putNextEntry(new JarEntry(e.getName()));out.write(b);out.closeEntry();
  }
  out.putNextEntry(new JarEntry("com/pkmngen/game/NeoBridge.class"));out.write(Files.readAllBytes(Paths.get(args[2])));out.closeEntry();
 }
 }
}
