package studio.jiayu.neoagent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only Damai detail reporting rule. Never authorizes an action or verifies a goal. */
final class FinishObservationRules {
 static final String PACKAGE="cn.damai",TITLE="cn.damai:id/project_detail_title_bar",VENUE="cn.damai:id/venue_main_ui_0";
 private static final Pattern QUOTED_SEARCH=Pattern.compile("(?:搜索|搜一下|查找|找一下)\\s*(?:“([^”]{2,60})”|「([^」]{2,60})」|《([^》]{2,60})》|\"([^\"]{2,60})\"|『([^』]{2,60})』)");
 private static final Pattern SEARCH=Pattern.compile("搜索|搜一下|查找|找一下|(?<!搜一)搜(?!一搜)");
 private static final Pattern RISK=Pattern.compile("关注|想看|购买|购票|买票|下单|订单|付款|支付|退票|删除|修改|编辑|发送|分享|收藏|抢票|预订|提交|选座|通知|提醒|合同|文档");
 static final class Verdict {
  final boolean canReport,verified=false;final String code;
  private Verdict(boolean canReport,String code){this.canReport=canReport;this.code=code;}
 }
 /** Values must come from the local owner and a fresh native window inspection, never model fields. */
 static final class Scope {
  final String run,pkg;final long epoch,revision;final int window,left,top,right,bottom;final boolean running,current,expected,covered;
  Scope(String run,String pkg,long epoch,long revision,int window,int left,int top,int right,int bottom,boolean running,boolean current,boolean expected,boolean covered){this.run=run;this.pkg=pkg;this.epoch=epoch;this.revision=revision;this.window=window;this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.running=running;this.current=current;this.expected=expected;this.covered=covered;}
 }
 /** Own text/description only. Descendant-derived display labels and model claims are ineligible.
  * Source identity is the frozen observation identity; fresh identity follows node.refresh().
  */
 static final class Anchor {
  final String pkg,resource,type,identity,text,description;final int window,left,top,right,bottom;final boolean visible,enabled,unique,refreshed;
  Anchor(String pkg,String resource,String type,String identity,String text,String description,int window,int left,int top,int right,int bottom,boolean visible,boolean enabled,boolean unique,boolean refreshed){this.pkg=pkg;this.resource=resource;this.type=type;this.identity=identity;this.text=text==null?"":text;this.description=description==null?"":description;this.window=window;this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.visible=visible;this.enabled=enabled;this.unique=unique;this.refreshed=refreshed;}
 }
 static String requestedObject(String pkg,String original,boolean checkout,boolean purchaseAuthorized,boolean documentTask){
  if(!PACKAGE.equals(pkg)||original==null||original.length()>2400||checkout||purchaseAuthorized||documentTask||RISK.matcher(original).find())return "";
  Matcher all=SEARCH.matcher(original);int count=0;while(all.find())count++;if(count!=1)return "";
  Matcher quoted=QUOTED_SEARCH.matcher(original);if(!quoted.find())return "";String wanted="";for(int i=1;i<=5;i++)if(quoted.group(i)!=null)wanted=quoted.group(i).trim();
  String tail=original.substring(quoted.end());if(wanted.length()<2||!tail.matches("(?s).*(?:打开|查看|看看)[^。；;\\n]{0,80}(?:演出|演唱会|音乐会)[^。；;\\n]{0,40}详情.*"))return "";
  return wanted;
 }
 static Verdict evaluate(String pkg,String original,boolean checkout,boolean purchaseAuthorized,boolean documentTask,Scope source,Scope fresh,Anchor sourceTitle,Anchor freshTitle,Anchor sourceVenue,Anchor freshVenue){
  String wanted=requestedObject(pkg,original,checkout,purchaseAuthorized,documentTask);if(wanted.isEmpty())return new Verdict(false,"strict_fallback");
  if(!sameScope(source,fresh))return new Verdict(false,"stale_scope");
  if(!eligible(sourceTitle,TITLE,source)||!eligible(freshTitle,TITLE,fresh)||!eligible(sourceVenue,VENUE,source)||!eligible(freshVenue,VENUE,fresh)||!freshTitle.refreshed||!freshVenue.refreshed)return new Verdict(false,"missing_native_evidence");
  if(!sameAnchor(sourceTitle,freshTitle)||!sameAnchor(sourceVenue,freshVenue))return new Verdict(false,"changed_native_evidence");
  if(!sourceTitle.text.contains(wanted)&&!sourceTitle.description.contains(wanted))return new Verdict(false,"target_not_in_native_title");
  return new Verdict(true,"model_reported_detail");
 }
 private static boolean sameScope(Scope a,Scope b){return a!=null&&b!=null&&a.run!=null&&!a.run.isEmpty()&&a.run.equals(b.run)&&PACKAGE.equals(a.pkg)&&a.pkg.equals(b.pkg)&&a.epoch>=0&&a.epoch==b.epoch&&a.revision>0&&a.revision==b.revision&&a.window>=0&&a.window==b.window&&a.left==b.left&&a.top==b.top&&a.right==b.right&&a.bottom==b.bottom&&a.left<a.right&&a.top<a.bottom&&a.running&&b.running&&a.current&&b.current&&a.expected&&b.expected&&!a.covered&&!b.covered;}
 private static boolean eligible(Anchor a,String resource,Scope scope){return a!=null&&PACKAGE.equals(a.pkg)&&resource.equals(a.resource)&&"android.widget.LinearLayout".equals(a.type)&&a.identity!=null&&!a.identity.isEmpty()&&a.identity.length()<=4096&&a.text.length()<=2000&&a.description.length()<=2000&&(!a.text.trim().isEmpty()||!a.description.trim().isEmpty())&&a.window==scope.window&&a.visible&&a.enabled&&a.unique&&a.left>=scope.left&&a.top>=scope.top&&a.right<=scope.right&&a.bottom<=scope.bottom&&a.left<a.right&&a.top<a.bottom;}
 private static boolean sameAnchor(Anchor a,Anchor b){return a.identity.equals(b.identity)&&a.text.equals(b.text)&&a.description.equals(b.description)&&a.window==b.window&&a.left==b.left&&a.top==b.top&&a.right==b.right&&a.bottom==b.bottom;}
}
