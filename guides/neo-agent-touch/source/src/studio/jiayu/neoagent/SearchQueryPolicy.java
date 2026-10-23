package studio.jiayu.neoagent;
import java.text.Normalizer;import java.util.*;import java.util.regex.*;
/** Search refinements may add filters; they do not grant any purchase or message-sending action. */
final class SearchQueryPolicy {
 static String normalized(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+","");}
 static boolean searchField(String resource,String hint,String description){String all=(resource==null?"":resource)+" "+(hint==null?"":hint)+" "+(description==null?"":description);return !all.matches("(?is).*(password|message_input|chat_input|send_message|密码|发送消息|聊天输入).*")&&all.matches("(?is).*(search|query|keyword|搜索|搜一搜|搜商品|搜视频).*" );}
 static boolean allowed(String goal,String original,String target,String pkg,String text){return allowed(goal,original,target,pkg,text,false);}
 static boolean allowed(String goal,String original,String target,String pkg,String text,boolean independentSearchFields){if(text==null||text.trim().isEmpty()||text.length()>120||text.matches("(?s).*[\\p{Cntrl}\\u200B-\\u200F\\u202A-\\u202E].*")||PrivateText.credential(text)||!PrivateText.redact(text).equals(text))return false;
  boolean purchase=PurchasePolicy.grant(original,pkg).order;if(purchase&&!PurchasePolicy.ordinaryItem(text))return false;
  if(independentSearchFields){Boolean route=RouteIntent.allowed(original,text);if(route!=null)return route;}
  String query=normalized(text);if(target!=null&&!target.trim().isEmpty()){Matcher words=Pattern.compile("[\\p{L}\\p{N}]+").matcher(Normalizer.normalize(target,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT));boolean found=false,complete=true;while(words.find()){found=true;if(!query.contains(words.group()))complete=false;}if(found&&complete)return true;if(!independentSearchFields)return false;}
  if(purchase)return true;String source=normalized(goal)+(independentSearchFields?normalized(original)+normalized(target):"");if(source.contains(query))return true;
  // Song/artist and other public query parts can be reordered or separated by spaces/quotes.
  Matcher parts=Pattern.compile("[\\p{L}\\p{N}]+").matcher(Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT));int count=0;boolean known=true;while(parts.find()){if(!source.contains(parts.group()))known=false;count++;}if(known&&count>1&&count<=12)return true;
  // Chinese queries also concatenate phrases without spaces (artist+title or title+artist).
  String compact=query.replaceAll("[^\\p{L}\\p{N}]","");String pool=source.replaceAll("[^\\p{L}\\p{N}]","");if(compact.length()<4)return false;boolean[] reached=new boolean[compact.length()+1];reached[0]=true;
  for(int end=2;end<=compact.length();end++)for(int start=0;start<=end-2;start++)if(reached[start]&&pool.contains(compact.substring(start,end))){reached[end]=true;break;}return reached[compact.length()];
 }
}
