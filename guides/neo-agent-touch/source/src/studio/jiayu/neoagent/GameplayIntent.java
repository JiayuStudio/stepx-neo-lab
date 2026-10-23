package studio.jiayu.neoagent;
import java.util.regex.*;

/** Extra exploration budget follows the user's play request, never a page instruction. */
final class GameplayIntent {
 static final Pattern GAME=Pattern.compile("羊了个羊|小游戏|消除游戏|拼图游戏|游戏关卡");
 static final Pattern PLAY=Pattern.compile("试玩|游玩|玩(?!具|法|家|笑|意)|挑战|通关|完成.{0,12}关卡|消除(?!游戏)");
 static boolean play(String original){
  if(original==null||original.length()>12000)return false;
  // A later disclaimer about a tutorial/victory screen does not cancel an earlier real play request.
  // Carry the named game across one adjacent clause: '打开羊了个羊，然后试玩一分钟'.
  boolean previousGame=false;
  for(String clause:original.split("[，。；,;!?！？\\n]+")){
   boolean game=GAME.matcher(clause).find(),context=game||previousGame;previousGame=game;
   if(clause.matches("(?is).*(搜索|搜一搜|搜|播放|观看|查找|查询|查看|查|看|找|写|生成|制作|新建|编辑|撰写).*(视频|教程|攻略).*")){previousGame=false;continue;}
   if(clause.matches("(?is).*(介绍|解释|讲讲|怎么玩|如何玩|怎样玩).*")){previousGame=false;continue;}
   if(clause.matches("(?is).*(购买|下单|买).*(玩具|商品|手机|模型).*")){previousGame=false;continue;}
   if(!context)continue;
   Matcher verbs=PLAY.matcher(clause);while(verbs.find()){
    String before=clause.substring(0,verbs.start());
    if(before.matches("(?is).*(不要|不想|不用|不必|无需|不需要|别|禁止|不能|不)(?:再|去|来|实际|尝试|短时|稍微|给我|帮我|一起|\\s)*$"))continue;
    Matcher queries=Pattern.compile("搜索|搜一搜|搜|查找|查询|查看|看看|播放|观看|找(?!到)|写|生成|制作|新建|编辑|撰写").matcher(before);int last=-1;while(queries.find())last=queries.end();
    if(last>=0&&!before.substring(last).matches("(?is).*(然后|接着|最后|再|并|且).*"))continue;
    return true;
   }
  }
  return false;
 }
 static int steps(String original,int requested){boolean game=play(original);return Math.max(1,Math.min(game?200:40,game?Math.max(200,requested):requested));}
 static long duration(String original){return play(original)?1800000L:600000L;}
}
