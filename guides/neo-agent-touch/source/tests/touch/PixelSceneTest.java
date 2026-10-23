package studio.jiayu.neoagent;
import java.util.*;
public final class PixelSceneTest {
 static int count;static void yes(boolean result){if(!result)throw new AssertionError("Wrong visual scene feedback");count++;}
 public static void main(String[] args){
  int[] board=new int[8192];Arrays.fill(board,0xff202020);PixelScene scenes=new PixelScene();String first=scenes.identify("opaque-root",board);
  yes(first.equals(scenes.identify("opaque-root",board.clone())));
  int[] cursor=board.clone();cursor[100]=cursor[101]=0xffffffff;yes(first.equals(scenes.identify("opaque-root",cursor)));
  int[] changed=board.clone();for(int i=100;i<120;i++)changed[i]=0xffddcc55;String next=scenes.identify("opaque-root",changed);
  yes(!first.equals(next));yes(first.equals(scenes.identify("opaque-root",board)));
  yes(!next.equals(scenes.identify("changed-native-editor",changed)));
  int[] tint=board.clone();for(int i=0;i<tint.length;i++)tint[i]=0xff242424;yes(PixelScene.similar(board,tint));
  int[] masked=board.clone(),movedMask=board.clone();for(int i=100;i<700;i++)masked[i]=0;for(int i=500;i<900;i++)movedMask[i]=0;yes(PixelScene.similar(masked,movedMask));yes(!PixelScene.similar(new int[8192],board));
  yes(!PixelScene.similar(null,board));yes(!PixelScene.similar(board,new int[12]));
  changed[100]=0xff000000;yes(!first.equals(scenes.identify("opaque-root",changed)));
  for(int i=0;i<16;i++)scenes.identify("screen-"+i,board);yes(!first.equals(scenes.identify("opaque-root",board)));
  scenes.clear();yes(scenes.identify("opaque-root",board).equals("visual-scene-1"));
  System.out.println("PASS: PixelScene "+count+" opaque screen changes, cursor noise and bounded-history checks");
 }
}
