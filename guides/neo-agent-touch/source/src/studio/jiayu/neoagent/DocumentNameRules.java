package studio.jiayu.neoagent;
import java.util.regex.*;
/** Bind a fresh document's name to the user's request, never to unrelated editor text. */
final class DocumentNameRules {
 static final int MAX=80;
 static String base(String value){if(value==null)return "";return value.trim().replaceFirst("(?i)\\.docx$","");}
 static boolean safe(String value){String s=base(value);return !s.isEmpty()&&s.length()<=MAX&&!s.equals(".")&&!s.equals("..")&&!s.matches("(?s).*[\\p{Cntrl}/\\\\:*?\"<>|].*")&&!s.endsWith(".");}
 static String requested(String request){
  if(request==null)return "";
  Matcher m=Pattern.compile("(?:名为|命名为|文件名(?:为|是)?|名称(?:为|是)?|叫做)\\s*[:：]?\\s*(?:[“\"「『《]([^”\"」』》\\n]{1,120})[”\"」』》]|([^\\n，。；;]{1,120}?)(?=的?(?:文档|文件)(?:[，。；;\\s]|$)|[，。；;]|$))").matcher(request);
  String found="";while(m.find()){String v=base(m.group(1)==null?m.group(2):m.group(1));if(!safe(v))return "";if(!found.isEmpty()&&!found.equals(v))return "";found=v;}return found;
 }
 static String expected(String request,String token,boolean conflict){String nonce=token==null?"":token.replaceAll("[^a-zA-Z0-9]","");if(nonce.length()<8)throw new IllegalArgumentException("Document task nonce required");nonce=nonce.substring(0,Math.min(12,nonce.length()));String chosen=requested(request);if(chosen.isEmpty())return "Neo文档_"+nonce;if(!conflict)return chosen;return chosen.substring(0,Math.min(chosen.length(),MAX-nonce.length()-1))+"_"+nonce;}
 static boolean matches(String candidate,String expected){return safe(candidate)&&safe(expected)&&base(candidate).equals(base(expected));}
 static boolean conflict(String context){return context!=null&&context.matches("(?s).*(同名文件|文件已存在|文档已存在|已存在同名|文件名已存在|文件名重复|是否(?:替换|覆盖)|替换(?:现有|已有)文件|覆盖(?:现有|已有)文件).*" );}
 static boolean overwriteAction(String label,String context){if(!conflict(context)||label==null)return false;String v=label.trim();if(v.matches("^(取消|返回|关闭|不替换|不覆盖|保留两者|另存为|更改名称)$"))return false;return v.matches("(?s).*(替换|覆盖|确认|确定|保存|继续).*")||v.equals("是");}
}
