package studio.jiayu.neoagent;

/** Freshness and visual consistency before a coordinate gesture; never goal or permission evidence. */
final class VisualActionRules {
 static final int WIDTH=120,HEIGHT=240;
 static final int EFFECT_MASK_DP=8;
 static final long MAX_FRAME_AGE_MS=1200;
 static final class Frame {
  final int width,height;final int[] pixels;final long capturedAt;
  Frame(int w,int h,int[] values,long at){width=w;height=h;pixels=values.clone();capturedAt=at;}
 }
 static final class Verdict {
  final boolean allowed;final String reason;final int globalChanged,localChanged;
  Verdict(boolean yes,String why,int global,int local){allowed=yes;reason=why;globalChanged=global;localChanged=local;}
 }
 static boolean stableOcclusion(String reason){return "path_masked".equals(reason)||"target_coverage_insufficient".equals(reason)||"global_coverage_insufficient".equals(reason);}
 static boolean specificNativeTarget(int appWidth,int appHeight,int width,int height,boolean semantic){return semantic&&appWidth>0&&appHeight>0&&width>0&&height>0&&(long)width*height*4<=(long)appWidth*appHeight;}
 static boolean fresh(long capturedAt,long now){return capturedAt>0&&now>=capturedAt&&now-capturedAt<=MAX_FRAME_AGE_MS;}
 static Verdict denied(String reason,int global,int local){return new Verdict(false,reason,global,local);}
 static Verdict compare(Frame before,Frame after,int x,int y,int x2,int y2,int radius,long now){
  if(before==null||after==null||before.width!=after.width||before.height!=after.height||before.width<=0||before.height<=0||before.pixels.length!=WIDTH*HEIGHT||after.pixels.length!=WIDTH*HEIGHT)return denied("missing_or_resized_frame",0,0);
  if(!fresh(after.capturedAt,now))return denied("stale_fresh_frame",0,0);
  if(x<0||y<0||x2<0||y2<0||x>=before.width||x2>=before.width||y>=before.height||y2>=before.height)return denied("path_outside_frame",0,0);
  // A masked endpoint or any masked point along a swipe cannot be established by global similarity.
  int steps=Math.max(1,(int)Math.ceil(Math.hypot((x2-x)*(double)WIDTH/before.width,(y2-y)*(double)HEIGHT/before.height)*2));
  for(int n=0;n<=steps;n++){int px=x+(x2-x)*n/steps,py=y+(y2-y)*n/steps;int at=Math.min(HEIGHT-1,py*HEIGHT/before.height)*WIDTH+Math.min(WIDTH-1,px*WIDTH/before.width);if(!visible(before.pixels[at],after.pixels[at]))return denied("path_masked",0,0);}
  int visible=0,changed=0,near=0,nearVisible=0,nearChanged=0;int[] cellVisible=new int[160],cellChanged=new int[160],cellNearVisible=new int[160],cellNearChanged=new int[160];double pad=Math.max(radius,Math.max(before.width/(double)WIDTH,before.height/(double)HEIGHT)*2.5);
  for(int gy=0;gy<HEIGHT;gy++)for(int gx=0;gx<WIDTH;gx++){
   int at=gy*WIDTH+gx,a=before.pixels[at],b=after.pixels[at],cell=(gy*16/HEIGHT)*10+gx*10/WIDTH;double px=(gx+.5)*before.width/WIDTH,py=(gy+.5)*before.height/HEIGHT;boolean local=distance(px,py,x,y,x2,y2)<=pad;if(local)near++;
   if(!visible(a,b))continue;visible++;cellVisible[cell]++;if(local){nearVisible++;cellNearVisible[cell]++;}
   if(different(a,b)){changed++;cellChanged[cell]++;if(local){nearChanged++;cellNearChanged[cell]++;}}
  }
  if(visible<WIDTH*HEIGHT/5)return denied("global_coverage_insufficient",changed,nearChanged);
  if(near<8||nearVisible<8||nearVisible*5<near*4)return denied("target_coverage_insufficient",changed,nearChanged);
  // Permit small color/compression changes and small unrelated spinners; replace/drop/dim transitions fail.
  if(nearChanged>=4&&nearChanged*100>=nearVisible*8)return denied("target_changed",changed,nearChanged);
  if(nearChanged>=8&&(x!=x2||y!=y2))for(int i=0;i<cellNearVisible.length;i++)if(cellNearVisible[i]>=12&&cellNearChanged[i]*100>=cellNearVisible[i]*12)return denied("target_changed",changed,nearChanged);
  if(changed*100>=visible*4)return denied("global_changed",changed,nearChanged);
  if(changed>=Math.max(24,visible/350))for(int i=0;i<cellVisible.length;i++)if(cellVisible[i]>=20&&cellChanged[i]*100>=cellVisible[i]*12)return denied("coherent_region_changed",changed,nearChanged);
  return new Verdict(true,"similar_not_goal_evidence",changed,nearChanged);
 }
 private static boolean visible(int a,int b){return (a>>>24)!=0&&(b>>>24)!=0;}
 private static boolean different(int a,int b){int max=0,sum=0;for(int shift=0;shift<=16;shift+=8){int delta=Math.abs(((a>>>shift)&255)-((b>>>shift)&255));max=Math.max(max,delta);sum+=delta;}return max>44&&sum>66;}
 private static double distance(double x,double y,double ax,double ay,double bx,double by){double dx=bx-ax,dy=by-ay,length=dx*dx+dy*dy;if(length==0)return Math.hypot(x-ax,y-ay);double t=Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/length));return Math.hypot(x-(ax+t*dx),y-(ay+t*dy));}
}
