package studio.jiayu.neoagent;
public class DocumentNameRulesTest {
 static int n;static void check(boolean value){if(!value)throw new AssertionError("case "+n);n++;}
 public static void main(String[] args){
  String request="在WPS新建一个名为“Neo适配检查”的文档，自己写三段原理文字并保存。",token="abcdef1234567890";
  check(DocumentNameRules.requested(request).equals("Neo适配检查"));check(DocumentNameRules.expected(request,token,false).equals("Neo适配检查"));
  check(DocumentNameRules.matches("Neo适配检查",DocumentNameRules.expected(request,token,false)));check(DocumentNameRules.matches("Neo适配检查.docx",DocumentNameRules.expected(request,token,false)));
  check(!DocumentNameRules.matches("其他文件",DocumentNameRules.expected(request,token,false)));check(!DocumentNameRules.matches("Neo适配检查旧版",DocumentNameRules.expected(request,token,false)));
  check(DocumentNameRules.requested("新建文件，命名为\"Alpha.docx\"，保存").equals("Alpha"));check(DocumentNameRules.requested("新建名为Alpha的文档，保存").equals("Alpha"));check(DocumentNameRules.requested("文件名：Alpha，保存").equals("Alpha"));
  String generated=DocumentNameRules.expected("新建文档介绍Agent原理",token,false);check(generated.equals("Neo文档_abcdef123456"));check(DocumentNameRules.safe(generated));
  check(!generated.equals(DocumentNameRules.expected("新建文档介绍Agent原理","abcdef6543210000",false)));check(generated.equals(DocumentNameRules.expected("新建文档介绍Agent原理",token,false)));
  String unique=DocumentNameRules.expected(request,token,true);check(unique.equals("Neo适配检查_abcdef123456"));check(!DocumentNameRules.matches("Neo适配检查",unique));check(DocumentNameRules.matches(unique+".docx",unique));
  for(String invalid:new String[]{"",".","..","a/b","a\\b","a\nb","a\rb","a\tb","a:b","a?b","a*b","a|b","a<b","a>b","a\"b","a."})check(!DocumentNameRules.safe(invalid));
  check(!DocumentNameRules.safe(new String(new char[81]).replace('\0','x')));check(DocumentNameRules.safe(new String(new char[80]).replace('\0','x')));
  check(DocumentNameRules.conflict("文件已存在，是否替换"));check(DocumentNameRules.overwriteAction("替换","文件已存在，是否替换"));check(DocumentNameRules.overwriteAction("确定","已存在同名文件"));
  check(!DocumentNameRules.overwriteAction("取消","文件已存在，是否替换"));check(!DocumentNameRules.overwriteAction("保留两者","文件已存在，是否替换"));check(!DocumentNameRules.overwriteAction("保存","保存路径"));
  check(DocumentNameRules.overwriteAction("替换文件","文件已存在，是否替换"));check(DocumentNameRules.overwriteAction("确认覆盖文件","文件已存在，是否替换"));check(!DocumentNameRules.overwriteAction("不替换","文件已存在，是否替换"));
  check(DocumentNameRules.requested("名为“../原件”的文档").isEmpty());check(DocumentNameRules.requested("名为“甲”的文档，名为“乙”的文件").isEmpty());
  String longRequest="名为“"+new String(new char[80]).replace('\0','x')+"”的文档";check(DocumentNameRules.expected(longRequest,token,true).length()==80);
  boolean invalidToken=false;try{DocumentNameRules.expected(request,"bad",false);}catch(IllegalArgumentException expected){invalidToken=true;}check(invalidToken);
  System.out.println("PASS: "+n+" document request name, nonce, collision and overwrite guard cases");
 }
}
