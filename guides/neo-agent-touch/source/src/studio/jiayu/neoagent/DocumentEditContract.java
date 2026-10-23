package studio.jiayu.neoagent;
import java.util.regex.*;
/** Validate the actual before/after body, independently of a model-supplied find range. */
final class DocumentEditContract {
 static final Pattern TARGET=Pattern.compile("(?:修改|调整|更改|变更|改)(?:为|成|到)\\s*每月\\s*([0-9]{1,2})\\s*(?:日|号)");
 static final Pattern MONTH=Pattern.compile("每月\\s*([0-9]{1,2})\\s*(?:日|号)");
 static boolean constrained(String request){return request!=null&&request.matches("(?s).*(其他(?:段落|条款|内容|部分|文字).{0,8}(?:保持不变|不变|不改)|只把|仅修改|只修改).*" );}
 static int day(String request){if(request==null)return 0;Matcher m=TARGET.matcher(request);if(!m.find())return 0;int n=Integer.parseInt(m.group(1));return n>0&&n<=31?n:-1;}
 static boolean paymentParagraph(String body,int at,String request){int start=body.lastIndexOf('\n',Math.max(0,at-1))+1,end=body.indexOf('\n',at);if(end<0)end=body.length();String p=body.substring(start,end);return request!=null&&request.contains("租金")?p.contains("租金")||p.contains("交租"):p.matches("(?s).*(支付|付款|缴纳|交租|租金).*" );}
 static boolean targetSatisfied(String request,String body){int wanted=day(request);if(wanted==0)return true;if(wanted<0||body==null)return false;Matcher m=MONTH.matcher(body);while(m.find())if(Integer.parseInt(m.group(1))==wanted&&paymentParagraph(body,m.start(),request))return true;return false;}
 static boolean permits(String request,String before,String after){
  if(before==null||after==null)return false;if(DocumentText.same(before,after))return true;if(!constrained(request))return true;
  String a=DocumentText.normalized(before),b=DocumentText.normalized(after);int wanted=day(request);
  if(wanted<0)return false;
  if(wanted>0){Matcher m=MONTH.matcher(a);while(m.find()){if(!paymentParagraph(a,m.start(),request)||Integer.parseInt(m.group(1))==wanted)continue;String expected=a.substring(0,m.start(1))+wanted+a.substring(m.end(1));if(expected.equals(b))return true;}return false;}
  int prefix=0;while(prefix<a.length()&&prefix<b.length()&&a.charAt(prefix)==b.charAt(prefix))prefix++;
  int suffix=0;while(suffix<a.length()-prefix&&suffix<b.length()-prefix&&a.charAt(a.length()-1-suffix)==b.charAt(b.length()-1-suffix))suffix++;
  return a.substring(prefix,a.length()-suffix).indexOf('\n')<0&&b.substring(prefix,b.length()-suffix).indexOf('\n')<0;
 }
}
