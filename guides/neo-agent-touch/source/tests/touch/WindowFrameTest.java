package studio.jiayu.neoagent;
import java.util.Arrays;
public final class WindowFrameTest {
 static int checks;
 static void expect(int[] root,int[] surface,int w,int h,int[] wanted){
  if(!Arrays.equals(WindowFrame.resolve(root,surface,w,h),wanted))throw new AssertionError("Wrong visible window frame");checks++;
 }
 public static void main(String[] args){
  int[] screen={0,0,1200,2670};
  expect(new int[]{0,0,0,0},screen,1200,2670,screen);
  expect(new int[]{0,0,0,0},null,1200,2670,null);
  expect(new int[]{0,140,1200,2500},null,1200,2670,new int[]{0,140,1200,2500});
  expect(new int[]{0,140,1200,2500},screen,1200,2670,screen);
  expect(screen,new int[]{-50,-80,1250,2800},1200,2670,screen);
  expect(screen,new int[]{800,200,1200,1100},1200,2670,new int[]{800,200,1200,1100});
  expect(screen,new int[]{1200,0,1600,1000},1200,2670,screen);
  expect(screen,new int[]{400,500,200,300},1200,2670,screen);
  expect(null,new int[]{0,0,300,400},1200,2670,new int[]{0,0,300,400});
  expect(null,null,1200,2670,null);
  expect(screen,screen,0,2670,null);
  expect(new int[]{Integer.MIN_VALUE,0,Integer.MAX_VALUE,2670},null,1200,2670,screen);
  System.out.println("PASS: WindowFrame "+checks+" boundaries");
 }
}
