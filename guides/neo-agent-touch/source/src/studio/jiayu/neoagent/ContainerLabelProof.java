package studio.jiayu.neoagent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;

/** Complete bounded native-subtree evidence. A display label or truncated traversal is never a proof. */
public final class ContainerLabelProof {
 public static final int MAX_NODES=128,MAX_DEPTH=12,MAX_CHARACTERS=65536;
 public static final int VISIBLE=1,ENABLED=2,CLICKABLE=4,EDITABLE=8,LONG_CLICKABLE=16,SCROLLABLE=32,PASSWORD=64,CHECKABLE=128,CHECKED=256,SELECTED=512,FOCUSED=1024,FOCUSABLE=2048,ACCESSIBILITY_FOCUSED=4096,HEADING=8192,CONTENT_INVALID=16384;
 private ContainerLabelProof(){}
 public static final class Node {
  public final int depth,children,window,left,top,right,bottom,flags,inputType,movement,drawingOrder;
  public final String pkg,role,resource,uniqueId,text,description,hint,state,actions;
  public Node(int depth,int children,int window,String pkg,String role,String resource,String uniqueId,int left,int top,int right,int bottom,int flags,int inputType,int movement,int drawingOrder,String text,String description,String hint,String state,String actions){this.depth=depth;this.children=children;this.window=window;this.pkg=clean(pkg);this.role=clean(role);this.resource=clean(resource);this.uniqueId=clean(uniqueId);this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.flags=flags;this.inputType=inputType;this.movement=movement;this.drawingOrder=drawingOrder;this.text=clean(text);this.description=clean(description);this.hint=clean(hint);this.state=clean(state);this.actions=clean(actions);}
 }
 private static String clean(String value){return value==null?"":value;}
 public static final class Proof {
  private final String digest;public final int nodes,characters;
  private Proof(String digest,int nodes,int characters){this.digest=digest;this.nodes=nodes;this.characters=characters;}
  public boolean same(Proof other){return other!=null&&nodes==other.nodes&&characters==other.characters&&MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),other.digest.getBytes(StandardCharsets.US_ASCII));}
 }
 private static final class Level {int remaining;final boolean qualifying;Level(int remaining,boolean qualifying){this.remaining=remaining;this.qualifying=qualifying;}}
 public static final class Builder {
  private final MessageDigest digest;private final ArrayList<Level> stack=new ArrayList<>();private int nodes,characters;private boolean invalid,rootClosed,hasLabel,finished;
  public Builder(){try{digest=MessageDigest.getInstance("SHA-256");}catch(Exception unavailable){throw new IllegalStateException("SHA-256 unavailable");}}
  public void incomplete(){invalid=true;}
  public boolean valid(){return !invalid&&!finished;}
  public boolean begin(Node node){if(!valid()||node==null||rootClosed||node.depth!=stack.size()||node.depth>MAX_DEPTH||node.children<0||node.children>MAX_NODES||++nodes>MAX_NODES||(node.flags&PASSWORD)!=0){invalid=true;return false;}
   boolean qualifying=(node.flags&VISIBLE)!=0;
   if(!stack.isEmpty()){Level parent=stack.get(stack.size()-1);if(parent.remaining<=0){invalid=true;return false;}parent.remaining--;qualifying=qualifying&&parent.qualifying&&(node.flags&(CLICKABLE|EDITABLE|LONG_CLICKABLE))==0;}
   else if(nodes!=1){invalid=true;return false;}
   stack.add(new Level(node.children,qualifying));integer(0x4e4f4445);integer(node.depth);integer(node.children);integer(node.window);integer(node.left);integer(node.top);integer(node.right);integer(node.bottom);integer(node.flags);integer(node.inputType);integer(node.movement);integer(node.drawingOrder);
   for(String value:new String[]{node.pkg,node.role,node.resource,node.uniqueId,node.text,node.description,node.hint,node.state,node.actions})if(!string(value))return false;
   if(qualifying&&(!node.text.trim().isEmpty()||!node.description.trim().isEmpty()))hasLabel=true;
   return true;
  }
  public boolean end(){if(!valid()||stack.isEmpty()||stack.get(stack.size()-1).remaining!=0){invalid=true;return false;}stack.remove(stack.size()-1);integer(0x454e444e);if(stack.isEmpty())rootClosed=true;return true;}
  public Proof finish(){if(!valid()||!rootClosed||!stack.isEmpty()||nodes<2||!hasLabel){invalid=true;return null;}finished=true;StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return new Proof(hex.toString(),nodes,characters);}
  // Hash exact UTF-16 units: UTF-8's default replacement could merge distinct unpaired surrogates.
  private boolean string(String text){if(text.length()>MAX_CHARACTERS-characters){invalid=true;return false;}characters+=text.length();integer(text.length());for(int i=0;i<text.length();i++){char value=text.charAt(i);digest.update((byte)(value>>>8));digest.update((byte)value);}return true;}
  private void integer(int value){digest.update((byte)(value>>>24));digest.update((byte)(value>>>16));digest.update((byte)(value>>>8));digest.update((byte)value);}
 }
}
