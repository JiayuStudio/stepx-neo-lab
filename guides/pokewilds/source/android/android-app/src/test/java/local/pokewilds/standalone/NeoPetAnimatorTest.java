package local.pokewilds.standalone;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class NeoPetAnimatorTest {
 private int[][] poses(){int[][] p=new int[2][391];for(int y=8;y<20;y++)for(int x=4;x<13;x++){p[0][y*17+x]=255;p[1][y*17+x+1]=200;}return p;}
 @Test public void allActionsStayInHardwareRangeOverFullCycles(){for(String a:new String[]{"idle","feed","pet","play","sleep"})for(int n=0;n<300;n++){int[] f=NeoPetAnimator.render(poses(),a,n/30.0);assertEquals(391,f.length);for(int v:f)assertTrue(v>=0&&v<=255);}}
 @Test public void motionAndInteractionHaveDifferentFrames(){int[][] p=poses();assertFalse(Arrays.equals(NeoPetAnimator.render(p,"idle",1),NeoPetAnimator.render(p,"idle",1.5)));assertFalse(Arrays.equals(NeoPetAnimator.render(p,"feed",1),NeoPetAnimator.render(p,"idle",1)));assertFalse(Arrays.equals(NeoPetAnimator.render(p,"sleep",1),NeoPetAnimator.render(p,"play",1)));}
}
