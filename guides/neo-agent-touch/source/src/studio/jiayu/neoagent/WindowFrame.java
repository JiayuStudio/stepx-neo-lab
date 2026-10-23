package studio.jiayu.neoagent;

/** Accessibility window geometry can remain valid when a content root has no bounds. */
final class WindowFrame {
 static int[] resolve(int[] root,int[] window,int width,int height){
  int[] surface=clip(window,width,height);
  return surface==null?clip(root,width,height):surface;
 }
 private static int[] clip(int[] box,int width,int height){
  if(box==null||box.length!=4||width<=0||height<=0)return null;
  int left=Math.max(0,box[0]),top=Math.max(0,box[1]);
  int right=Math.min(width,box[2]),bottom=Math.min(height,box[3]);
  return right>left&&bottom>top?new int[]{left,top,right,bottom}:null;
 }
}
