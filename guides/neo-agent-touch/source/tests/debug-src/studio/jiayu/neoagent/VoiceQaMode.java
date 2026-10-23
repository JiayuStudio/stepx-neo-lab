package studio.jiayu.neoagent;
/** Debug fixture identity is fixed after entering an isolated voice mode. */
final class VoiceQaMode {
 enum Transition { KEEP, ISOLATE, REJECT }
 static Transition next(String current,String requested,boolean busy){
  String old=current==null?"":current,next=requested==null?"":requested;
  if(!next.isEmpty()&&!next.equals("guard")&&!next.equals("daily")&&!next.equals("pcm")&&!next.equals("plan")&&!next.equals("intent"))return Transition.REJECT;
  if(!old.isEmpty())return old.equals(next)?Transition.KEEP:Transition.REJECT;
  if(next.isEmpty())return Transition.KEEP;
  return busy?Transition.REJECT:Transition.ISOLATE;
 }
}
