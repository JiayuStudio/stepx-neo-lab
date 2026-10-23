package studio.jiayu.neoagent;
import java.util.*;

/** Bounded visual feedback for canvas/opaque UIs; never a completion or action permission. */
final class PixelScene {
 static final int WIDTH=64,HEIGHT=128;
 private static final class Scene {
  final String nativeKey,id;final int[] pixels;
  Scene(String nativeKey,String id,int[] pixels){this.nativeKey=nativeKey;this.id=id;this.pixels=pixels.clone();}
 }
 private final ArrayDeque<Scene> recent=new ArrayDeque<>();private int sequence;
 void clear(){recent.clear();sequence=0;}
 String identify(String nativeKey,int[] pixels){
  if(pixels==null||pixels.length==0)return nativeKey;
  for(Scene scene:recent)if(scene.nativeKey.equals(nativeKey)&&similar(scene.pixels,pixels))return scene.id;
  String id="visual-scene-"+(++sequence);recent.addLast(new Scene(nativeKey,id,pixels));while(recent.size()>12)recent.removeFirst();return id;
 }
 static boolean similar(int[] before,int[] after){
  if(before==null||after==null||before.length==0||before.length!=after.length)return false;
  int visible=0;for(int i=0;i<before.length;i++)if((before[i]>>>24)!=0&&(after[i]>>>24)!=0)visible++;
  if(visible<Math.max(3,before.length/5))return false;
  int changed=0,limit=Math.max(3,visible/1600);
  for(int i=0;i<before.length;i++){
   int a=before[i],b=after[i];boolean different=false;
   if((a>>>24)==0||(b>>>24)==0)continue;
   for(int shift=0;shift<=16;shift+=8)if(Math.abs(((a>>>shift)&255)-((b>>>shift)&255))>32)different=true;
   if(different&&++changed>=limit)return false;
  }return true;
 }
}
