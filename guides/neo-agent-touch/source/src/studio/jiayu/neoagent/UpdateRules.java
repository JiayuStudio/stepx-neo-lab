package studio.jiayu.neoagent;

import java.math.BigInteger;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Pure rules for this app's public release channel. No endpoint is user supplied. */
public final class UpdateRules {
 public static final String PACKAGE="studio.jiayu.neoagent",REPO="JiayuStudio/stepx-neo-lab";
 public static final String RELEASES="https://github.com/"+REPO+"/releases";
 public static final long MAX_APK_BYTES=150L*1024*1024;
 private static final Pattern SEMVER=Pattern.compile("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?");
 private UpdateRules(){}
 public static final class Version implements Comparable<Version>{
  final BigInteger major,minor,patch;final String[] pre;public final String text;
  private Version(String text,Matcher m){this.text=text;major=new BigInteger(m.group(1));minor=new BigInteger(m.group(2));patch=new BigInteger(m.group(3));pre=m.group(4)==null?new String[0]:m.group(4).split("\\.");}
  public int compareTo(Version v){int n=major.compareTo(v.major);if(n!=0)return n;n=minor.compareTo(v.minor);if(n!=0)return n;n=patch.compareTo(v.patch);if(n!=0)return n;if(pre.length==0||v.pre.length==0)return pre.length==v.pre.length?0:pre.length==0?1:-1;for(int i=0;i<Math.min(pre.length,v.pre.length);i++){String a=pre[i],b=v.pre[i];boolean an=a.matches("[0-9]+"),bn=b.matches("[0-9]+");n=an&&bn?new BigInteger(a).compareTo(new BigInteger(b)):an!=bn?(an?-1:1):a.compareTo(b);if(n!=0)return n;}return Integer.compare(pre.length,v.pre.length);}
 }
 public static Version version(String text){if(text==null||text.length()>96)return null;Matcher m=SEMVER.matcher(text);if(!m.matches())return null;if(m.group(4)!=null)for(String p:m.group(4).split("\\."))if(p.matches("[0-9]+")&&p.length()>1&&p.charAt(0)=='0')return null;return new Version(text,m);}
 public static Version releaseVersion(String tag){String prefix="neo-agent-touch-v";return tag!=null&&tag.startsWith(prefix)?version(tag.substring(prefix.length())):null;}
 public static boolean newer(String remote,String current){Version a=version(remote),b=version(current);return a!=null&&b!=null&&a.compareTo(b)>0;}
 public static boolean apkName(String name,String version){return name!=null&&(name.equals("Neo-Agent-Touch-"+version+".apk")||name.equals("Neo-Agent-Touch-"+version+"-release.apk"));}
 public static boolean assetUrl(String url,String tag,String name){try{URI u=new URI(url);return "https".equals(u.getScheme())&&"github.com".equals(u.getHost())&&u.getPort()==-1&&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null&&u.getPath().equals("/"+REPO+"/releases/download/"+tag+"/"+name);}catch(Exception e){return false;}}
 public static boolean downloadHost(String url){try{URI u=new URI(url);return "https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&Arrays.asList("github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","github-releases.githubusercontent.com").contains(u.getHost());}catch(Exception e){return false;}}
 public static String digest(String value){if(value==null)return "";String d=value.startsWith("sha256:")?value.substring(7):value;return d.matches("[0-9a-fA-F]{64}")?d.toLowerCase(Locale.ROOT):"";}
 /** SHA256SUMS is accepted only for an exact, unambiguous filename entry. */
 public static String checksum(String manifest,String name){String result="";if(manifest==null||manifest.length()>262144)return result;for(String line:manifest.split("\\r?\\n")){Matcher m=Pattern.compile("^([0-9a-fA-F]{64})[ \\t]+\\*?([^ \\t]+)[ \\t]*$").matcher(line);if(!m.matches()||!name.equals(m.group(2)))continue;String d=m.group(1).toLowerCase(Locale.ROOT);if(!result.isEmpty()&&!result.equals(d))return "";result=d;}return result;}
 public static boolean validSize(long size){return size>0&&size<=MAX_APK_BYTES;}
 public static boolean archive(String pkg,long candidateCode,String candidateVersion,long installedCode,String expectedVersion,Set<String> installedSigners,Set<String> candidateSigners){Version a=version(candidateVersion),b=version(expectedVersion);return PACKAGE.equals(pkg)&&candidateCode>installedCode&&a!=null&&b!=null&&expectedVersion.equals(candidateVersion)&&a.compareTo(b)==0&&installedSigners!=null&&!installedSigners.isEmpty()&&installedSigners.equals(candidateSigners);}
 public static final class Candidate {public final String tag,version,name,url,sha256,checksumUrl;public final long size,versionCode;public Candidate(String tag,String version,String name,String url,long size,String sha256,String checksumUrl){this(tag,version,name,url,size,sha256,checksumUrl,0);}public Candidate(String tag,String version,String name,String url,long size,String sha256,String checksumUrl,long code){this.tag=tag;this.version=version;this.name=name;this.url=url;this.size=size;this.sha256=sha256;this.checksumUrl=checksumUrl;this.versionCode=code;}}
 public static Candidate select(Collection<Candidate> candidates,String installed){Version current=version(installed);if(current==null)return null;Candidate best=null;for(Candidate c:candidates){Version v=releaseVersion(c.tag);if(v==null||!v.text.equals(c.version)||v.compareTo(current)<=0||!validSize(c.size)||!apkName(c.name,c.version)||!assetUrl(c.url,c.tag,c.name))continue;if(best==null||v.compareTo(version(best.version))>0)best=c;}return best;}
}
