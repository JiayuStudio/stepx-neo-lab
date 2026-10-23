package studio.jiayu.neoagent;
import java.util.*;
public class PlanCoverageTest {
 static int count;
 static final List<String> DYNAMIC=Arrays.asList("微信","企业微信","高德地图","QQ音乐","百度网盘","Notion");
 static void yes(boolean value,String message){if(!value)throw new AssertionError(message);count++;}
 static void check(String text,String... expected){Set<String> actual=PlanCoverage.apps(text),wanted=new LinkedHashSet<>(Arrays.asList(expected));yes(actual.equals(wanted),text+" => "+actual);}
 static void sequence(String text,String... expected){List<String> actual=new ArrayList<>();for(PlanCoverage.Mention m:PlanCoverage.mentions(text,DYNAMIC))actual.add(m.app);yes(actual.equals(Arrays.asList(expected)),text+" => "+actual);}
 static PlanCoverage.Stage stage(String app,String goal){return new PlanCoverage.Stage(app,goal);}
 static void plan(boolean accepted,String text,PlanCoverage.Stage... stages){List<PlanCoverage.Mention> mentions=PlanCoverage.mentions(text,DYNAMIC);List<String> identities=new ArrayList<>();for(PlanCoverage.Mention m:mentions)identities.add(m.app);String issue=PlanCoverage.issue(text,mentions,identities,Arrays.asList(stages));yes(issue.isEmpty()==accepted,"accept="+accepted+" "+text+" => "+issue);}
 public static void main(String[] args){
  check("在大麦看许嵩演唱会，然后去淘宝看StepX Neo，最后打开哔哩哔哩播放柯南","大麦","淘宝","哔哩哔哩");
  check("在WPS新建合同，然后打开美团看电影","WPS","美团");
  check("在淘宝或者京东看看价格");
  check("在淘宝搜索WPS教程，然后打开B站看视频","淘宝","哔哩哔哩");
  check("先在WPS生成文档，再在WPS修改并保存","WPS");
  check("在淘宝看价格，然后不要打开京东","淘宝");
  check("先在美团外卖看咖啡，最后打开饿了么比价","美团外卖","饿了么");
  check("随便聊聊这些应用");
  sequence("先打开企业微信看工作台，然后打开微信搜一搜，最后在高德地图查路线","企业微信","微信","高德地图");
  String queue="先在QQ音乐听歌，然后打开百度网盘搜索测试，最后打开Notion看笔记";
  sequence(queue,"QQ音乐","百度网盘","Notion");
  List<PlanCoverage.Mention> mentions=PlanCoverage.mentions(queue,DYNAMIC);String scope=TaskScope.forNames(Collections.singleton("百度网盘"),queue,mentions);
  yes(scope.contains("搜索测试")&&!scope.contains("听歌")&&!scope.contains("笔记"),"dynamic app clause leakage");
  sequence("先在QQ音乐听歌，然后不要打开微信","QQ音乐");
  sequence("先在QQ音乐搜索《打开微信教程》，然后打开百度网盘","QQ音乐","百度网盘");
  sequence("先打开「微信」搜一搜，然后打开企业微信","微信","企业微信");
  // The former connector gate dropped every app here. ASR need not produce punctuation or sequence words.
  sequence("打开微信搜一搜测试打开高德地图查路线打开QQ音乐播放许嵩","微信","高德地图","QQ音乐");
  sequence("微信搜索测试高德地图查路线QQ音乐播放许嵩","微信","高德地图","QQ音乐");
  sequence("请帮我微信搜索测试高德地图查路线QQ音乐播放许嵩","微信","高德地图","QQ音乐");
  sequence("在淘宝看手机价格在京东看看同款价格打开B站播放视频","淘宝","京东","哔哩哔哩");
  sequence("打开微信。打开高德地图。打开QQ音乐。","微信","高德地图","QQ音乐");
  sequence("使用企业微信看工作台，回到微信搜一搜","企业微信","微信");
  sequence("在淘宝只查价不加购然后打开京东看看价格","淘宝","京东");
  sequence("在淘宝查价不要再打开京东","淘宝");
  sequence("在淘宝查价不使用京东","淘宝");
  sequence("在淘宝搜索微信搜索教程，然后打开高德地图查路线","淘宝","高德地图");
  sequence("在淘宝搜索打开微信教程，然后打开高德地图查路线","淘宝","高德地图");
  sequence("打开淘宝或者打开京东看价格");
  sequence("先在WPS Office写合同再打开B站看视频","WPS","哔哩哔哩");
  String decoded="先召开企业微信查看工作台然后召开开高德地图查看上海人民广场";
  sequence(decoded,"企业微信","高德地图");
  plan(true,decoded,stage("企业微信","查看工作台"),stage("高德地图","查看上海人民广场"));
  plan(false,decoded,stage("高德地图","查看上海人民广场"));
  plan(false,decoded,stage("高德地图","查看上海人民广场"),stage("企业微信","查看工作台"));
  String decodedScope=TaskScope.forNames(Collections.singleton("高德地图"),decoded,PlanCoverage.mentions(decoded,DYNAMIC));
  yes(decodedScope.contains("上海人民广场")&&!decodedScope.contains("工作台"),"decoded app clauses leaked across stages");
  sequence("召开开QQ音乐搜索许嵩然后召开高德地图搜索人民广场","QQ音乐","高德地图");
  sequence("不要召开企业微信查看工作台");
  sequence("召开企业微信会议查看工作台");
  sequence("在WPS写召开企业微信查看工作台这句话","WPS");
  sequence("在百度网盘搜索《召开企业微信查看工作台》","百度网盘");
  sequence("召开企业微信或者高德地图查看资料");
  sequence("在淘宝搜索SWPS手机，再打开京东看价格","淘宝","京东");
  // Set membership passes the reversed or incomplete plans; source-order alignment must reject them.
  String asr="微信搜索测试高德地图查路线QQ音乐播放许嵩";
  plan(true,asr,stage("微信","公共搜索测试"),stage("高德地图","查公共路线"),stage("QQ音乐","播放许嵩"));
  plan(false,asr,stage("QQ音乐","播放许嵩"),stage("高德地图","查路线"),stage("微信","搜索测试"));
  plan(false,asr,stage("微信","搜索测试"),stage("QQ音乐","播放许嵩"));
  plan(false,asr,stage("高德地图","查路线"),stage("微信","搜索测试"),stage("QQ音乐","播放许嵩"));
  String revisit="先打开微信搜一搜测试然后打开高德地图查路线最后回到微信搜索地图教程";
  sequence(revisit,"微信","高德地图","微信");
  plan(true,revisit,stage("微信","搜一搜测试"),stage("高德地图","查路线"),stage("微信","搜索地图教程"));
  plan(false,revisit,stage("微信","搜一搜测试并搜索地图教程"),stage("高德地图","查路线"));
  plan(true,"先打开微信搜索测试，再打开微信查看结果",stage("微信","搜索测试并查看结果"));
  // Same-package tasks must not be dropped or reversed merely because their app is already covered.
  String searches="先在微信搜索《北京天气》，然后在微信搜索《上海天气》";
  plan(true,searches,stage("微信","搜索北京天气，再搜索上海天气"));
  plan(true,searches,stage("微信","搜索北京天气"),stage("微信","搜索上海天气"));
  plan(false,searches,stage("微信","搜索上海天气，再搜索北京天气"));
  plan(false,searches,stage("微信","搜索北京天气"));
  plan(false,searches,stage("微信","搜索上海天气"),stage("微信","搜索北京天气"));
  String document="先在WPS新建《租赁合同甲》，然后在WPS修改《租赁合同甲》并保存";
  plan(true,document,stage("WPS","新建租赁合同甲，然后修改租赁合同甲并保存"));
  plan(true,document,stage("WPS","新建租赁合同甲并修改保存"));
  String fresh="新建完整租赁合同模板文档，由你自行起草正文，初稿每月10日前支付，保存文件名为测试租赁合同_顺序固定，保存后重新打开并核对内容。停止条件：文件已保存并重新打开核对完成。";
  String edit="打开本次新建的测试租赁合同_顺序固定文件，仅将租金支付条款由每月10日前支付修改为每月5日前支付，其他段落保持不变，保存后从文件列表重新打开并核对修改结果。只编辑本次新建文件，不签署、不发送。";
  yes(PlanCoverage.documentContinuation("在WPS新建租赁合同再只改支付日",fresh,edit),"owned document continuation not merged");
  yes(!PlanCoverage.documentContinuation("在WPS新建两份合同",fresh,edit.replace("顺序固定","另一份")),"different named document merged");
  yes(!PlanCoverage.documentContinuation("在WPS新建两份合同",fresh,"另建一个文档并修改为每月5日"),"new independent document merged");
  yes(!PlanCoverage.documentContinuation("在WPS新建两份合同",fresh,"新建另一份合同测试租赁合同_顺序固定副本，然后修改支付日"),"second contract merged");
  yes(!PlanCoverage.documentContinuation("在WPS修改旧文件",fresh,edit),"planner-created document permission invented");
  yes(!PlanCoverage.documentContinuation("在WPS新建合同",fresh,"打开WPS搜索合同模板"),"unrelated WPS search merged");
  yes(PlanCoverage.documentContinuation("在WPS新建合同", "新建合同文档并保存", "重新打开本次新建的文档，仅修改支付日并保存"),"unambiguous unnamed owned-file continuation");
  yes(!PlanCoverage.documentContinuation("在WPS新建合同", "新建合同文档并保存", "打开一份合同，仅修改支付日并保存"),"ambiguous existing document merged");
  System.out.println("PASS: "+count+" app-order, ASR coverage, revisits, same-app objects and owned-document continuation checks");
 }
}
