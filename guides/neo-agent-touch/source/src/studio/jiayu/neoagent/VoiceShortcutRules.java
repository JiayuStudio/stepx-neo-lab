package studio.jiayu.neoagent;
/** Only explicit activation controls in the current keyboard are eligible. */
final class VoiceShortcutRules {
 private VoiceShortcutRules(){}
 static boolean activationLabel(CharSequence raw){String s=raw==null?"":raw.toString().trim().replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);return s.equals("语音输入")||s.equals("语音")||s.equals("点击语音输入")||s.equals("开始语音输入")||s.equals("voice input")||s.equals("voice typing")||s.equals("start voice input");}
 static boolean allowed(boolean connected,boolean running,boolean attached,boolean focused,boolean unlocked){return connected&&!running&&attached&&focused&&unlocked;}
 static boolean unique(int count){return count==1;}
}
