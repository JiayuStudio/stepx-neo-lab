package studio.jiayu.neoagent;
import java.util.*;
/** Debug-only fixed public PCM identity and final-delivery guard. Contains no corrected task text. */
final class VoicePcmRules {
 static final String PCM_SHA256="ba535b61cd34213cca25ce95d0c8d1bd7bc13f0d837a73e4b0ea0c1657dee391";
 static final int PCM_BYTES=265796;
 static boolean input(int bytes,String sha){return bytes==PCM_BYTES&&PCM_SHA256.equals(sha);}
 static String joinRaw(List<String> finalSegments){StringBuilder all=new StringBuilder();for(String text:finalSegments){if(text==null||text.trim().isEmpty())continue;if(all.length()>0)all.append(' ');all.append(text);}return all.toString();}
 static boolean deliver(String owner,String current,long before,long now,String mode,boolean foreground,boolean controllerSame,boolean leaseCurrent,String raw){return owner!=null&&!owner.isEmpty()&&owner.equals(current)&&before==now&&"pcm".equals(mode)&&foreground&&controllerSame&&leaseCurrent&&raw!=null&&!raw.trim().isEmpty()&&raw.length()<=6000;}
}
