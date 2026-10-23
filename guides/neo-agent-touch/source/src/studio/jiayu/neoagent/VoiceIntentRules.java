package studio.jiayu.neoagent;

/** Request/session/turn ownership for one foreground utterance and its optional replacement plan. */
final class VoiceIntentRules {
 enum Phase { RECORDING, DRAFT, FINAL, PLANNING, READY, COMMITTED, CANCELLED }
 enum Final { REJECT, DRAFT, SEND }
 static final class Ticket {
  final String request,session;final long captureTurn;final boolean holdsGoal;
  private long planTurn=-1;private Phase phase=Phase.RECORDING;
  Ticket(String request,String session,long turn,boolean hold){this.request=request;this.session=session;captureTurn=turn;holdsGoal=hold;}
  Phase phase(){return phase;}
 }
 private Ticket active;
 Ticket begin(String request,String session,long turn,boolean hold){
  if(request==null||request.isEmpty()||session==null||session.isEmpty()||turn<0)throw new IllegalArgumentException("voice_intent_identity_required");
  if(active!=null&&active.phase!=Phase.COMMITTED)active.phase=Phase.CANCELLED;
  return active=new Ticket(request,session,turn,hold);
 }
 boolean owns(Ticket ticket,String request,String session){return ticket!=null&&active==ticket&&ticket.request.equals(request)&&ticket.session.equals(session)&&ticket.phase!=Phase.CANCELLED;}
 Final result(Ticket ticket,String request,String session,long turn,boolean foreground,boolean direct,String text){
  if(!owns(ticket,request,session)||ticket.phase!=Phase.RECORDING||ticket.captureTurn!=turn)return Final.REJECT;
  if(!foreground||text==null||text.trim().isEmpty()){ticket.phase=Phase.CANCELLED;return Final.REJECT;}
  ticket.phase=direct?Phase.FINAL:Phase.DRAFT;return direct?Final.SEND:Final.DRAFT;
 }
 boolean keyboardDraft(Ticket ticket,String session,long turn){if(!owns(ticket,ticket==null?"":ticket.request,session)||ticket.phase!=Phase.RECORDING||ticket.captureTurn!=turn)return false;ticket.phase=Phase.DRAFT;return true;}
 boolean manualDraft(Ticket ticket,String session,long turn){if(!owns(ticket,ticket==null?"":ticket.request,session)||ticket.phase!=Phase.DRAFT||ticket.captureTurn!=turn)return false;ticket.phase=Phase.FINAL;return true;}
 boolean draft(Ticket ticket){if(active!=ticket||ticket==null||ticket.phase!=Phase.FINAL)return false;ticket.phase=Phase.DRAFT;return true;}
 boolean canPlan(Ticket ticket,String session,long turn){return owns(ticket,ticket==null?"":ticket.request,session)&&ticket.phase==Phase.FINAL&&ticket.captureTurn==turn;}
 boolean planning(Ticket ticket,String session,long before,long next){if(!canPlan(ticket,session,before)||next<=before)return false;ticket.planTurn=next;ticket.phase=Phase.PLANNING;return true;}
 boolean planningCurrent(Ticket ticket,String session,long turn){return owns(ticket,ticket==null?"":ticket.request,session)&&ticket.phase==Phase.PLANNING&&ticket.planTurn==turn;}
 boolean ready(Ticket ticket,String session,long turn){if(!planningCurrent(ticket,session,turn))return false;ticket.phase=Phase.READY;return true;}
 boolean mayCommit(Ticket ticket,String session,long turn){return owns(ticket,ticket==null?"":ticket.request,session)&&ticket.phase==Phase.READY&&ticket.planTurn==turn;}
 boolean committed(Ticket ticket,String session,long turn){if(!mayCommit(ticket,session,turn))return false;ticket.phase=Phase.COMMITTED;return true;}
 boolean cancel(String request){if(active==null||request==null||!active.request.equals(request)||active.phase==Phase.CANCELLED)return false;active.phase=Phase.CANCELLED;return true;}
 void cancelCapture(){if(active!=null&&active.phase==Phase.RECORDING)active.phase=Phase.CANCELLED;}
 void abandon(Ticket ticket){if(active==ticket&&ticket!=null&&ticket.phase!=Phase.COMMITTED)ticket.phase=Phase.CANCELLED;}
}
