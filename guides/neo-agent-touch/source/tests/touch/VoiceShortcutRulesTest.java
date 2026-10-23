package studio.jiayu.neoagent;
public class VoiceShortcutRulesTest {
 static int count;static void yes(boolean b){count++;if(!b)throw new AssertionError(count);}
 public static void main(String[] args){for(String s:new String[]{"语音","语音输入","开始语音输入","点击语音输入","Voice Input"," voice typing ","start voice input"})yes(VoiceShortcutRules.activationLabel(s));for(String s:new String[]{"停止语音输入","取消","发送语音","语音输入设置","启用云语音","语音消息","录音","麦克风权限","语音输入法广告","press to send voice","搜索","","false"})yes(!VoiceShortcutRules.activationLabel(s));yes(!VoiceShortcutRules.activationLabel(null));yes(VoiceShortcutRules.unique(1));yes(!VoiceShortcutRules.unique(0));yes(!VoiceShortcutRules.unique(2));for(int mask=0;mask<32;mask++){boolean c=(mask&1)!=0,r=(mask&2)!=0,a=(mask&4)!=0,f=(mask&8)!=0,u=(mask&16)!=0;yes(VoiceShortcutRules.allowed(c,r,a,f,u)==(mask==29));}System.out.println("PASS: VoiceShortcutRules "+count+" exact activation, ambiguity, focus and ownership checks");}
}
