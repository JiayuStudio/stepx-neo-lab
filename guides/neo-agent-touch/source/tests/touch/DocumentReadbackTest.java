package studio.jiayu.neoagent;
public class DocumentReadbackTest {
 static int n;static void eq(String want,String actual){if(!want.equals(actual))throw new AssertionError("case "+n+": "+actual);n++;}
 public static void main(String[] args){String body="租赁合同\n甲方：[]\n乙方：[]\n租金每月5日前支付，押金[]\n签署：[]";
  eq("verified",DocumentReadback.classify(body,body,0));eq("verified",DocumentReadback.classify(body,"\n"+body+"\n",0));
  eq("mismatch",DocumentReadback.classify(body,body.replace("5日","10日"),0));eq("mismatch",DocumentReadback.classify(body,body+"旧条款",0));eq("mismatch",DocumentReadback.classify(body,"其他文件"+body,0));
  eq("partial",DocumentReadback.classify(body,body,1));eq("unavailable",DocumentReadback.classify(body,body,-1));eq("unavailable",DocumentReadback.classify(body,null,0));eq("unwritten",DocumentReadback.classify("",body,0));
  DocumentReopen p=new DocumentReopen();p.wrote(2);p.listObserved();p.openRequested();p.bodyRead();if(!p.currentRead())throw new AssertionError();p.invalidateRead();if(p.currentRead())throw new AssertionError("contradicted read retained");n++;
  p.listObserved();p.openRequested();p.invalidateRead();if(!p.pending)throw new AssertionError("pending retry discarded");p.bodyRead();if(!p.currentRead())throw new AssertionError();n++;
  String max=body+new String(new char[8000-body.length()]).replace('\0','x');eq("mismatch",DocumentReadback.classify(max,max+"!",0));if(DocumentReadback.PROBE<=max.length())throw new AssertionError("suffix probe truncated at body limit");n++;
  System.out.println("PASS: "+n+" document readback contradiction, unavailable, partial and recovery cases");
 }
}
