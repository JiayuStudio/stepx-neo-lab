package local.pokewilds.standalone;
import org.junit.Test;import static org.junit.Assert.*;
public class NeoConsoleLayoutTest {
 private boolean overlaps(NeoConsoleLayout.Box a,NeoConsoleLayout.Box b){return a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y;}
 @Test public void viewportAndPrimaryControlsRemainInsideAndSeparate(){
  int[][] sizes={{320,640},{360,800},{412,915},{640,320},{800,360},{915,412},{600,960},{960,600},{320,568}};
  for(int[] size:sizes){NeoConsoleLayout l=new NeoConsoleLayout(size[0],size[1],1);
   for(NeoConsoleLayout.Box b:new NeoConsoleLayout.Box[]{l.screen,l.dpad,l.a,l.b,l.start,l.select,l.menu,l.rotate}){
    assertTrue(b.x>=0);assertTrue(b.y>=0);assertTrue(b.x+b.w<=size[0]+1);assertTrue(b.y+b.h<=size[1]+1);
    if(b!=l.screen)assertFalse(overlaps(l.screen,b));
   }
   for(NeoConsoleLayout.Box utility:l.utilities){
    assertTrue(utility.x>=0&&utility.y>=0&&utility.x+utility.w<=size[0]+1&&utility.y+utility.h<=size[1]+1);
    for(NeoConsoleLayout.Box primary:new NeoConsoleLayout.Box[]{l.screen,l.dpad,l.a,l.b,l.select,l.start})assertFalse("utility overlaps a primary control",overlaps(utility,primary));
   }
   for(int i=0;i<l.utilities.length;i++)for(int j=0;j<i;j++)assertFalse(overlaps(l.utilities[i],l.utilities[j]));
   assertFalse(overlaps(l.dpad,l.select));assertFalse(overlaps(l.dpad,l.start));
   assertFalse(overlaps(l.menu,l.rotate));assertFalse(overlaps(l.a,l.b));assertFalse(overlaps(l.start,l.select));
  }
 }
 @Test public void compactDockKeepsOriginalLandscapeViewport(){NeoConsoleLayout l=new NeoConsoleLayout(2670,1200,3.25f);assertEquals(1200-94*3.25f,l.screen.h,.01);assertEquals(7,l.utilities.length);assertEquals("skills",NeoConsoleLayout.UTILITY_COMMANDS[4]);assertEquals("settings",NeoConsoleLayout.UTILITY_COMMANDS[5]);}
 @Test public void portraitUsesAtLeastNinetyPercentWidthAndLargeKeys(){NeoConsoleLayout l=new NeoConsoleLayout(1200,2672,3.25f);assertTrue(l.screen.w>=1080);assertTrue(l.dpad.w>=420);assertTrue(l.a.w>=200);}
 @Test public void portraitMatchesClassicGbKeyPlacement(){NeoConsoleLayout l=new NeoConsoleLayout(1080,2400,3);assertTrue(l.portrait);assertTrue(l.dpad.cx()<l.b.cx());assertTrue(l.b.cx()<l.a.cx());assertTrue(l.a.cy()<l.b.cy());assertTrue(l.start.cy()>l.select.cy());assertTrue(l.select.y>l.dpad.y+l.dpad.h);}
}
