package studio.jiayu.neoagent;
public class SearchTargetTest {
 static int n;static void equal(String input,String expected){String actual=SearchTarget.fromUser(input);if(!actual.equals(expected))throw new AssertionError("Target: "+actual+" expected "+expected);n++;}
 public static void main(String[] args){
  equal("打开QQ音乐，搜索许嵩的《有何不可》，播放免费版本","许嵩 有何不可");
  equal("搜索《不再犹豫》，然后播放","不再犹豫");equal("搜索「不再犹豫」，播放","不再犹豫");equal("搜索《你好，李焕英》，看看结果","你好，李焕英");
  equal("通过发现里的搜一搜搜索 StepX Neo，看到结果停止","StepX Neo");equal("打开微信搜一搜，看到页面结束","");
  equal("找到我的文件中的搜索入口，搜索关键词 测试，看到结果停止","测试");equal("搜索关键字：测试文档，看到结果停止","测试文档");
  equal("搜索 StepX Neo 16+256G 手机，确认型号","StepX Neo 16+256G 手机");equal("搜索《StepX Neo》16+256G 手机，然后看价格","StepX Neo 16+256G 手机");
  equal("去哔哩哔哩搜索航拍中国，打开一个视频","航拍中国");equal("搜索一个视频，随便看看","");equal("不涉及查询的普通聊天","");
  String original="在QQ音乐搜索许嵩的《有何不可》，播放免费版本";String t=SearchTarget.fromUser(original);
  for(String query:new String[]{"许嵩 有何不可","有何不可 许嵩","许嵩有何不可","有何不可许嵩"}){if(!SearchQueryPolicy.allowed(original,original,t,"com.tencent.qqmusic",query))throw new AssertionError("Derived target rejected public song query");n++;}
  if(SearchQueryPolicy.allowed(original,original,t,"com.tencent.qqmusic","周杰伦 有何不可"))throw new AssertionError("Wrong performer accepted");n++;
  String quoted="去京东淘宝拼多多搜索「海信G7 Pro」65英寸的价格",exact="海信G7 Pro 65英寸";
  if(!SearchTarget.product(quoted,exact).equals(exact))throw new AssertionError("Quoted model/specification lost its native public search target");n++;
  if(!SearchQueryPolicy.allowed(quoted,quoted,SearchTarget.product(quoted,exact),"com.taobao.taobao",exact))throw new AssertionError("Native product query rejected its own exact target");n++;
  if(SearchQueryPolicy.allowed(quoted,quoted,SearchTarget.product(quoted,exact),"com.taobao.taobao","海信"))throw new AssertionError("Brand-only query weakened the exact model/specification");n++;
  System.out.println("PASS: "+n+" original-query parsing, quoted titles, interface nouns and music runtime-target checks");
 }
}
