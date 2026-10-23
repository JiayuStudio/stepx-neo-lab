package studio.jiayu.neoagent;
public final class RouteIntentTest {
 static int checks;static void yes(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
 static boolean input(String original,String query){return SearchQueryPolicy.allowed("查看公交或驾车路线，搜索北京机场",original,"上海人民广场 北京机场","com.autonavi.minimap",query,true);}
 static void query(boolean allowed,String original,String text){yes(input(original,text)==allowed,"query="+text+" original="+original);}
 public static void main(String[] args){
  String live="打开高德地图，搜索上海人民广场，查看从上海虹桥站到上海人民广场的公共交通路线方案，看到路线结果后结束。不用我的当前位置作为起点，不启动实时导航、不打车、不支付、不接受新协议。";
  RouteIntent route=RouteIntent.parse(live);yes(route.complete()&&route.origin.equals("上海虹桥站")&&route.destination.equals("上海人民广场"),"Route endpoints swallowed instructions");
  for(String q:new String[]{"上海虹桥站","上海人民广场","上海虹桥站 上海人民广场","上海虹桥站到上海人民广场","从上海虹桥站至上海人民广场","上海虹桥站→上海人民广场","上海虹桥站->上海人民广场","上海虹桥站上海人民广场公交"})query(true,live,q);
  for(String q:new String[]{"我的当前位置","当前位置","当前定位","我的位置","本机位置","上海人民广场 我的当前位置","公交 北京机场","北京机场公交","上海虹桥站到北京机场","上海人民广场到上海虹桥站","上海虹桥站上海人民广场驾车","上海虹桥","人民广场","公交","驾车"})query(false,live,q);
  // New endpoints demonstrate that no city, station, attraction or provider label is hardcoded.
  String dynamic="查询从杭州东站到浙江省博物馆的步行路线，不用当前定位。";
  for(String q:new String[]{"杭州东站","浙江省博物馆","从杭州东站到浙江省博物馆","杭州东站 浙江省博物馆 步行"})query(true,dynamic,q);
  for(String q:new String[]{"上海人民广场","杭州东站到上海人民广场","浙江省博物馆到杭州东站","公交浙江省博物馆","我的位置","杭州东"})query(false,dynamic,q);
  String labeled="起点设为武汉站，终点为湖北省博物馆，查看驾车路线。";
  route=RouteIntent.parse(labeled);yes(route.origin.equals("武汉站")&&route.destination.equals("湖北省博物馆"),"Labeled endpoints not parsed");
  for(String q:new String[]{"武汉站","湖北省博物馆","武汉站到湖北省博物馆","起点武汉站终点湖北省博物馆","终点为湖北省博物馆起点为武汉站","驾车武汉站湖北省博物馆"})query(true,labeled,q);
  for(String q:new String[]{"起点湖北省博物馆终点武汉站","武汉站 武汉大学","湖北省博物馆公交","武汉站驾车北京机场"})query(false,labeled,q);
  String labelsReverse="终点：人民公园；起点：成都东站。";route=RouteIntent.parse(labelsReverse);yes(route.origin.equals("成都东站")&&route.destination.equals("人民公园"),"Label order changed route direction");query(true,labelsReverse,"成都东站→人民公园");query(false,labelsReverse,"人民公园→成都东站");
  String english="Find directions from Central Station to City Hall. Do not use my location.";route=RouteIntent.parse(english);yes(route.complete()&&route.origin.equals("centralstation")&&route.destination.equals("cityhall"),"English from/to not parsed");query(true,english,"Central Station");query(true,english,"from Central Station to City Hall");query(false,english,"City Hall to Central Station");query(false,english,"Central Station Airport");
  // Only canonical formatting is normalized; no substring/fuzzy geographical alias is granted.
  String quoted="从“南京南站”到「南京博物院」的公交路线";query(true,quoted,"南京南站 南京博物院");query(false,quoted,"南京站");query(false,quoted,"南站");
  String contradictory="从杭州东站到浙江省博物馆，不要用我的当前位置作为起点";query(false,contradictory,"浙江省博物馆 我的当前位置");
  yes(RouteIntent.allowed("搜索人民公园，不用我的当前位置作为起点","我的当前位置").equals(Boolean.FALSE),"Current-location exclusion required a parsed route");
  for(String original:new String[]{"搜索人民公园，当前位置不作为起点","搜索人民公园，当前位置不要作为起点","搜索人民公园，起点不用当前位置"}){
   yes(!SearchQueryPolicy.allowed("搜索人民公园",original,"人民公园","com.autonavi.minimap","当前位置",true),"Postposed or labeled negative fell back to source words");
   yes(SearchQueryPolicy.allowed("搜索人民公园",original,"人民公园","com.autonavi.minimap","人民公园",true),"Negative clause blocked ordinary POI query");
  }
  yes(RouteIntent.allowed("搜索人民公园，不用我的当前位置作为起点","人民公园")==null,"Ordinary place search incorrectly became a route contract");
  yes(SearchQueryPolicy.allowed("搜索人民公园","搜索人民公园，不用我的当前位置作为起点","人民公园","com.autonavi.minimap","人民公园",true),"Ordinary place search blocked");
  yes(RouteIntent.allowed("在地图搜索《从南到北书店》","从南到北书店")==null,"Quoted place title treated as a route");
  yes(SearchQueryPolicy.allowed("搜索从南到北书店","在地图搜索《从南到北书店》","从南到北书店","com.autonavi.minimap","从南到北书店",true),"Quoted place query blocked");
  yes(RouteIntent.allowed("不要从杭州东站到浙江省博物馆，搜索人民公园","人民公园")==null,"Negated route granted endpoint scope");
  yes(RouteIntent.allowed("从杭州东站到浙江省博物馆；从武汉站到湖北省博物馆","武汉站")==null,"Ambiguous multiple routes guessed one pair");
  yes(RouteIntent.allowed("起点杭州东站，终点浙江省博物馆，起点武汉站","武汉站")==null,"Conflicting endpoint correction guessed a route");
  // The strict branch cannot authorize model-only targets, and does not change music/product policy.
  yes(!SearchQueryPolicy.allowed("搜索北京机场",dynamic,"北京机场","com.autonavi.minimap","北京机场",true),"Planner target bypassed original route");
  yes(!SearchQueryPolicy.allowed("搜索StepX Neo手机",live,"StepX Neo 16+256G","com.taobao.taobao","StepX Neo"),"Unrelated shopping rules weakened");
  System.out.println("PASS: RouteIntent "+checks+" exact endpoint, direction, negative-current-location, dynamic place and fallback checks");
 }
}
