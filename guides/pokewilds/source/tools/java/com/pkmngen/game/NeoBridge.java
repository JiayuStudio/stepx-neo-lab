package com.pkmngen.game;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.pkmngen.game.Network;
import com.pkmngen.game.util.Save;
/** Called only on the game's render thread: never races game state. */
public final class NeoBridge {
 private static long nextSave=System.currentTimeMillis()+60000,nextState,retryAfter,savedAt;
 private static String saveStatus="等待进入世界";
 private static final Path request=Paths.get("neo-save.request"), state=Paths.get("neo-state.properties");
 public static void tick(Game g) {
  long now=System.currentTimeMillis();
  if(now>=retryAfter && g.map!=null && g.player!=null && g.playerCanMove && g.player.canMove && g.player.acceptInput
     && (g.battle==null||!g.actionStack.contains(g.battle.drawAction))
     && (now>=nextSave||Files.exists(request))) {
   try{
    long began=System.nanoTime();saveAtomic(g);Files.deleteIfExists(request);
    savedAt=System.currentTimeMillis();nextSave=savedAt+60000;retryAfter=0;
    saveStatus="自动保存成功 · "+String.format(java.util.Locale.ROOT,"%.1f",(System.nanoTime()-began)/1e9)+"秒";
   }catch(Exception e){
    saveStatus="自动保存失败，15秒后重试: "+e.getClass().getSimpleName();retryAfter=now+15000;
    System.err.println("Neo: "+saveStatus);
   }
  }
  // Publish failures independently: an unsuccessful save must still reach the status page.
  if(now>=nextState){nextState=now+1000;try{publish(g,now);}catch(Exception e){System.err.println("Neo: state unavailable "+e.getClass().getSimpleName());}}
 }
 public static void saveForReload(Game g)throws Exception {saveAtomic(g);}
 public static void prepareLoad(DrawSetupMenu menu,Game g){
  Path file=Paths.get("neo-load.request");if(!Files.isRegularFile(file))return;
  try{
   Properties p=new Properties();try(Reader in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){p.load(in);}
   String name=p.getProperty("file","");if(name.contains("/")||name.contains("\\")||!name.endsWith(".sav")||!Files.isDirectory(Paths.get(name),LinkOption.NOFOLLOW_LINKS))throw new IOException("Invalid saved world");
   int index=menu.fileNames.indexOf(name);if(index<0)throw new IOException("Saved world not listed");
   menu.newLoadIndex=1;menu.fileIndex=index;DrawSetupMenu.currIndex=3;DrawSetupMenu.lastIndex=3;InputProcessor.startJustPressed=true;
   Files.deleteIfExists(file);Files.writeString(Paths.get("neo-load.started"),name,StandardCharsets.UTF_8);
  }catch(Exception e){try{Files.deleteIfExists(file);}catch(IOException ignored){}ZhDialogs.showMessageDialog(null,"无法读取所选世界，请检查存档是否完整。");}
 }
 public static void refreshInputEdges(){
  if(!InputProcessor.acceptInput)return;
  InputProcessor.upJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardUp);
  InputProcessor.downJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardDown);
  InputProcessor.leftJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardLeft);
  InputProcessor.rightJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardRight);
  InputProcessor.aJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardA);
  InputProcessor.bJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardB);
  InputProcessor.startJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardStart);
  InputProcessor.lJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardL);
  InputProcessor.rJustPressed|=com.badlogic.gdx.Gdx.input.isKeyJustPressed(InputProcessor.keyboardR);
 }
 private static void saveAtomic(Game g) throws Exception {
  // Stage a complete world snapshot, then rename. Preserve all other maps.
  Path dest=Paths.get(g.map.id+".sav"), stage=Paths.get(g.map.id+".neo-stage"), prev=Paths.get(g.map.id+".neo-backup");
  if(!Files.exists(dest) && Files.exists(prev)) Files.move(prev,dest);
  delete(stage); Files.createDirectories(stage);
  if(Files.isDirectory(dest)) try(java.util.stream.Stream<Path> walk=Files.walk(dest)) {
   for(Path p:(Iterable<Path>)walk::iterator){ Path t=stage.resolve(dest.relativize(p)); if(Files.isDirectory(p))Files.createDirectories(t);else Files.copy(p,t); }
  }
  String base=stage.toString()+"/";
  Save.saveData(new Network.GameSaveData(g),base+"game");
  Save.saveData(new Network.MapSaveData(g),base+"map"+g.map.currMapId);
  Save.saveData(new Network.PlayerSpawnLocationData(g.player),base+"spawn"+g.player.network.id+g.map.currMapId);
  Save.saveMinimap(g.map,base+"map"+g.map.currMapId+".png");
  try(java.util.stream.Stream<Path> walk=Files.walk(stage)) {for(Path p:(Iterable<Path>)walk::iterator)if(p.toString().endsWith(".json.zip"))try(ZipFile z=new ZipFile(p.toFile())) {ZipEntry en=z.getEntry("data.json");if(en==null)throw new IOException("Missing JSON");try(InputStream in=z.getInputStream(en)){while(in.read(new byte[8192])!=-1){}}}}
  delete(prev); if(Files.exists(dest))Files.move(dest,prev,StandardCopyOption.ATOMIC_MOVE);
  try{Files.move(stage,dest,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){if(!Files.exists(dest)&&Files.exists(prev))Files.move(prev,dest);throw e;}
 }
 private static void delete(Path p)throws IOException {if(Files.exists(p))try(java.util.stream.Stream<Path>w=Files.walk(p)){for(Path x:(Iterable<Path>)w.sorted(Comparator.reverseOrder())::iterator)Files.delete(x);}}
 private static void publish(Game g,long now)throws IOException {
  Properties p=new Properties(); p.setProperty("updated",Long.toString(now));p.setProperty("save",saveStatus);p.setProperty("savedAt",Long.toString(savedAt));
  p.setProperty("activity",g.battle!=null && g.actionStack.contains(g.battle.drawAction)?"battle":g.playerCanMove?"explore":"rest");
  if(g.player!=null){int i=0; for(Pokemon mon:g.player.pokemon){p.setProperty("pet."+i,mon.specie.name);p.setProperty("level."+i,Integer.toString(mon.level));p.setProperty("hp."+i,Integer.toString(mon.currentStats.get("hp")));i++;}p.setProperty("count",Integer.toString(i));}
  Path tmp=Paths.get("neo-state.tmp");try(Writer w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){p.store(w,"Neo companion");}Files.move(tmp,state,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
 }
}
