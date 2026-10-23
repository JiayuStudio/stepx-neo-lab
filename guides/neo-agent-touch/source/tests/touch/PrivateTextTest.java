package studio.jiayu.neoagent;
public final class PrivateTextTest {
 public static void main(String[] args){
  String text="送到示例街道1号，手机号13800000000；邮件person@example.invalid；key sk-abcdefghijklmnop";
  String clean=PrivateText.redact(text);if(clean.contains("示例街道")||clean.contains("13800000000")||clean.contains("person@")||clean.contains("sk-"))throw new AssertionError("private text leaked");
  if(!PrivateText.credential(text)||PrivateText.credential("我平时咖啡无糖"))throw new AssertionError("credential recognition");
  if(!PrivateText.redact("咖啡无糖，黑色手机支架50元以内").equals("咖啡无糖，黑色手机支架50元以内"))throw new AssertionError("ordinary request changed");
  System.out.println("PASS: credentials, contacts and supplied address removed; ordinary preferences preserved");
 }
}
