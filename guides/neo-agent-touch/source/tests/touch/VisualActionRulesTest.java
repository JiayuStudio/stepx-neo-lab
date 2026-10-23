package studio.jiayu.neoagent;
import java.util.*;
public class VisualActionRulesTest {
 static int checks;static final int W=VisualActionRules.WIDTH,H=VisualActionRules.HEIGHT;static final long NOW=10000;
 static int[] base(){int[] a=new int[W*H];Arrays.fill(a,0xff485666);return a;}
 static void rect(int[] a,int x,int y,int w,int h,int color){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)a[yy*W+xx]=color;}
 static VisualActionRules.Frame frame(int[] a){return new VisualActionRules.Frame(W,H,a,NOW-50);}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 static VisualActionRules.Verdict compare(int[] a,int[] b){return VisualActionRules.compare(frame(a),frame(b),60,120,60,120,8,NOW);}
 public static void main(String[] args){
  int[] original=base();check(compare(original,original).allowed,"unchanged target rejected");
  int[] noisy=original.clone();for(int i=0;i<noisy.length;i++)noisy[i]+=0x000a0a0a;check(compare(original,noisy).allowed,"small global rendering/color noise rejected");
  int[] dots=original.clone();for(int i=0;i<5;i++)dots[i*21+5]=0xffeeeeee;check(compare(original,dots).allowed,"small unrelated animation rejected");
  int[] spinner=original.clone();rect(spinner,80,60,8,8,0xffeeeeee);check(compare(original,spinner).allowed,"small outside-target spinner rejected");
  int[] target=original.clone();rect(target,56,116,8,8,0xffeeeeee);VisualActionRules.Verdict v=compare(original,target);check(!v.allowed&&v.reason.equals("target_changed"),"changed target allowed");
  int[] popup=original.clone();rect(popup,10,30,80,70,0xffeeddcc);v=compare(original,popup);check(!v.allowed&&v.reason.equals("global_changed"),"new modal away from target allowed");
  int[] board=original.clone();rect(board,80,60,10,10,0xffeeeeee);v=compare(original,board);check(!v.allowed&&v.reason.equals("coherent_region_changed"),"coherent board tile change allowed");
  int[] dimmed=original.clone();Arrays.fill(dimmed,0xff101018);check(!compare(original,dimmed).allowed,"global overlay/dimming allowed");
  int[] oldMask=original.clone(),newMask=original.clone();rect(oldMask,5,40,10,20,0);rect(newMask,90,40,10,20,0);check(compare(oldMask,newMask).allowed,"old/new capsule mask union changed the scene");
  int[] targetMasked=original.clone();rect(targetMasked,58,118,4,4,0);v=compare(original,targetMasked);check(!v.allowed&&v.reason.equals("path_masked"),"new capsule hid target but globally similar passed");
  v=compare(targetMasked,original);check(!v.allowed&&v.reason.equals("path_masked"),"old capsule hid model's target but passed");
  int[] poorCoverage=original.clone();rect(poorCoverage,52,113,7,15,0);v=compare(original,poorCoverage);check(!v.allowed&&v.reason.equals("target_coverage_insufficient"),"partly occluded target neighborhood allowed");
  int[] hidden=original.clone();Arrays.fill(hidden,0);check(!compare(original,hidden).allowed,"no visible source accepted");
  int[] swipe=original.clone();rect(swipe,59,117,3,6,0);v=VisualActionRules.compare(frame(original),frame(swipe),40,100,80,140,8,NOW);check(!v.allowed&&v.reason.equals("path_masked"),"swipe crossed a masked midpoint");
  int[] swipeChanged=original.clone();rect(swipeChanged,56,116,8,8,0xffeeeeee);v=VisualActionRules.compare(frame(original),frame(swipeChanged),40,100,80,140,8,NOW);check(!v.allowed&&v.reason.equals("target_changed"),"swipe corridor changed");
  check(VisualActionRules.fresh(NOW-1200,NOW),"deadline boundary rejected");check(!VisualActionRules.fresh(NOW-1201,NOW),"old check frame accepted");check(!VisualActionRules.fresh(NOW+1,NOW),"future timestamp accepted");
  check(!VisualActionRules.compare(frame(original),new VisualActionRules.Frame(W,H,original,NOW-1201),60,120,60,120,8,NOW).allowed,"expired frame passed similarity");
  check(!VisualActionRules.compare(null,frame(original),60,120,60,120,8,NOW).allowed,"missing model frame accepted");
  check(!VisualActionRules.compare(frame(original),new VisualActionRules.Frame(W+1,H,original,NOW-50),60,120,60,120,8,NOW).allowed,"resized screen accepted");
  check(!VisualActionRules.compare(frame(original),frame(original),-1,120,60,120,8,NOW).allowed,"out-of-frame coordinate accepted");
  int[] caller=original.clone();VisualActionRules.Frame copy=frame(caller);caller[0]=0;check(copy.pixels[0]!=0,"comparison evidence aliases mutable caller buffer");
  check(VisualActionRules.specificNativeTarget(1080,2400,160,60,true),"specific native button denied fast path");
  check(!VisualActionRules.specificNativeTarget(1080,2400,1080,2400,true),"whole Canvas container allowed fast path");
  check(!VisualActionRules.specificNativeTarget(1080,2400,160,60,false),"unlabelled coordinate container allowed fast path");
  check(!VisualActionRules.specificNativeTarget(0,2400,160,60,true),"unknown native bounds allowed fast path");
  check(VisualActionRules.stableOcclusion("path_masked"),"stable path mask lacks rejected-action feedback");
  check(VisualActionRules.stableOcclusion("target_coverage_insufficient"),"stable target coverage lacks rejected-action feedback");
  check(VisualActionRules.stableOcclusion("global_coverage_insufficient"),"stable whole-frame coverage lacks rejected-action feedback");
  check(!VisualActionRules.stableOcclusion("target_changed")&&!VisualActionRules.stableOcclusion("stale_fresh_frame"),"real movement/expiry incorrectly treated as stable rejection");
  System.out.println("PASS: VisualActionRules "+checks+" unchanged/noise/animation, target/modal/board changes, mask union, swipe corridor and frame age checks");
 }
}
