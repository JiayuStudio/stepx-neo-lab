package studio.jiayu.neoagent;
/** Keeps credentials and supplied delivery contacts out of planner/history requests. */
final class PrivateText {
 static boolean credential(String text){return text!=null&&text.matches("(?is).*sk-[a-z0-9_-]{12,}.*");}
 static String redact(String text){if(text==null)return "";return text.replaceAll("(?i)sk-[a-z0-9_-]{12,}","[凭据已隐藏]").replaceAll("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}","[联系方式留在本机]").replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])","[联系方式留在本机]").replaceAll("(收货地址|详细地址|我住在|送到|送至|配送到|地址[是为:：])[^\\n，。,；;!?！？]{1,120}","[配送资料留在本机，由用户在原应用处理]");}
}
