package studio.jiayu.neoagent;
/** Classify native editor evidence; an unavailable/partial read cannot confirm completion. */
final class DocumentReadback {
 static final int MAX_BODY=8000,PROBE=MAX_BODY+1;
 static String classify(String expected,String actual,int offset){return classify(expected,actual,offset,"");}
 static String classify(String expected,String actual,int offset,String request){
  if(expected==null||expected.isEmpty())return "unwritten";
  if(actual==null||offset<0)return "unavailable";
  if(offset!=0)return "partial";
  if(!DocumentText.same(actual,expected))return "mismatch";
  return DocumentScope.textComplete(request,actual)?"verified":"content_incomplete";
 }
}
