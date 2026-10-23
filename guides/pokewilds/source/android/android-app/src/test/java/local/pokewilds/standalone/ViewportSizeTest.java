package local.pokewilds.standalone;

import org.junit.Test;
import static org.junit.Assert.*;

public class ViewportSizeTest {
    @Test public void widescreenUsesMoreWorldWithoutNativeResolutionCost() {
        assertArrayEquals(new int[]{768, 432}, ViewportSize.auto(1920, 1080));
        assertArrayEquals(new int[]{768, 432}, ViewportSize.auto(1280, 720));
    }
    @Test public void handlesHandheldRatiosAndWidthGranularity() {
        assertArrayEquals(new int[]{496, 432}, ViewportSize.auto(1240, 1080));
        assertArrayEquals(new int[]{576, 432}, ViewportSize.auto(1600, 1200));
        assertArrayEquals(new int[]{688, 432}, ViewportSize.auto(1920, 1200));
    }
    @Test public void squareAndPortraitKeepMenuUsable() {
        assertArrayEquals(new int[]{480, 432}, ViewportSize.auto(720, 720));
        assertArrayEquals(new int[]{480, 432}, ViewportSize.auto(1080, 1920));
    }
    @Test public void missingLayoutHasSafeFallback() {
        assertArrayEquals(new int[]{480, 432}, ViewportSize.auto(0, 0));
        assertArrayEquals(new int[]{480, 432}, ViewportSize.auto(-1, 1080));
    }
    @Test public void zoomAutoUsesExactTwoTimesUpscaleOnPhone() {
        assertArrayEquals(new int[]{1170, 540}, ViewportSize.autoZoom(2340, 1080));
        assertArrayEquals(new int[]{1170, 540}, ViewportSize.autoZoom(2341, 1081));
    }
    @Test public void zoomAutoKeepsMenusUsableOnSmallScreens() {
        assertArrayEquals(new int[]{960, 800}, ViewportSize.autoZoom(960, 800));
        assertArrayEquals(new int[]{720, 648}, ViewportSize.autoZoom(720, 720));
        assertArrayEquals(new int[]{480, 432}, ViewportSize.autoZoom(0, 0));
    }
    @Test public void zoomAutoCentersLandscapeCanvasOnPortraitDisplay() {
        assertArrayEquals(new int[]{540, 486}, ViewportSize.autoZoom(1080, 2340));
    }
    @Test public void roundingKeepsAspectErrorBelowOnePercentAcrossHandhelds() {
        int[][] screens={{1920,1080},{1920,1200},{1240,1080},{1440,1080},{720,720},{1080,1920},{2560,1080}};
        for (int[] screen : screens) {
            int[] size=ViewportSize.auto(screen[0],screen[1]);
            assertEquals(0,size[0]%8);
            assertTrue(size[0]>=480 && size[1]>=432);
            double expected=Math.max(480.0/432, (double)screen[0]/screen[1]);
            assertTrue(Math.abs((double)size[0]/size[1]/expected-1)<0.01);
        }
    }
}
