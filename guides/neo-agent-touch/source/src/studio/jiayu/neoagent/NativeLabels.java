package studio.jiayu.neoagent;
import android.view.accessibility.AccessibilityNodeInfo;
/** Read native descendant labels for clickable containers; never infer semantics from geometry. */
final class NativeLabels {
 static boolean resourceAction(AccessibilityNodeInfo n){String id=n.getViewIdResourceName();return id!=null&&id.matches("(?i).*(?:[-_/])(btn|button)$");}
 static String label(AccessibilityNodeInfo node){if(node==null||node.isPassword())return "";String text=node.getText()==null?"":node.getText().toString().trim(),desc=node.getContentDescription()==null?"":node.getContentDescription().toString().trim();if(!text.isEmpty())return text;if(!desc.isEmpty())return desc;StringBuilder b=new StringBuilder();children(node,b,0,new int[]{0});return b.toString().trim();}
 static void children(AccessibilityNodeInfo n,StringBuilder out,int depth,int[] count){if(n==null||n.isPassword()||depth>4||count[0]++>24||out.length()>180)return;if(depth>0&&(n.isClickable()||n.isEditable()||n.isLongClickable()))return;if(depth>0){CharSequence text=n.getText(),desc=n.getContentDescription();if(text!=null&&text.length()>0)out.append(text).append(' ');else if(desc!=null&&desc.length()>0)out.append(desc).append(' ');}for(int i=0;i<n.getChildCount();i++)children(n.getChild(i),out,depth+1,count);}
}
