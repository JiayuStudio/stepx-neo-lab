package studio.jiayu.neoagent;
import java.text.Normalizer;import java.util.*;import java.util.regex.*;

/** Exact user-derived route endpoints. No geocoding, abbreviations, UI role inference or navigation authority. */
final class RouteIntent {
 private static final String END="，。,；;!?！？\\n";
 private static final String CURRENT="(?:我的)?当前位置|当前定位|我的位置|本机位置|所在位置|当前地点|current\\s*location|my\\s*location";
 private static final Pattern FROM=Pattern.compile("从([^"+END+"]{2,70}?)(?:到|至)([^"+END+"]{2,90})");
 private static final Pattern ENGLISH=Pattern.compile("\\bfrom\\s+([^,.;!?\\n]{2,70}?)\\s+to\\s+([^,.;!?\\n]{2,90})",Pattern.CASE_INSENSITIVE);
 private static final Pattern LABEL=Pattern.compile("(起点|终点)\\s*(?:设置为|设为|选为|为|是|[:：])?\\s*(.{1,80}?)(?=起点|终点|["+END+"]|$)");
 private static final Pattern NO_CURRENT=Pattern.compile("(?:不要|不用|不使用|不采用|不以|不从|别用|禁止使用|禁止用|不能使用|无需使用)[^"+END+"]{0,12}(?:"+CURRENT+")",Pattern.CASE_INSENSITIVE);
 private static final Pattern NO_CURRENT_AFTER=Pattern.compile("(?:"+CURRENT+")\\s*(?:不作为|不要作为|不能作为|不用于|不设为|不要设为)(?:起点|出发点|终点)",Pattern.CASE_INSENSITIVE);
 private static final Pattern CURRENT_QUERY=Pattern.compile(CURRENT,Pattern.CASE_INSENSITIVE);
 final String origin,destination;final boolean currentForbidden;final Set<String> modes;
 private RouteIntent(String from,String to,boolean forbidden,Set<String> modeWords){origin=from;destination=to;currentForbidden=forbidden;modes=modeWords;}
 static String normalized(String text){return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("\\s+","");}
 /** null means no unambiguous pair: retain the ordinary search policy, after explicit exclusions. */
 static Boolean allowed(String original,String query){RouteIntent route=parse(original);String input=normalized(query);if(route.currentForbidden&&CURRENT_QUERY.matcher(input).find())return false;if(!route.complete())return null;return route.accepts(input);}
 boolean complete(){return !origin.isEmpty()&&!destination.isEmpty();}
 static RouteIntent parse(String original){String source=Normalizer.normalize(original==null?"":original,Normalizer.Form.NFKC);ArrayList<String[]> pairs=new ArrayList<>();
  collect(source,FROM,pairs);collect(source,ENGLISH,pairs);String from="",to="";boolean conflicting=false;Matcher labels=LABEL.matcher(source);
  while(labels.find()){if(quoted(source,labels.start())||negatedBefore(source,labels.start()))continue;String endpoint=endpoint(labels.group(2),labels.group(1).equals("起点"));if(endpoint.isEmpty())continue;if(labels.group(1).equals("起点")){if(!from.isEmpty()&&!from.equals(endpoint))conflicting=true;from=endpoint;}else{if(!to.isEmpty()&&!to.equals(endpoint))conflicting=true;to=endpoint;}}
  if(!from.isEmpty()&&!to.isEmpty())pairs.add(new String[]{from,to});String a="",b="";for(String[] pair:pairs){if(!a.isEmpty()&&(!a.equals(pair[0])||!b.equals(pair[1])))conflicting=true;a=pair[0];b=pair[1];}
  if(conflicting){a="";b="";}return new RouteIntent(a,b,NO_CURRENT.matcher(source).find()||NO_CURRENT_AFTER.matcher(source).find(),modeWords(source));
 }
 private static void collect(String source,Pattern pattern,List<String[]> pairs){Matcher m=pattern.matcher(source);while(m.find()){if(quoted(source,m.start())||negatedBefore(source,m.start()))continue;String a=endpoint(m.group(1),true),b=endpoint(m.group(2),false);if(!a.isEmpty()&&!b.isEmpty())pairs.add(new String[]{a,b});}}
 private static String endpoint(String raw,boolean origin){String value=raw.trim();
  if(origin)value=value.replaceFirst("(?:出发|作为起点)$","").trim();
  else value=value.replaceFirst("(?i)\\s+(?:by|using)\\s+(?:public\\s+transit|transit|driving|walking|bicycling)(?:\\s+route)?$","").replaceFirst("(?i)\\s+(?:route|directions)$","").replaceFirst("(?:的)?(?:公共交通|公交|地铁|驾车|开车|步行|骑行)?(?:路线|线路|出行)(?:方案|结果)?$","").replaceFirst("(?:怎么走|如何走)$","").trim();
  value=value.replaceAll("[《》「」“”\"『』]","");String compact=normalized(value);
  if(compact.length()<2||compact.length()>60||!compact.matches("[\\p{L}\\p{N}·.'’&()（）/\\-]+")||compact.matches("(?s).*(?:然后|接着|不要|不用|禁止|搜索|查看|查询|路线|线路|作为起点|作为终点|起点|终点|出发到).*"))return "";
  return compact;
 }
 private boolean accepts(String query){if(exact(query))return true;for(String mode:modes){if(query.startsWith(mode)&&exact(query.substring(mode.length())))return true;if(query.endsWith(mode)&&exact(query.substring(0,query.length()-mode.length())))return true;}return false;}
 private boolean exact(String query){if(query.equals(origin)||query.equals(destination))return true;for(String separator:new String[]{"","到","至","→","->","—"})if(query.equals(origin+separator+destination)||query.equals("从"+origin+separator+destination)&&!separator.isEmpty())return true;if(query.equals("from"+origin+"to"+destination))return true;
  RouteIntent labeled=parseLabelsOnly(query);return labeled!=null&&origin.equals(labeled.origin)&&destination.equals(labeled.destination);
 }
 private static RouteIntent parseLabelsOnly(String query){if(!query.startsWith("起点")&&!query.startsWith("终点"))return null;Matcher m=LABEL.matcher(query);String from="",to="";int consumed=0;while(m.find()){if(m.start()!=consumed)return null;String value=endpoint(m.group(2),m.group(1).equals("起点"));if(value.isEmpty())return null;if(m.group(1).equals("起点")){if(!from.isEmpty())return null;from=value;}else{if(!to.isEmpty())return null;to=value;}consumed=m.end();}return consumed==query.length()&&!from.isEmpty()&&!to.isEmpty()?new RouteIntent(from,to,false,Collections.emptySet()):null;}
 private static Set<String> modeWords(String source){LinkedHashSet<String> result=new LinkedHashSet<>();String[][] groups={{"公共交通","公交","地铁"},{"驾车","开车"},{"步行"},{"骑行"}};for(String[] group:groups)for(String mode:group){Matcher m=Pattern.compile("(?:"+mode+"(?:路线|线路|方案|出行)|(?:乘坐|坐|选择|使用)"+mode+")").matcher(source);while(m.find())if(!quoted(source,m.start())&&!negatedBefore(source,m.start()))result.addAll(Arrays.asList(group));}return result;}
 private static boolean negatedBefore(String source,int start){String prefix=source.substring(0,start);int boundary=-1;for(int i=prefix.length()-1;i>=0;i--)if(END.indexOf(prefix.charAt(i))>=0){boundary=i;break;}String tail=prefix.substring(boundary+1).replaceAll("\\s+","");return tail.matches("(?s).*(?:不要|不用|不想|不必|无需|别|禁止|不能|不允许)(?:再|把|用|使用|帮我|给我|查看|查询|搜索|规划|设置|设为|作为|选择|采用|计算|走)*$");}
 private static boolean quoted(String source,int end){char closing=0;for(int i=0;i<end;i++){char ch=source.charAt(i);if(closing!=0){if(ch==closing)closing=0;continue;}int index="《「“\"『".indexOf(ch);if(index>=0)closing="》」”\"』".charAt(index);}return closing!=0;}
}
