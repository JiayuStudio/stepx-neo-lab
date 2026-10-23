package studio.jiayu.neoagent;
import java.util.*;

/** Recipient-local application scopes. Visual consent never belongs to a neighbouring application. */
final class RoutineAccessRules {
 static final String LEGACY_SCOPE="full_target_app_v1",SCOPE="full_target_app_v2";
 static final int MAX_RECORDS=16;
 private RoutineAccessRules(){}
 static LinkedHashMap<String,Boolean> merge(Map<String,Boolean> previous,Collection<String> apps,boolean vision){
  LinkedHashMap<String,Boolean> result=new LinkedHashMap<>();
  if(previous!=null)for(Map.Entry<String,Boolean> item:previous.entrySet()){
   requirePackage(item.getKey());if(item.getValue()==null)throw new IllegalArgumentException("Missing application scope");result.put(item.getKey(),item.getValue());
  }
  if(apps==null)throw new IllegalArgumentException("Missing applications");
  for(String pkg:apps){requirePackage(pkg);result.put(pkg,vision||Boolean.TRUE.equals(result.get(pkg)));}
  return result;
 }
 /** A v1 record explicitly assigned one scope to each listed app; retain that existing consent. */
 static LinkedHashMap<String,Boolean> legacy(Collection<String> apps,boolean vision){return merge(null,apps,vision);}
 static boolean allows(Map<String,Boolean> scopes,Collection<String> apps,boolean vision){
  if(scopes==null||apps==null||apps.isEmpty())return false;
  for(String pkg:apps){if(pkg==null||pkg.isEmpty()||!scopes.containsKey(pkg)||scopes.get(pkg)==null||vision&&!Boolean.TRUE.equals(scopes.get(pkg)))return false;}
  return true;
 }
 private static void requirePackage(String pkg){if(pkg==null||pkg.isEmpty()||!pkg.equals(pkg.trim()))throw new IllegalArgumentException("Invalid application scope");}
}
