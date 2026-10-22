package studio.jiayu.neoagent;
import android.content.Context;import android.graphics.*;import android.view.*;import android.os.Build;import android.os.SystemClock;import android.animation.ValueAnimator;
/** Continuous blue perimeter with a slow, visible breathing halo; centre remains transparent. */
final class ControlEffects extends View {
 Bitmap halo;SweepGradient flow;ComposeShader haloFlow;final Matrix flowMatrix=new Matrix();final long started=SystemClock.uptimeMillis();
 final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path border=new Path();final float[] radii=new float[8];
 ControlEffects(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setFitsSystemWindows(false);setLayerType(LAYER_TYPE_HARDWARE,null);}
 // Application bounds belong to action/screenshot validation, never the display-edge effect.
 void appBounds(Rect ignored){}
 void target(float x,float y,String label){} // Edge-only presentation; no centre ripple.
 @Override public WindowInsets onApplyWindowInsets(WindowInsets insets){shape(insets);invalidate();return insets;}
 @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){shape(getRootWindowInsets());}
 void shape(WindowInsets insets){float fallback=0;int resource=getResources().getIdentifier("rounded_corner_radius","dimen","android");if(resource!=0)fallback=getResources().getDimension(resource);if(fallback==0)fallback=getResources().getDisplayMetrics().density*28;
  int[] positions={RoundedCorner.POSITION_TOP_LEFT,RoundedCorner.POSITION_TOP_RIGHT,RoundedCorner.POSITION_BOTTOM_RIGHT,RoundedCorner.POSITION_BOTTOM_LEFT};float limit=Math.min(getWidth(),getHeight())/2f;
  for(int i=0;i<4;i++){RoundedCorner corner=Build.VERSION.SDK_INT>=31&&insets!=null?insets.getRoundedCorner(positions[i]):null;float radius=Math.max(0,Math.min(limit,corner==null?fallback:corner.getRadius())-.5f);radii[i*2]=radii[i*2+1]=radius;}
  if(halo!=null){halo.recycle();halo=null;}flow=null;haloFlow=null;border.reset();if(getWidth()>1&&getHeight()>1)border.addRoundRect(new RectF(.5f,.5f,getWidth()-.5f,getHeight()-.5f),radii,Path.Direction.CW);
 }
 void prepareHalo(){if(halo!=null||getWidth()<2||getHeight()<2)return;halo=Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(halo);float d=getResources().getDisplayMetrics().density;paint.setAlpha(255);paint.setStyle(Paint.Style.STROKE);paint.setColor(0xA0478EFF);paint.setStrokeWidth(d*7);paint.setShadowLayer(d*32,0,0,0xFF367EFF);c.drawPath(border,paint);paint.clearShadowLayer();paint.setColor(0x485998FF);paint.setStrokeWidth(d*24);c.drawPath(border,paint);paint.setColor(0x4875B5FF);paint.setStrokeWidth(d*7);c.drawPath(border,paint);flow=new SweepGradient(getWidth()/2f,getHeight()/2f,new int[]{0xFF2678FF,0xFF91D6FF,0xFF387AFF,0xFF63B8FF,0xFF2678FF},new float[]{0,.22f,.5f,.78f,1});haloFlow=new ComposeShader(flow,new BitmapShader(halo,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP),PorterDuff.Mode.DST_IN);}
 void drawFrame(Canvas c,float strength){drawFrame(c,strength,strength);}
 void drawFrame(Canvas c,float strength,float phase){if(border.isEmpty())shape(getRootWindowInsets());prepareHalo();float d=getResources().getDisplayMetrics().density;if(haloFlow!=null){flowMatrix.setRotate(phase*360,getWidth()/2f,getHeight()/2f);flow.setLocalMatrix(flowMatrix);paint.setStyle(Paint.Style.FILL);paint.setShader(haloFlow);paint.setAlpha((int)(175+80*Math.max(0,Math.min(1,strength))));int saved=c.save();float rim=d*42;if(getWidth()>rim*2&&getHeight()>rim*2)c.clipOutRect(rim,rim,getWidth()-rim,getHeight()-rim);c.drawRect(0,0,getWidth(),getHeight(),paint);c.restoreToCount(saved);paint.setShader(null);paint.setAlpha(255);}paint.setStyle(Paint.Style.STROKE);paint.setColor(0xE04F9FFF);paint.setStrokeWidth(Math.max(1,d*2));c.drawPath(border,paint);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);boolean motion=ValueAnimator.areAnimatorsEnabled();float wave=motion?(float)(.5-.5*Math.cos((SystemClock.uptimeMillis()-started)*Math.PI*2/3600)):.6f;drawFrame(c,wave,motion?((SystemClock.uptimeMillis()-started)%8000)/8000f:0);if(motion&&isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postInvalidateOnAnimation();}
 @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();flow=null;haloFlow=null;if(halo!=null){halo.recycle();halo=null;}}
}
