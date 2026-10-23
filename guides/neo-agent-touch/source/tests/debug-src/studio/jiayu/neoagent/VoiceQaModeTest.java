package studio.jiayu.neoagent;
final class VoiceQaModeTest {
 static int count;static void check(String old,String next,boolean busy,VoiceQaMode.Transition expected){if(VoiceQaMode.next(old,next,busy)!=expected)throw new AssertionError("Unexpected fixture transition");count++;}
 public static void main(String[]args){
  check("","daily",false,VoiceQaMode.Transition.ISOLATE);check("","guard",false,VoiceQaMode.Transition.ISOLATE);
  check("","daily",true,VoiceQaMode.Transition.REJECT);check("","guard",true,VoiceQaMode.Transition.REJECT);
  check("guard","daily",false,VoiceQaMode.Transition.REJECT);check("daily","guard",false,VoiceQaMode.Transition.REJECT);
  check("guard","",false,VoiceQaMode.Transition.REJECT);check("daily","",false,VoiceQaMode.Transition.REJECT);
  check("guard","guard",false,VoiceQaMode.Transition.KEEP);check("daily","daily",true,VoiceQaMode.Transition.KEEP);
  check("","",true,VoiceQaMode.Transition.KEEP);check("","conflict",false,VoiceQaMode.Transition.REJECT);
  check("","pcm",false,VoiceQaMode.Transition.ISOLATE);check("","pcm",true,VoiceQaMode.Transition.REJECT);check("guard","pcm",false,VoiceQaMode.Transition.REJECT);check("daily","pcm",false,VoiceQaMode.Transition.REJECT);check("pcm","daily",false,VoiceQaMode.Transition.REJECT);check("pcm","",false,VoiceQaMode.Transition.REJECT);check("pcm","pcm",false,VoiceQaMode.Transition.KEEP);check("","plan",false,VoiceQaMode.Transition.ISOLATE);check("plan","pcm",false,VoiceQaMode.Transition.REJECT);check("plan","daily",false,VoiceQaMode.Transition.REJECT);
  System.out.println("PASS: VoiceQaMode "+count+" first-entry isolation, fixed instance mode, busy rejection and same-mode reuse checks");
 }
}
