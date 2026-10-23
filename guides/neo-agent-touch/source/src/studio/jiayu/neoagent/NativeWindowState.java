package studio.jiayu.neoagent;
import android.graphics.Rect;import android.view.accessibility.*;
/** Fresh local window inspection; exports no window names, app inventory, or private text. */
final class NativeWindowState {
 final AccessibilityNodeInfo root;final boolean expected,covered;final Rect bounds;
 private NativeWindowState(AccessibilityNodeInfo root,boolean expected,boolean covered,Rect bounds){this.root=root;this.expected=expected;this.covered=covered;this.bounds=bounds;}
 static NativeWindowState inspect(ControlService s){
  AccessibilityNodeInfo root=s.getRootInActiveWindow();
  boolean expected=root!=null&&s.allowed.contentEquals(root.getPackageName());
  boolean covered=false;Rect app=new Rect();
  if(expected){
   Rect rootBox=new Rect(),surface=null;root.getBoundsInScreen(rootBox);int layer=-1;
   java.util.List<AccessibilityWindowInfo> windows=s.getWindows();
   for(AccessibilityWindowInfo w:windows)if(w.getId()==root.getWindowId()){
    layer=w.getLayer();surface=new Rect();w.getBoundsInScreen(surface);break;
   }
   android.util.DisplayMetrics display=s.screenMetrics();
   int[] frame=WindowFrame.resolve(box(rootBox),surface==null?null:box(surface),display.widthPixels,display.heightPixels);
   if(frame==null)expected=false;
   else{
    app.set(frame[0],frame[1],frame[2],frame[3]);
    for(AccessibilityWindowInfo w:windows){
     if(w.getLayer()<=layer||w.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD||s.controlBar.owns(w)||(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY&&!s.effectsWindowTitle.isEmpty()&&s.effectsWindowTitle.contentEquals(w.getTitle()==null?"":w.getTitle())))continue;
     AccessibilityNodeInfo other=w.getRoot();if(other!=null&&s.allowed.contentEquals(other.getPackageName()))continue;
     if(w.getType()==AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY&&other!=null&&s.getPackageName().contentEquals(other.getPackageName()))continue;
     Rect obstruction=new Rect();w.getBoundsInScreen(obstruction);
     if(obstruction.intersect(app)&&(long)obstruction.width()*obstruction.height()>(long)app.width()*app.height()*.12){covered=true;break;}
    }
   }
  }
  return new NativeWindowState(root,expected,covered,app);
 }
 private static int[] box(Rect r){return new int[]{r.left,r.top,r.right,r.bottom};}
}
