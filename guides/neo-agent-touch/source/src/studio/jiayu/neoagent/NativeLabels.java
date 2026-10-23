package studio.jiayu.neoagent;
import android.graphics.Rect;import android.os.Build;import android.view.accessibility.AccessibilityNodeInfo;import java.util.List;
/** Read native descendant labels for clickable containers; never infer semantics from geometry. */
final class NativeLabels {
 static boolean resourceAction(AccessibilityNodeInfo n){String id=n.getViewIdResourceName();return id!=null&&id.matches("(?i).*(?:[-_/])(btn|button)$");}
 static String label(AccessibilityNodeInfo node){if(node==null||node.isPassword())return "";String text=node.getText()==null?"":node.getText().toString().trim(),desc=node.getContentDescription()==null?"":node.getContentDescription().toString().trim();if(!text.isEmpty())return text;if(!desc.isEmpty())return desc;StringBuilder b=new StringBuilder();children(node,b,0,new int[]{0});return b.toString().trim();}
 static void children(AccessibilityNodeInfo n,StringBuilder out,int depth,int[] count){if(n==null||n.isPassword()||depth>4||count[0]++>24||out.length()>180)return;if(depth>0&&(n.isClickable()||n.isEditable()||n.isLongClickable()))return;if(depth>0){CharSequence text=n.getText(),desc=n.getContentDescription();if(text!=null&&text.length()>0)out.append(text).append(' ');else if(desc!=null&&desc.length()>0)out.append(desc).append(' ');}for(int i=0;i<n.getChildCount();i++)children(n.getChild(i),out,depth+1,count);}
 /** Independent complete evidence for an unlabelled, small clickable container. Never uses label(). */
 static ContainerLabelProof.Proof containerProof(AccessibilityNodeInfo node,Rect app){
  try{
   if(node==null||app==null||!node.refresh()||!node.isVisibleToUser()||!node.isEnabled()||!node.isClickable()||node.isEditable()||node.isPassword()||resourceAction(node)||hasOwnLabel(node))return null;
   Rect bounds=new Rect();node.getBoundsInScreen(bounds);if(!app.contains(bounds)||!VisualActionRules.specificNativeTarget(app.width(),app.height(),bounds.width(),bounds.height(),true))return null;
   CharSequence pkg=node.getPackageName();if(pkg==null||pkg.length()==0)return null;
   ContainerLabelProof.Builder builder=new ContainerLabelProof.Builder();if(!capture(node,0,node.getWindowId(),pkg.toString(),builder))return null;
   return builder.finish();
  }catch(Exception incomplete){return null;}
 }
 private static boolean hasOwnLabel(AccessibilityNodeInfo node){return node.getText()!=null&&!node.getText().toString().trim().isEmpty()||node.getContentDescription()!=null&&!node.getContentDescription().toString().trim().isEmpty();}
 private static String full(CharSequence value){if(value==null)return "";if(value.length()>ContainerLabelProof.MAX_CHARACTERS)throw new IllegalArgumentException("native label exceeds proof budget");return value.toString();}
 private static int flags(AccessibilityNodeInfo node){int flags=0;if(node.isVisibleToUser())flags|=ContainerLabelProof.VISIBLE;if(node.isEnabled())flags|=ContainerLabelProof.ENABLED;if(node.isClickable())flags|=ContainerLabelProof.CLICKABLE;if(node.isEditable())flags|=ContainerLabelProof.EDITABLE;if(node.isLongClickable())flags|=ContainerLabelProof.LONG_CLICKABLE;if(node.isScrollable())flags|=ContainerLabelProof.SCROLLABLE;if(node.isPassword())flags|=ContainerLabelProof.PASSWORD;if(node.isCheckable())flags|=ContainerLabelProof.CHECKABLE;if(node.isChecked())flags|=ContainerLabelProof.CHECKED;if(node.isSelected())flags|=ContainerLabelProof.SELECTED;if(node.isFocused())flags|=ContainerLabelProof.FOCUSED;if(node.isFocusable())flags|=ContainerLabelProof.FOCUSABLE;if(node.isAccessibilityFocused())flags|=ContainerLabelProof.ACCESSIBILITY_FOCUSED;if(node.isHeading())flags|=ContainerLabelProof.HEADING;if(node.isContentInvalid())flags|=ContainerLabelProof.CONTENT_INVALID;return flags;}
 private static String actions(AccessibilityNodeInfo node){List<AccessibilityNodeInfo.AccessibilityAction> actions=node.getActionList();if(actions==null||actions.size()>ContainerLabelProof.MAX_NODES)throw new IllegalArgumentException("native action list incomplete");StringBuilder out=new StringBuilder();for(AccessibilityNodeInfo.AccessibilityAction action:actions){if(action==null)throw new IllegalArgumentException("native action missing");String label=full(action.getLabel());if(out.length()+label.length()+32>ContainerLabelProof.MAX_CHARACTERS)throw new IllegalArgumentException("native action list exceeds proof budget");out.append(action.getId()).append(':').append(label.length()).append(':').append(label).append(';');}return out.toString();}
 private static boolean capture(AccessibilityNodeInfo node,int depth,int window,String pkg,ContainerLabelProof.Builder builder){
  if(node==null||depth>ContainerLabelProof.MAX_DEPTH||!builder.valid()||!node.refresh()||node.getWindowId()!=window||!pkg.contentEquals(node.getPackageName()==null?"":node.getPackageName())){builder.incomplete();return false;}
  int children=node.getChildCount();Rect bounds=new Rect();node.getBoundsInScreen(bounds);
  ContainerLabelProof.Node sample=new ContainerLabelProof.Node(depth,children,window,pkg,full(node.getClassName()),node.getViewIdResourceName(),Build.VERSION.SDK_INT>=33?node.getUniqueId():"",bounds.left,bounds.top,bounds.right,bounds.bottom,flags(node),node.getInputType(),node.getMovementGranularities(),node.getDrawingOrder(),full(node.getText()),full(node.getContentDescription()),full(node.getHintText()),full(node.getStateDescription()),actions(node));
  if(!builder.begin(sample))return false;
  for(int i=0;i<children;i++)if(!capture(node.getChild(i),depth+1,window,pkg,builder))return false;
  if(node.getChildCount()!=children){builder.incomplete();return false;}
  return builder.end();
 }
}
