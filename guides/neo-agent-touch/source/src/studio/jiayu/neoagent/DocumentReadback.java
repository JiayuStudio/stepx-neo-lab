package studio.jiayu.neoagent;
/** Classify native editor evidence; an unavailable/partial read cannot confirm completion. */
final class DocumentReadback {
 static final int MAX_BODY=8000,PROBE=MAX_BODY+1;
 static String classify(String expected,String actual,int offset){
  if(expected==null||expected.isEmpty())return "unwritten";
  if(actual==null||offset<0)return "unavailable";
  if(offset!=0)return "partial";
  return DocumentText.same(actual,expected)&&DocumentScope.textComplete(actual)?"verified":"mismatch";
 }
}
