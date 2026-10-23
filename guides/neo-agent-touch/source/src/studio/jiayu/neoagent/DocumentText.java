package studio.jiayu.neoagent;
/** Exact editor readback, allowing only line-ending and outer blank differences. */
final class DocumentText {
 static String normalized(String text){return text==null?"":text.replace("\r\n","\n").replace('\r','\n').trim();}
 static boolean same(String actual,String expected){return expected!=null&&!normalized(expected).isEmpty()&&normalized(actual).equals(normalized(expected));}
 static String replaceOnce(String body,String find,String replacement)throws Exception{if(body==null||find==null||find.isEmpty()||replacement==null||find.equals(replacement))throw new Exception("替换内容无效");int at=body.indexOf(find);if(at<0||at!=body.lastIndexOf(find))throw new Exception("原文片段必须唯一匹配");return body.substring(0,at)+replacement+body.substring(at+find.length());}
 static boolean wholeSelection(String actual,String expected,int offset,int start,int end){if(actual==null||offset!=0||!same(actual,expected))return false;int a=Math.min(start,end),b=Math.max(start,end);return a>=0&&b<=actual.length()&&same(actual.substring(a,b),expected)&&normalized(actual.substring(0,a)).isEmpty()&&normalized(actual.substring(b)).isEmpty();}
}
