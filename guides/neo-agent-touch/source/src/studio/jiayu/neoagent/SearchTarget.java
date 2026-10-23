package studio.jiayu.neoagent;
import java.util.regex.*;
/** Derive public search objects from actual verbs, respecting quoted titles and interface nouns. */
final class SearchTarget {
 static final Pattern SEARCH=Pattern.compile("搜索|搜一下|查找|找一下|(?<!搜一)搜(?!一搜)");
 static final Pattern STOP=Pattern.compile("然后|并且|并|再|打开|播放|观看|看看|的视频");
 static String product(String user,String proposal){String t=proposal==null?"":proposal.trim();String publicText=user==null?"":user.replaceAll("[《》「」“”\"『』]"," ");return MutationRules.targetMentioned(user,t)||MutationRules.targetMentioned(publicText,t)?t:shopping(user);}
 static String shopping(String user){if(user==null||user.matches("(?s).*(不要|禁止|别|不用|不必).{0,3}(搜|查找|找一下).*"))return "";Matcher searches=SEARCH.matcher(user);int count=0;while(searches.find())count++;return count==1?fromUser(user):"";}
 static String fromUser(String user){if(user==null)return "";Matcher m=SEARCH.matcher(user);while(m.find()){
  String rest=user.substring(m.end()).trim().replaceFirst("^(?:关键词|关键字)\\s*[:：]?\\s*","");if(rest.matches("(?s)^(入口|按钮|功能|输入框|框|栏)(?:[，。,；;\\s]|$).*"))continue;
  StringBuilder term=new StringBuilder();char closing=0;for(int i=0;i<rest.length()&&term.length()<=120;i++){char ch=rest.charAt(i);if(closing!=0){term.append(ch);if(ch==closing)closing=0;continue;}
   int q="《「“\"『".indexOf(ch);if(q>=0){closing="》」”\"』".charAt(q);term.append(ch);continue;}
   if("，。,；;\n!?！？".indexOf(ch)>=0||STOP.matcher(rest).region(i,rest.length()).lookingAt())break;term.append(ch);
  }
  String result=term.toString().trim().replaceAll("的\\s*(?=[《「“\"『])","").replaceAll("[《「“\"『》」”』]"," ").replaceAll("\\s+"," ").trim();
  if(result.length()>=2&&result.length()<=60&&!result.matches("^(?:一个|个)?视频$"))return result;
 }return "";}
}
