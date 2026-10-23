package studio.jiayu.neoagent;
import java.util.*;
/** Ephemeral feedback for non-dispatched candidates, bound to current native scene and identity. */
final class RejectedActions {
 static final class Entry {final String scene,target,action,parameters,reason;int attempts=1;Entry(String s,String t,String a,String p,String r){scene=s;target=t;action=a;parameters=p;reason=r;}}
 final ArrayDeque<Entry> entries=new ArrayDeque<>();
 void clear(){entries.clear();}
 Entry find(String scene,String target,String action,String parameters){for(Entry e:entries)if(e.scene.equals(scene)&&e.target.equals(target)&&e.action.equals(action)&&e.parameters.equals(parameters))return e;return null;}
 void record(String scene,String target,String action,String parameters,String reason){if(scene==null||target==null||action==null||parameters==null)return;Entry e=find(scene,target,action,parameters);if(e!=null){e.attempts++;return;}entries.addLast(new Entry(scene,target,action,parameters,reason));while(entries.size()>12)entries.removeFirst();}
}
