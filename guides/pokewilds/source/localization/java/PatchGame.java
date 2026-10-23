import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Narrow, reproducible display/window patches against the unmodified release. */
public class PatchGame implements Opcodes {
    static final String ZH="com/pkmngen/game/ZhText",WIN="com/pkmngen/game/ZhWindow",GAME="com/pkmngen/game/Game";
    static InsnList callGame(String method) {InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new MethodInsnNode(INVOKESTATIC,WIN,method,"(L"+GAME+";)V",false));return l;}
    static InsnList resizeHook(String method) {InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new VarInsnNode(ILOAD,1));l.add(new VarInsnNode(ILOAD,2));l.add(new MethodInsnNode(INVOKESTATIC,WIN,method,"(L"+GAME+";II)V",false));return l;}
    static boolean charAtSafe(String owner) {
        for(String n:new String[]{"DrawBuildRequirements","DrawItemPickup","DrawCraftsMenu","DrawYesNoMenu","DrawPokemonMenu$SelectedMenu","DrawUseTossMenu"})if(owner.equals("com/pkmngen/game/"+n)||owner.startsWith("com/pkmngen/game/"+n+"$"))return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        Path original=Path.of(args[0]),output=Path.of(args[1]),extras=Path.of(args[2]);
        Files.createDirectories(output.getParent());int changed=0,hooks=0;
        try(ZipFile jar=new ZipFile(original.toFile());ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))) {
            Enumeration<? extends ZipEntry> entries=jar.entries();
            while(entries.hasMoreElements()) {
                ZipEntry e=entries.nextElement();if(e.isDirectory())continue;
                byte[] data=jar.getInputStream(e).readAllBytes();String name=e.getName();
                if(Files.exists(extras.resolve(name)))continue;
                if(name.startsWith("com/pkmngen/game/")&&name.endsWith(".class")) {
                    ClassReader reader=new ClassReader(data);ClassNode c=new ClassNode(ASM8);reader.accept(c,0);boolean touched=false;
                    for(MethodNode m:c.methods) {
                        if(m.name.equals("step")&&m.desc.equals("(L"+GAME+";)V")&&Set.of("DrawBattleMenuNormal","DrawBattleMenuSafariZone","DrawAttacksMenu","DrawStatsScreen","DrawItemMenuGen2").contains(c.name.substring(c.name.lastIndexOf('/')+1))){
                            AbstractInsnNode lastReturn=null;for(AbstractInsnNode z:m.instructions.toArray())if(z.getOpcode()==RETURN)lastReturn=z;
                            for(AbstractInsnNode z:m.instructions.toArray())if(z.getOpcode()==RETURN&&(z==lastReturn||c.name.endsWith("DrawItemMenuGen2"))){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,1));l.add(new VarInsnNode(ALOAD,0));l.add(new MethodInsnNode(INVOKESTATIC,"com/pkmngen/game/ZhLabels","labels","(L"+GAME+";Lcom/pkmngen/game/Action;)V",false));m.instructions.insertBefore(z,l);touched=true;}
                        }
                        if(c.name.equals(GAME)&&m.name.equals("resize")) {m.instructions.insert(resizeHook("beforeResize"));for(AbstractInsnNode n:m.instructions.toArray())if(n.getOpcode()==RETURN)m.instructions.insertBefore(n,resizeHook("afterResize"));touched=true;}
                        if(c.name.equals(GAME)&&m.name.equals("render")){m.instructions.insert(callGame("tick"));touched=true;}
                        if(c.name.equals(GAME)&&m.name.equals("dispose")){m.instructions.insert(callGame("dispose"));touched=true;}
                        for(AbstractInsnNode n:m.instructions.toArray()) {
                            if(n instanceof MethodInsnNode x) {
                                if(x.owner.equals("javax/swing/JOptionPane")&&(x.name.equals("showConfirmDialog")||x.name.equals("showMessageDialog"))){x.owner="com/pkmngen/game/ZhDialogs";touched=true;}
                                if(x.owner.equals("com/badlogic/gdx/utils/ScreenUtils")&&x.name.equals("getFrameBufferPixmap")&&x.desc.equals("(IIII)Lcom/badlogic/gdx/graphics/Pixmap;")&&Set.of("com/pkmngen/game/Attack$Slash","com/pkmngen/game/Attack$ShadowClaw","com/pkmngen/game/Attack$Psychic","com/pkmngen/game/Attack$Lick","com/pkmngen/game/Battle$LoadAndPlayAnimation").contains(c.name)){x.owner="com/pkmngen/game/ZhFrameBuffer";x.name="capture";touched=true;}
                                if(c.name.equals("com/pkmngen/game/InputProcessor")&&x.owner.equals("com/badlogic/gdx/Input")&&x.name.equals("isKeyPressed")) {
                                    // Remove the Gdx.input receiver, keep its integer key argument.
                                    InsnList l=new InsnList();l.add(new InsnNode(SWAP));l.add(new InsnNode(POP));m.instructions.insertBefore(x,l);
                                    x.setOpcode(INVOKESTATIC);x.owner=WIN;x.name="keyDown";x.itf=false;touched=true;
                                }
                                if(x.owner.equals(GAME)&&x.name.equals("saveErrorLogAndNotifyUser")){x.owner=ZH;x.name="reportError";touched=true;}
                                if(x.owner.equals("java/lang/String")&&x.name.equals("toCharArray")&&!c.name.equals(GAME)&&!(c.name.equals("com/pkmngen/game/Pokemon$SetNickname")&&m.name.equals("<init>"))&&!(c.name.equals("com/pkmngen/game/Tile$SetSignText")&&m.name.equals("<init>"))) {
                                    String context=null;
                                    if(c.name.endsWith("/DrawAttacksMenu")&&m.name.equals("step")){
                                        if(x.getPrevious() instanceof VarInsnNode v&&v.var==4)context="typeName";
                                        for(AbstractInsnNode q=x.getPrevious(),end=null;q!=null&&q!=end;q=q.getPrevious()){
                                            if(q instanceof MethodInsnNode cat&&cat.name.equals("name")&&cat.owner.endsWith("Attack$Category")){context="categoryName";break;}
                                            if(q instanceof VarInsnNode||q instanceof LabelNode)break;
                                        }
                                    }
                                    if(c.name.endsWith("/DrawStatsScreen")&&m.name.equals("step")&&x.getPrevious() instanceof TypeInsnNode){int count=0;for(AbstractInsnNode q=x.getPrevious();q!=null&&count++<7;q=q.getPrevious())if(q instanceof FieldInsnNode f&&f.name.equals("types")){context="typeName";break;}}
                                    if(context!=null)m.instructions.insertBefore(x,new MethodInsnNode(INVOKESTATIC,"com/pkmngen/game/ZhNames",context,"(Ljava/lang/String;)Ljava/lang/String;",false));
                                    m.instructions.insertBefore(x,new MethodInsnNode(INVOKESTATIC,ZH,"translate","(Ljava/lang/String;)Ljava/lang/String;",false));touched=true;hooks++;
                                }
                                if(x.owner.equals("java/lang/String")&&x.name.equals("charAt")&&charAtSafe(c.name)) {x.setOpcode(INVOKESTATIC);x.owner=ZH;x.name=c.name.contains("DrawPokemonMenu$SelectedMenu")?"displayFieldCharAt":"displayCharAt";x.desc="(Ljava/lang/String;I)C";touched=true;hooks++;}
                                if(x.owner.equals("java/lang/String")&&x.name.equals("length")&&charAtSafe(c.name)&&(m.name.equals("step")||m.name.equals("draw"))){x.setOpcode(INVOKESTATIC);x.owner=ZH;x.name=c.name.contains("DrawPokemonMenu$SelectedMenu")?"displayFieldLength":"displayLength";x.desc="(Ljava/lang/String;)I";touched=true;}
                                if(x.owner.equals("java/lang/String")&&x.name.equals("split")&&x.desc.equals("(Ljava/lang/String;)[Ljava/lang/String;")&&(c.name.equals("com/pkmngen/game/DrawBuildRequirements")||c.name.equals("com/pkmngen/game/DrawItemPickup"))) {
                                    InsnList l=new InsnList();l.add(new InsnNode(SWAP));l.add(new MethodInsnNode(INVOKESTATIC,ZH,"translate","(Ljava/lang/String;)Ljava/lang/String;",false));l.add(new InsnNode(SWAP));m.instructions.insertBefore(x,l);touched=true;
                                }
                                if(x.owner.equals("java/lang/String")&&x.name.equals("split")&&x.desc.equals("(Ljava/lang/String;)[Ljava/lang/String;")&&(c.name.equals("com/pkmngen/game/DisplayText")||c.name.equals("com/pkmngen/game/DisplayTextIntro"))) {
                                    InsnList l=new InsnList();l.add(new InsnNode(SWAP));l.add(new MethodInsnNode(INVOKESTATIC,ZH,"dialogue","(Ljava/lang/String;)Ljava/lang/String;",false));l.add(new InsnNode(SWAP));m.instructions.insertBefore(x,l);touched=true;
                                }
                                if(c.name.equals(GAME)&&m.name.equals("create")&&x.name.equals("initInverseTextDict")){InsnList l=new InsnList();l.add(new VarInsnNode(ALOAD,0));l.add(new MethodInsnNode(INVOKESTATIC,ZH,"install","(L"+GAME+";)V",false));m.instructions.insert(x,l);touched=true;}
                                if(c.name.equals(GAME)&&m.name.equals("render")&&x.name.equals("toggleFullScreen")){x.setOpcode(INVOKESTATIC);x.owner=WIN;x.name="toggleFullscreen";x.desc="(L"+GAME+";)V";touched=true;}
                            }
                            if(n instanceof FieldInsnNode f&&f.getOpcode()==GETFIELD&&f.owner.equals("com/pkmngen/game/Pokemon")&&f.name.equals("nickname")&&(c.name.startsWith("com/pkmngen/game/Draw")||c.name.equals("com/pkmngen/game/Tile")||c.name.equals("com/pkmngen/game/Battle"))&&!(n.getNext() instanceof FieldInsnNode next&&next.getOpcode()==PUTFIELD&&next.name.equals("nickname"))){m.instructions.set(n,new MethodInsnNode(INVOKESTATIC,"com/pkmngen/game/ZhNames","displayNickname","(Lcom/pkmngen/game/Pokemon;)Ljava/lang/String;",false));touched=true;}
                            if(c.name.equals("com/pkmngen/game/desktop/DesktopLauncher")&&m.name.equals("createConfig")&&n.getOpcode()==ARETURN){InsnList l=new InsnList();l.add(new InsnNode(DUP));l.add(new MethodInsnNode(INVOKESTATIC,WIN,"configure","(Lcom/badlogic/gdx/backends/lwjgl3/Lwjgl3ApplicationConfiguration;)V",false));m.instructions.insertBefore(n,l);touched=true;}
                            if(n instanceof LdcInsnNode x && x.cst instanceof String s) {
                                String translated=switch(s){case "Save your progress?"->"要保存游戏进度吗？";case "WARNING"->"提示";case "Problem with save!"->"存档路径异常";case "A save file with the same name already exists - overwrite?"->"已存在同名存档，要覆盖吗？";case "An issue occurred while saving:\n\n"->"保存游戏时发生错误：\n\n";case "PokeWilds crashed :( reason:\n\n"->"PokeWilds 发生错误，原因：\n\n";default->null;};
                                if(translated!=null){x.cst=translated;touched=true;}
                            }
                            if(c.name.equals(GAME)&&m.name.equals("render")&&n instanceof FieldInsnNode f&&f.owner.equals("com/badlogic/gdx/math/Vector2")&&(f.name.equals("x")||f.name.equals("y"))) {
                                AbstractInsnNode prev=n.getPrevious(),a=prev==null?null:prev.getPrevious(),b=n.getNext(),d=b==null?null:b.getNext();
                                if(prev instanceof FieldInsnNode field&&field.name.equals("currScreen")&&a instanceof VarInsnNode v&&v.getOpcode()==ALOAD&&v.var==0&&b instanceof LdcInsnNode k&&Float.valueOf(6f).equals(k.cst)&&d!=null&&d.getOpcode()==FDIV){
                                    m.instructions.set(prev,new MethodInsnNode(INVOKESTATIC,WIN,f.name.equals("x")?"halfWidth":"halfHeight","(L"+GAME+";)F",false));m.instructions.remove(n);m.instructions.remove(b);m.instructions.remove(d);touched=true;
                                }
                            }
                        }
                    }
                    if(touched){ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);c.accept(writer);data=writer.toByteArray();changed++;}
                }
                out.putNextEntry(new ZipEntry(name));out.write(data);out.closeEntry();
            }
            try(var stream=Files.walk(extras)) {for(Path p:stream.filter(Files::isRegularFile).toList()){out.putNextEntry(new ZipEntry(extras.relativize(p).toString().replace('\\','/')));out.write(Files.readAllBytes(p));out.closeEntry();}}
        }
        System.out.printf("Changed classes: %d, display hooks: %d%n",changed,hooks);
    }
}
