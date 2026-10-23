package studio.jiayu.neoagent;
import android.graphics.Rect;import android.view.accessibility.AccessibilityNodeInfo;import java.security.MessageDigest;

/** In-memory signature of actionable UI. Text never leaves this device through this class. */
final class GuiRevision {
 static String signature(AccessibilityNodeInfo root)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");append(root,digest,0,new int[]{0});StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
 private static void append(AccessibilityNodeInfo node,MessageDigest digest,int depth,int[] count)throws Exception{
  if(node==null||depth>35||count[0]++>300||!node.isVisibleToUser())return;
  if(depth==0||node.isClickable()||node.isEditable()||node.isScrollable())digest.update(identity(node).getBytes("UTF-8"));
  for(int i=0;i<node.getChildCount();i++)append(node.getChild(i),digest,depth+1,count);
 }
 static String identity(AccessibilityNodeInfo node){Rect box=new Rect();node.getBoundsInScreen(box);return node.getWindowId()+"|"+node.getPackageName()+"|"+node.getViewIdResourceName()+"|"+node.getClassName()+"|"+box.toShortString()+"|"+node.isEnabled()+"|"+node.isChecked()+"|"+node.isSelected()+"|"+bounded(node.getText())+"|"+bounded(node.getContentDescription());}
 private static String bounded(CharSequence value){String s=value==null?"":value.toString();return s.substring(0,Math.min(240,s.length()));}
}
