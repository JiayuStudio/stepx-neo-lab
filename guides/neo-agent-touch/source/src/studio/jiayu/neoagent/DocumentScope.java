package studio.jiayu.neoagent;
/** A fresh-document workflow may not read or open the user's existing WPS files. */
final class DocumentScope {
 static boolean applies(String pkg){return "cn.wps.moffice_eng".equals(pkg);}
 static String entry(String text,String description){for(String v:new String[]{text,description})if(v!=null&&v.trim().matches("^(首页|最近|文档|我的|新建|新建文档|创建|创建文档|创建新文件|文字|文字文档|WPS文字|Word|Word文档|空白|空白文档|空白文字|新建空白|新建空白文档|新建文字|新建DOCX文档|新建docx文档|返回|取消|关闭|跳过|暂不登录|不登录|仅本地|本地文档|\\+|新建按钮)$"))return v.trim();return "";}
 static boolean blank(String text,String description){String e=entry(text,description);return e.matches("^(空白|空白文档|空白文字|新建空白|新建空白文档|新建DOCX文档|新建docx文档)$");}
 static boolean home(String text){return text.contains("最近")&&(text.contains("新建")||text.contains("创建"))&&(text.contains("首页")||text.contains("我的")||text.contains("文档"));}
 static boolean textComplete(String text){return text.contains("租赁合同")&&text.contains("甲方")&&text.contains("乙方")&&text.contains("租金")&&text.contains("押金")&&text.matches("(?s).*(签署|签字|盖章).*");}
 static boolean sameFile(String label,String name){if(label==null||name==null||name.isEmpty())return false;String base=name.trim().replaceFirst("(?i)\\.docx$","");return java.util.regex.Pattern.compile("(?:^|[\\s,，:：])"+java.util.regex.Pattern.quote(base)+"(?i:\\.docx)?(?=$|[\\s,，;；])").matcher(label.trim()).find();}

 static boolean saved(String text){return text!=null&&text.matches("(?s).*(保存成功|已保存|保存完成|同步成功|已同步).*" );}
}
