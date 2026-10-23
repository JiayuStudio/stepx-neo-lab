package studio.jiayu.neoagent;

/** Display-relative geometry; automatic avoidance never changes the user's saved position. */
public final class FloatingChatGeometry {
 private FloatingChatGeometry(){}
 private static int clamp(int value,int low,int high){return Math.max(low,Math.min(Math.max(low,high),value));}
 private static float unit(float value,float fallback){return Float.isNaN(value)||Float.isInfinite(value)?fallback:Math.max(0,Math.min(1,value));}
 public static int[] position(int screenWidth,int screenHeight,int width,int height,int inset,float x,float y){int pad=Math.max(0,Math.min(inset,Math.max(0,Math.min(screenWidth-width,screenHeight-height)/2)));int maxX=Math.max(pad,screenWidth-width-pad),maxY=Math.max(pad,screenHeight-height-pad);return new int[]{pad+Math.round((maxX-pad)*unit(x,1)),pad+Math.round((maxY-pad)*unit(y,.5f))};}
 public static float[] normalize(int screenWidth,int screenHeight,int width,int height,int inset,int x,int y){int[] low=position(screenWidth,screenHeight,width,height,inset,0,0),high=position(screenWidth,screenHeight,width,height,inset,1,1);return new float[]{unit((x-low[0])/(float)Math.max(1,high[0]-low[0]),1),unit((y-low[1])/(float)Math.max(1,high[1]-low[1]),.5f)};}
 private static boolean intersects(int x,int y,int width,int height,int left,int top,int right,int bottom){return x<right&&x+width>left&&y<bottom&&y+height>top;}
 public static int[] avoid(int screenWidth,int screenHeight,int width,int height,int inset,int x,int y,int left,int top,int right,int bottom){if(!intersects(x,y,width,height,left,top,right,bottom))return new int[]{x,y};int[] low=position(screenWidth,screenHeight,width,height,inset,0,0),high=position(screenWidth,screenHeight,width,height,inset,1,1);int[][] options={{x,low[1]},{x,high[1]},{low[0],y},{high[0],y},{low[0],low[1]},{high[0],low[1]},{low[0],high[1]},{high[0],high[1]}};for(int[] p:options){p[0]=clamp(p[0],low[0],high[0]);p[1]=clamp(p[1],low[1],high[1]);if(!intersects(p[0],p[1],width,height,left,top,right,bottom))return p;}return null;}
}
