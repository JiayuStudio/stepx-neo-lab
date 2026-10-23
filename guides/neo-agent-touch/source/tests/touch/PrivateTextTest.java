package studio.jiayu.neoagent;
public final class PrivateTextTest {
 public static void main(String[] args){
  String text="送到示例街道1号，手机号13800000000；邮件person@example.invalid；key sk-abcdefghijklmnop";
  String clean=PrivateText.redact(text);if(clean.contains("示例街道")||clean.contains("13800000000")||clean.contains("person@")||clean.contains("sk-"))throw new AssertionError("private text leaked");
  if(!PrivateText.credential(text)||PrivateText.credential("我平时咖啡无糖"))throw new AssertionError("credential recognition");
  if(!PrivateText.redact("咖啡无糖，黑色手机支架50元以内").equals("咖啡无糖，黑色手机支架50元以内"))throw new AssertionError("ordinary request changed");
  // Both planners expose full original source clauses as routing context. The
  // clause builder must receive the same sanitized original as TaskWorkflow.
  String request="先打开淘宝搜索海信g7pro，"+text+"；然后打开京东比价，只看价格，不下单、不付款";
  String routed=PrivateText.redact(request);
  java.util.List<StageSourceRules.AppSpan> apps=java.util.Arrays.asList(
   new StageSourceRules.AppSpan("com.taobao.taobao",routed.indexOf("淘宝"),routed.indexOf("淘宝")+2),
   new StageSourceRules.AppSpan("com.jingdong.app.mall",routed.indexOf("京东"),routed.indexOf("京东")+2));
  StringBuilder sources=new StringBuilder();for(StageSourceRules.Clause clause:StageSourceRules.clauses(routed,apps))sources.append(clause.source);
  String routingSources=sources.toString();
  if(routingSources.contains("示例街道")||routingSources.contains("13800000000")||routingSources.contains("person@")||routingSources.contains("sk-"))throw new AssertionError("routing source leaked contact or credentials");
  for(String required:new String[]{"淘宝","京东","海信g7pro","只看价格","不下单","不付款"})if(!routingSources.contains(required))throw new AssertionError("routing source lost ordinary goal scope");
  if(!PrivateText.redact(routed).equals(routed))throw new AssertionError("routing sanitization not idempotent");
  System.out.println("PASS: credentials, contacts and supplied address removed; ordinary preferences preserved");
 }
}
