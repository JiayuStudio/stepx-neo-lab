package studio.jiayu.neoagent;
import android.content.Context;import android.view.*;import android.widget.LinearLayout;
/** Drag anywhere, including over buttons. Crossing touch slop cancels the child's click. */
final class ControlCapsule extends LinearLayout {
 interface Drag {void begin();void move(float dx,float dy);void end();}
 private final Drag drag;private final int slop;private float downX,downY;boolean dragging;
 ControlCapsule(Context context,Drag drag){super(context);this.drag=drag;slop=ViewConfiguration.get(context).getScaledTouchSlop();setClickable(true);setContentDescription("控制胶囊，可拖动换位置");}
 private void down(MotionEvent e){downX=e.getRawX();downY=e.getRawY();dragging=false;drag.begin();}
 private void move(MotionEvent e){float dx=e.getRawX()-downX,dy=e.getRawY()-downY;if(!dragging&&Math.hypot(dx,dy)>slop)dragging=true;if(dragging)drag.move(dx,dy);}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN)down(e);else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){move(e);if(dragging)return true;}return false;}
 @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:return true;case MotionEvent.ACTION_MOVE:move(e);return true;case MotionEvent.ACTION_UP:if(dragging){drag.move(e.getRawX()-downX,e.getRawY()-downY);drag.end();}else performClick();dragging=false;return true;case MotionEvent.ACTION_CANCEL:if(dragging)drag.end();dragging=false;return true;}return true;}
 @Override public boolean performClick(){return super.performClick();}
}
