package studio.jiayu.neoagent;
/** Relevance changes ordering, never membership in the user's approved candidate set. */
final class CandidateRoutingRules {
 static int priority(boolean related,boolean preferred){return related?(preferred?0:1):(preferred?2:3);}
}
