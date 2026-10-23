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
  String article="手机内Agent先获取当前授权应用的画面与系统控件。\n模型根据用户目标选择下一步动作。\n手机执行动作并重新观察，核对目标是否达成。",request="新建名为“Neo适配检查”的文档，写三段介绍手机内Agent工作原理的文字";
  eq("verified",DocumentReadback.classify(article,article,0,request));eq("verified",DocumentReadback.classify(article,article.replace("\n","\r\n"),0,request));
  eq("mismatch",DocumentReadback.classify(article,article.replace("手机执行","模型猜测"),0,request));eq("mismatch",DocumentReadback.classify(article,article+"\n额外文字",0,request));eq("partial",DocumentReadback.classify(article,article,1,request));eq("unavailable",DocumentReadback.classify(article,null,0,request));
  eq("content_incomplete",DocumentReadback.classify(article,article,0,"起草租赁合同模板"));eq("verified",DocumentReadback.classify(body,body,0,"起草租赁合同模板"));eq("mismatch",DocumentReadback.classify(body,body.replace("押金","租金"),0,"起草租赁合同模板"));
  eq("mismatch",DocumentReadback.classify(" "," ",0,request));
  System.out.println("PASS: "+n+" document readback contradiction, unavailable, partial and recovery cases");
 }
}
