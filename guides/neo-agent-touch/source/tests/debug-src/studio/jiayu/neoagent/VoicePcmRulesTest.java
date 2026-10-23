package studio.jiayu.neoagent;
import java.util.*;
final class VoicePcmRulesTest {
 static int n;static void yes(boolean ok){if(!ok)throw new AssertionError("PCM fixture rule failed");n++;}
 public static void main(String[]args){yes(VoicePcmRules.input(265796,VoicePcmRules.PCM_SHA256));yes(!VoicePcmRules.input(78720,VoicePcmRules.PCM_SHA256));yes(!VoicePcmRules.input(265796,"wrong"));
  String raw=VoicePcmRules.joinRaw(Arrays.asList("先召开 企业微信","","然后召开开高德地图"));yes(raw.equals("先召开 企业微信 然后召开开高德地图"));yes(!raw.contains("打开"));
  yes(VoicePcmRules.deliver("a","a",3,3,"pcm",true,true,true,raw));yes(!VoicePcmRules.deliver("a","b",3,3,"pcm",true,true,true,raw));yes(!VoicePcmRules.deliver("a","a",3,4,"pcm",true,true,true,raw));yes(!VoicePcmRules.deliver("a","a",3,3,"daily",true,true,true,raw));yes(!VoicePcmRules.deliver("a","a",3,3,"pcm",false,true,true,raw));yes(!VoicePcmRules.deliver("a","a",3,3,"pcm",true,false,true,raw));yes(!VoicePcmRules.deliver("a","a",3,3,"pcm",true,true,false,raw));yes(!VoicePcmRules.deliver("a","a",3,3,"pcm",true,true,true,"  "));yes(!VoicePcmRules.deliver("a","a",3,3,"pcm",true,true,true,new String(new char[6001])));
  VoiceSession session=new VoiceSession();long token=session.begin(true);session.segment(token,raw);VoiceSession.Result result=session.finish(token,true);yes(result.text.equals(VoiceSession.normalize(raw)));yes(result.text.contains("召开")&&!result.text.contains("打开"));
  System.out.println("PASS: VoicePcmRules "+n+" fixed audio identity, uncorrected text, same-session final-only delivery checks");}
}
