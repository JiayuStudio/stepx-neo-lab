package studio.jiayu.neoagent;

import android.graphics.Rect;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.Map;

/** Local frozen native anchors for one read-only Damai finish. No action or completion proof. */
final class FinishObservationGate {
 static final class Frozen {
  final FinishObservationRules.Scope scope;final FinishObservationRules.Anchor title,venue;
  Frozen(FinishObservationRules.Scope scope,FinishObservationRules.Anchor title,FinishObservationRules.Anchor venue){this.scope=scope;this.title=title;this.venue=venue;}
 }
 private static String original(ControlService owner){return owner.activeRequest==null?owner.task:owner.activeRequest.original;}
 private static boolean eligible(ControlService owner){return !FinishObservationRules.requestedObject(owner.allowed,original(owner),owner.checkout,owner.purchase.authorized(),DocumentScope.applies(owner.allowed)).isEmpty();}
 /** Called on the observation UI job, before screenshot/model processing or any node refresh. */
 static Frozen capture(ControlService owner,ControlService.Observation source,NativeWindowState window){
  if(Looper.myLooper()!=Looper.getMainLooper()||!eligible(owner)||source==null||source.id==null||window==null||window.root==null)return null;
  try{
   FinishObservationRules.Scope scope=scope(owner,source,window);
   FinishObservationRules.Anchor title=freeze(owner,source,window.root,FinishObservationRules.TITLE),venue=freeze(owner,source,window.root,FinishObservationRules.VENUE);
   return title==null||venue==null?null:new Frozen(scope,title,venue);
  }catch(Exception incomplete){return null;}
 }
 /** Only the ordinary finish branch may use this after its original strict check failed. */
 static boolean allowsReportedFinish(ControlService owner,ControlService.Observation source,long generation){
  if(Looper.myLooper()!=Looper.getMainLooper()||source==null||source.finishAnchors==null||!eligible(owner))return false;
  try{
   owner.check(generation);owner.validateObservation(source,generation,false);
   NativeWindowState window=NativeWindowState.inspect(owner);if(window.root==null)return false;
   FinishObservationRules.Anchor title=fresh(window.root,FinishObservationRules.TITLE,source),venue=fresh(window.root,FinishObservationRules.VENUE,source);
   owner.check(generation);owner.validateObservation(source,generation,false);
   Frozen frozen=source.finishAnchors;
   return FinishObservationRules.evaluate(owner.allowed,original(owner),owner.checkout,owner.purchase.authorized(),DocumentScope.applies(owner.allowed),frozen.scope,scope(owner,source,window),frozen.title,title,frozen.venue,venue).canReport;
  }catch(Exception staleOrIncomplete){return false;}
 }
 private static FinishObservationRules.Scope scope(ControlService owner,ControlService.Observation source,NativeWindowState window){
  Rect bounds=window.bounds;AccessibilityNodeInfo root=window.root;
  return new FinishObservationRules.Scope(owner.runId,root==null?"":String.valueOf(root.getPackageName()),owner.generation,source.id.revision,root==null?-1:root.getWindowId(),bounds.left,bounds.top,bounds.right,bounds.bottom,owner.running,owner.ownership.current(source.id),window.expected,window.covered);
 }
 private static FinishObservationRules.Anchor freeze(ControlService owner,ControlService.Observation source,AccessibilityNodeInfo root,String resource){
  AccessibilityNodeInfo node=null;String identity=null;int matches=0;
  for(Map.Entry<Integer,AccessibilityNodeInfo> item:source.targets.entrySet())if(resource.equals(item.getValue().getViewIdResourceName())){matches++;node=item.getValue();identity=source.identities.get(item.getKey());}
  if(matches!=1||node==null||identity==null||!identity.equals(GuiRevision.identity(node)))return null;
  // Read the source copy before obtaining any new native reference. Never refresh it.
  FinishObservationRules.Anchor frozen=copy(node,identity,false);
  AccessibilityNodeInfo unique=unique(root,resource);if(unique==null||!same(frozen,copy(unique,GuiRevision.identity(unique),false)))return null;
  return frozen;
 }
 private static FinishObservationRules.Anchor fresh(AccessibilityNodeInfo root,String resource,ControlService.Observation source){AccessibilityNodeInfo node=unique(root,resource),bound=null;int matches=0;for(AccessibilityNodeInfo candidate:source.targets.values())if(resource.equals(candidate.getViewIdResourceName())){matches++;bound=candidate;}if(matches!=1||node==null||bound==null||!node.equals(bound)||!node.refresh())return null;return copy(node,GuiRevision.identity(node),true);}
 private static AccessibilityNodeInfo unique(AccessibilityNodeInfo root,String resource){List<AccessibilityNodeInfo> matches=root.findAccessibilityNodeInfosByViewId(resource);return matches!=null&&matches.size()==1?matches.get(0):null;}
 private static FinishObservationRules.Anchor copy(AccessibilityNodeInfo node,String identity,boolean refreshed){Rect bounds=new Rect();node.getBoundsInScreen(bounds);return new FinishObservationRules.Anchor(String.valueOf(node.getPackageName()),node.getViewIdResourceName(),String.valueOf(node.getClassName()),identity,text(node.getText()),text(node.getContentDescription()),node.getWindowId(),bounds.left,bounds.top,bounds.right,bounds.bottom,node.isVisibleToUser(),node.isEnabled(),true,refreshed);}
 private static String text(CharSequence value){if(value==null)return "";if(value.length()>2000)throw new IllegalArgumentException("native anchor exceeds bound");return value.toString();}
 private static boolean same(FinishObservationRules.Anchor a,FinishObservationRules.Anchor b){return a!=null&&b!=null&&a.pkg.equals(b.pkg)&&a.resource.equals(b.resource)&&a.type.equals(b.type)&&a.identity.equals(b.identity)&&a.text.equals(b.text)&&a.description.equals(b.description)&&a.window==b.window&&a.left==b.left&&a.top==b.top&&a.right==b.right&&a.bottom==b.bottom&&a.visible==b.visible&&a.enabled==b.enabled;}
}
