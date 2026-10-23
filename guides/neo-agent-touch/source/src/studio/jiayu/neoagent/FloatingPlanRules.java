package studio.jiayu.neoagent;

/** An overlay request cannot outlive cancellation or move to a different chat. */
final class FloatingPlanRules {
 enum Phase { IDLE, PLANNING, PENDING, COMMITTED, CLOSED }
 private long epoch;
 private String session="";
 private Phase phase=Phase.IDLE;
 synchronized long begin(String owner){if(phase==Phase.CLOSED||owner==null||owner.isEmpty())return -1;session=owner;phase=Phase.PLANNING;return ++epoch;}
 synchronized boolean planning(long token,String owner){return epoch==token&&session.equals(owner)&&phase==Phase.PLANNING;}
 synchronized boolean ready(long token,String owner){if(!planning(token,owner))return false;phase=Phase.PENDING;return true;}
 synchronized boolean pending(long token,String owner){return epoch==token&&session.equals(owner)&&phase==Phase.PENDING;}
 synchronized boolean committed(long token,String owner){if(!pending(token,owner))return false;phase=Phase.COMMITTED;return true;}
 synchronized void cancel(){if(phase!=Phase.CLOSED){++epoch;phase=Phase.IDLE;}}
 synchronized void close(){++epoch;phase=Phase.CLOSED;}
 synchronized boolean ownsPlanning(){return phase==Phase.PLANNING||phase==Phase.PENDING;}
 synchronized Phase phase(){return phase;}
}
