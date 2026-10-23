package studio.jiayu.neoagent;
public final class GameplayIntentTest {
 static int count;static void yes(boolean result){if(!result)throw new AssertionError("Wrong play budget scope");count++;}
 public static void main(String[] args){
  for(String text:new String[]{"在微信小游戏里玩羊了个羊并尝试通关","帮我挑战小游戏关卡","试玩这个消除游戏"})yes(GameplayIntent.play(text));
  for(String text:new String[]{"去B站播放羊了个羊通关视频","搜索羊了个羊游戏教程","淘宝找游戏手机","打开微信搜索StepX Neo","WPS新建租赁合同", "搜索小游戏攻略"})yes(!GameplayIntent.play(text));
  String real="在微信中找到并短时试玩官方羊了个羊小游戏，自行学习真实画面规则，进入棋盘后进行几次有把握的操作，试玩约一分钟即可结束，不需要通关。不要把入口或教程页面说成实际游戏通关。";
  yes(GameplayIntent.play(real));yes(GameplayIntent.steps(real,24)==200);yes(GameplayIntent.duration(real)==1800000L);
  for(String text:new String[]{"不想玩羊了个羊，只看攻略","去B站搜索羊了个羊试玩视频","在WPS写一份羊了个羊小游戏试玩攻略","介绍羊了个羊小游戏怎么玩"})yes(!GameplayIntent.play(text));
  for(String text:new String[]{"打开羊了个羊，然后试玩一分钟","打开羊了个羊，然后玩一会","搜索并打开羊了个羊，试玩一分钟"})yes(GameplayIntent.play(text));
  for(String text:new String[]{"搜索羊了个羊攻略，看看怎么玩","不要玩羊了个羊","不玩羊了个羊，只看视频","购买羊了个羊玩具","搜索消除游戏","搜索羊了个羊通关"})yes(!GameplayIntent.play(text));
  yes(!GameplayIntent.play(null));yes(GameplayIntent.steps("玩羊了个羊",24)==200);
  yes(GameplayIntent.steps("玩羊了个羊",999)==200);yes(GameplayIntent.steps("搜索手机",999)==40);
  yes(GameplayIntent.steps("搜索手机",24)==24);yes(GameplayIntent.duration("玩羊了个羊")==1800000L);
  yes(GameplayIntent.duration("搜索手机")==600000L);
  System.out.println("PASS: GameplayIntent "+count+" play/search distinction and bounded budgets");
 }
}
