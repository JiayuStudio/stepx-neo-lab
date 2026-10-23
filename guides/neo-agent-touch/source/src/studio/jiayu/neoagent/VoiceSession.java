package studio.jiayu.neoagent;
/** One foreground utterance. Partial hypotheses can never become task instructions. */
final class VoiceSession {
 enum State { IDLE, PREPARING, LISTENING, FINISHING }
 static final int MAX_TEXT=6000;
 private long generation;private State state=State.IDLE;private boolean direct;private final StringBuilder text=new StringBuilder();
 synchronized long begin(boolean autoSend){if(state!=State.IDLE)return -1;generation++;state=State.PREPARING;direct=autoSend;text.setLength(0);return generation;}
 synchronized boolean current(long token){return token==generation&&state!=State.IDLE;}
 synchronized boolean listening(long token){if(token!=generation||state!=State.PREPARING)return false;state=State.LISTENING;return true;}
 synchronized boolean stop(long token){if(token!=generation||state!=State.LISTENING)return false;state=State.FINISHING;return true;}
 synchronized boolean forceDraft(long token){if(!current(token))return false;direct=false;return true;}
 synchronized boolean segment(long token,String finalText){if(!current(token))return false;String value=normalize(finalText);if(value.isEmpty())return true;if(text.length()+value.length()+1>MAX_TEXT)throw new IllegalArgumentException("语音过长，请分次说出目标");if(text.length()>0)text.append(' ');text.append(value);return true;}
 synchronized String preview(long token,String partial){if(!current(token))return "";String p=normalize(partial);return (text.toString()+(text.length()>0&&!p.isEmpty()?" ":"")+p).trim();}
 synchronized Result finish(long token,boolean foreground){if(token!=generation||state==State.IDLE)return null;String value=text.toString().trim();boolean send=direct&&foreground;state=State.IDLE;text.setLength(0);return !foreground||value.isEmpty()?null:new Result(value,send);}
 synchronized void cancel(){generation++;state=State.IDLE;text.setLength(0);}
 synchronized State state(){return state;}
 static String normalize(String value){if(value==null)return "";return value.replaceAll("[\\p{Cc}&&[^\\n\\t]]","").trim().replaceAll("\\s+"," ").replaceAll("(?<=[\\p{IsHan}]) (?=[\\p{IsHan}])","");}
 static final class Result {final String text;final boolean direct;Result(String value,boolean send){text=value;direct=send;}}
}
