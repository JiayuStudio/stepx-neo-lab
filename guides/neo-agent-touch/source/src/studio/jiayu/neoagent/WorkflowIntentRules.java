package studio.jiayu.neoagent;
import java.util.regex.*;
/** Conservative category contradiction checks, not a general intent or permission parser. */
final class WorkflowIntentRules {
 private static String unquoted(String text){StringBuilder out=new StringBuilder();char close=0;for(char c:(text==null?"":text).toCharArray()){if(close!=0){if(c==close)close=0;out.append(' ');continue;}int q="《「“\"『".indexOf(c);if(q>=0){close="》」”\"』".charAt(q);out.append(' ');}else out.append(c);}return out.toString();}
 static boolean positivePlayback(String text){String s=unquoted(text);Matcher m=Pattern.compile("播放(?!器|页|界面|按钮|入口|设置|列表)|听歌|听音乐").matcher(s);while(m.find()){
  int boundary=Math.max(Math.max(s.lastIndexOf('，',m.start()),s.lastIndexOf('。',m.start())),Math.max(s.lastIndexOf('；',m.start()),s.lastIndexOf('\n',m.start())));String before=s.substring(boundary+1,m.start());
  if(before.matches("(?s).*(?:不要|不用|不再|不得|禁止|不能|不必|无需|不需要|不允许|别|不|停止|暂停|关闭|取消)(?:\\s|去|再|自动|实际|真的|给我|帮我|进行|开始)*$"))continue;
  // The app name may stand between the denial and the verb (e.g. 不要在某应用播放).
  // Ambiguous unpunctuated denial scope is rejected, never treated as a positive grant.
  if(before.matches("(?s).*(?:不要|不用|不再|不得|禁止|不能|不必|无需|不需要|不允许|不想|请勿|别|不在|不用).{0,40}$"))continue;
  if(before.matches("(?s).*(?:写入|写下|写|输入|记录|转写|说明|教程|歌词|文字|文本|文案|不要执行).{0,18}$"))continue;
  return true;
 }return false;}
 static boolean independentMusic(String goal){String s=unquoted(goal);if(!positivePlayback(s))return false;
  if(s.matches("(?is).*(?:语音|通话|录音|视频|剧集|第.{0,6}集|MV|歌词|文字|文本|教程|小程序|公众号).*"))return false;
  return s.matches("(?s).*(?:歌曲|音乐|听歌|歌单|专辑|单曲).*");
 }
 static boolean conflict(String category,String source,String goal,boolean appExplicitInSource){
  if(!"通讯".equals(category)||!independentMusic(goal))return false;
  // An explicitly requested in-app media capability is different from an app-less music goal.
  return !(appExplicitInSource&&(source==null?"":source).matches("(?s).*(?:小程序|公众号|聊天|语音|录音|音频文件|歌曲链接|音乐文件).*"));
 }
}
