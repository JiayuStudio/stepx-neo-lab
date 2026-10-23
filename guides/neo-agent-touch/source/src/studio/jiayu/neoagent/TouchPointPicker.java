package studio.jiayu.neoagent;

import java.util.HashSet;
import java.util.Random;

/** Small bounded touch variation. Never expands a target or repeats a point in a run. */
final class TouchPointPicker {
 private final Random random;
 private final HashSet<Long> used=new HashSet<>();
 TouchPointPicker(){this(new Random());}
 TouchPointPicker(Random random){this.random=random;}
 synchronized void reset(){used.clear();}
 synchronized long[] snapshot(){long[] result=new long[used.size()];int i=0;for(long point:used)result[i++]=point;return result;}
 synchronized void restore(long[] points){used.clear();for(long point:points)used.add(point);}
 synchronized int[] pick(int left,int top,int right,int bottom,int anchorX,int anchorY,int radius){
  int minX=Math.max(left,anchorX-radius),maxX=Math.min(right-1,anchorX+radius);
  int minY=Math.max(top,anchorY-radius),maxY=Math.min(bottom-1,anchorY+radius);
  if(minX>maxX||minY>maxY)throw new IllegalArgumentException("点击区域过小或不可见，请人工接管");
  int width=maxX-minX+1,height=maxY-minY+1,count=width*height,start=random.nextInt(count);
  for(int offset=0;offset<count;offset++){
   int index=(start+offset)%count,x=minX+index%width,y=minY+index/width;
   if(x==anchorX&&y==anchorY)continue;
   long key=((long)x<<32)^(y&0xffffffffL);
   if(used.add(key))return new int[]{x,y};
  }
  throw new IllegalArgumentException("安全点击区域内无新落点，请人工接管");
 }
}
