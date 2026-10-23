package studio.jiayu.neoagent;
import java.util.*;

/** Protocol boundary cases: hostile URLs, beta precedence, hash ambiguity, and signer mismatch. */
public final class UpdateRulesTest {
 static int checks;static void check(boolean ok){checks++;if(!ok)throw new AssertionError("update rule "+checks);}
 static UpdateRules.Candidate item(String version,String name,String url,long size){return new UpdateRules.Candidate("neo-agent-touch-v"+version,version,name,url,size,"","");}
 static UpdateRules.Candidate app(String version){String name="Neo-Agent-Touch-"+version+".apk";return item(version,name,"https://github.com/"+UpdateRules.REPO+"/releases/download/neo-agent-touch-v"+version+"/"+name,222437);}
 public static void main(String[] args){
  check(UpdateRules.newer("0.1.16-beta.1","0.1.15-beta.1"));check(UpdateRules.newer("0.1.16-beta.10","0.1.16-beta.2"));check(UpdateRules.newer("0.1.16","0.1.16-rc.8"));check(!UpdateRules.newer("0.1.16-beta.1","0.1.16"));check(!UpdateRules.newer("0.1.16+new","0.1.16+old"));check(!UpdateRules.newer("0.1.15","0.1.16-beta.1"));
  check(UpdateRules.version("01.2.3")==null);check(UpdateRules.version("1.2.3-beta.01")==null);check(UpdateRules.version("1.2.3-")==null);check(UpdateRules.version("1.2.3-01a")!=null);check(UpdateRules.newer("1.2.3-alpha.a","1.2.3-alpha.99"));
  check(UpdateRules.releaseVersion("neo-agent-touch-film-v1.0.4")==null);check(UpdateRules.releaseVersion("source-v9.0.0")==null);check(UpdateRules.releaseVersion("neo-agent-touch-v0.1.16-beta.1")!=null);
  UpdateRules.Candidate safe=app("0.1.16-beta.1"),newer=app("0.1.16-beta.10");check(UpdateRules.select(Arrays.asList(safe,newer,app("0.1.15")),"0.1.15-beta.1")==newer);check(UpdateRules.select(Arrays.asList(safe),"0.1.16")==null);
  check(UpdateRules.apkName("Neo-Agent-Touch-0.1.16-beta.1-release.apk","0.1.16-beta.1"));check(!UpdateRules.apkName("Neo-Agent-Touch-0.1.16-beta.1-debug.apk","0.1.16-beta.1"));check(!UpdateRules.apkName("Neo-Agent-Touch-0.1.16-beta.1-source.zip","0.1.16-beta.1"));
  check(UpdateRules.assetUrl(safe.url,safe.tag,safe.name));for(String url:new String[]{safe.url.replace("https:","http:"),safe.url.replace("github.com","github.com.attacker.invalid"),safe.url.replace("github.com","@github.com"),safe.url+"?token=x",safe.url+"#x",safe.url.replace("github.com","github.com:443"),safe.url.replace("/JiayuStudio/","/other/"),safe.url.replace("/download/","/download/../")})check(!UpdateRules.assetUrl(url,safe.tag,safe.name));
  check(UpdateRules.downloadHost("https://release-assets.githubusercontent.com/release-asset?a=b"));check(!UpdateRules.downloadHost("https://release-assets.githubusercontent.com.attacker.invalid/a"));check(!UpdateRules.downloadHost("https://127.0.0.1/a"));check(!UpdateRules.downloadHost("file:///update.apk"));
  String a=String.join("",Collections.nCopies(64,"a")),b=String.join("",Collections.nCopies(64,"b"));check(UpdateRules.digest("sha256:"+a).equals(a));check(UpdateRules.digest("sha512:"+a).isEmpty());check(UpdateRules.digest(a.substring(1)).isEmpty());check(UpdateRules.checksum(a+"  "+safe.name+"\n"+b+"  source.zip",safe.name).equals(a));check(UpdateRules.checksum(a+" *"+safe.name,safe.name).equals(a));check(UpdateRules.checksum(a+"  ./"+safe.name,safe.name).isEmpty());check(UpdateRules.checksum(a+"  "+safe.name+"\n"+b+"  "+safe.name,safe.name).isEmpty());
  check(!UpdateRules.validSize(0));check(!UpdateRules.validSize(-1));check(UpdateRules.validSize(UpdateRules.MAX_APK_BYTES));check(!UpdateRules.validSize(UpdateRules.MAX_APK_BYTES+1));
  Set<String> installed=new HashSet<>(Arrays.asList(a)),same=new HashSet<>(Arrays.asList(a)),other=new HashSet<>(Arrays.asList(b));check(UpdateRules.archive(UpdateRules.PACKAGE,17,safe.version,16,safe.version,installed,same));check(!UpdateRules.archive("other.app",17,safe.version,16,safe.version,installed,same));check(!UpdateRules.archive(UpdateRules.PACKAGE,16,safe.version,16,safe.version,installed,same));check(!UpdateRules.archive(UpdateRules.PACKAGE,17,"0.1.17",16,safe.version,installed,same));check(!UpdateRules.archive(UpdateRules.PACKAGE,17,safe.version,16,safe.version,installed,other));check(!UpdateRules.archive(UpdateRules.PACKAGE,17,safe.version,16,safe.version,new HashSet<>(),new HashSet<>()));
  check(!UpdateRules.archive(UpdateRules.PACKAGE,17,"0.1.16-beta.1+other",16,"0.1.16-beta.1",installed,same));
  System.out.println("UpdateRulesTest: "+checks+" checks passed");
 }
}
