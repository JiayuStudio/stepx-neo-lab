package studio.jiayu.neoagent;
import java.text.Normalizer;import java.util.*;import java.util.regex.*;
/** Check explicit app instructions in source order. This is not a proof of task completion. */
final class PlanCoverage {
 static final class Mention {
  final String app;final int start,end;
  Mention(String a,int s){this(a,s,s+a.length());}
  Mention(String a,int s,int e){app=a;start=s;end=e;}
 }
 static final class Stage {
  final String app,goal;
  Stage(String a,String g){app=a;goal=g==null?"":g;}
 }
 private static final String ACTION="搜索|搜一搜|搜一下|搜|查找|查一下|查询|查看|看看|看|查|新建|创建|写|编辑|修改|保存|播放|听|导航|规划|比价|关注|购买|买|加入|打开|找|订";
 private static final Pattern PREFIX=Pattern.compile("(?:打开|启动|进入|切换到|切到|返回到|回到|返回|使用|用|在|去|先|再|最后)\\s*[「“\"『《]?\\s*$");
 private static final Pattern FLOW=Pattern.compile("(?:然后|接着|最后|再去|再打开|再在|接下来|之后|随后|依次|分别|同时|并且|以及)\\s*$");
 // Observed offline-ASR launch confusion. This only locates exact app/action clauses;
 // it never rewrites the transcript, meeting text, or the planner's input.
 private static final Pattern ASR_LAUNCH=Pattern.compile("(?:^|[，。；,;\\n]|然后|接着|最后|接下来|之后|随后)(?:(?:请|麻烦|帮我|给我|请你|我想要|我想|我要|现在|先|帮忙)\\s*)*召开开?\\s*$");
 private static final Pattern NEXT_ACTION=Pattern.compile("^[」”\"』》]?\\s*(?:里|中|上)?\\s*(?:帮我|给我|帮忙)?\\s*(?:"+ACTION+")");
 private static final Pattern ALTERNATIVE=Pattern.compile("\\s*[」”\"』》]?\\s*(?:或者|或是|或|还是|二选一)\\s*(?:打开|使用|用|在|去)?\\s*[「“\"『《]?\\s*");
 private static final Pattern SEARCH_QUESTION=Pattern.compile("(?:搜索|搜一搜|搜一下|查找|查询)\\s*(?:如何|怎么|怎样)");
 private static final Pattern QUERY_BOUNDARY=Pattern.compile("[，。；,;\\n]|然后|接着|最后|随后|接下来|之后|并且|同时|以及|再|并");
 private static boolean searchQuestionObject(String user,int launchStart){
  String before=user.substring(0,launchStart);Matcher question=SEARCH_QUESTION.matcher(before);int end=-1;while(question.find())end=question.end();
  if(end<0)return false;String query=before.substring(end);return query.length()<=120&&!QUERY_BOUNDARY.matcher(query).find();
 }
 static boolean inQuote(String user,int end){char closing=0;for(int i=0;i<end;i++){char c=user.charAt(i);if(closing!=0){if(c==closing)closing=0;continue;}int q="《「“\"『".indexOf(c);if(q>=0)closing="》」”\"』".charAt(q);}return closing!=0;}
 static List<Mention> mentions(String user){return mentions(user,Collections.emptyList());}
 static List<Mention> mentions(String user,Collection<String> selectedNames){
  ArrayList<Mention> found=new ArrayList<>();if(user==null||user.isEmpty())return found;
  List<Mention> all=rawMentions(user,selectedNames);
  ArrayList<StageSourceRules.AppSpan> sharedSpans=new ArrayList<>();for(Mention m:all)sharedSpans.add(new StageSourceRules.AppSpan(m.app,m.start,m.end));
  if(SharedAppSourceRules.detect(user,sharedSpans)!=null)return all;
  for(int i=0;i<all.size();i++){
   Mention item=all.get(i);String before=user.substring(0,item.start);Matcher prefix=PREFIX.matcher(before);boolean explicit=prefix.find();int start=explicit?prefix.start():item.start;
   String after=user.substring(item.end);boolean hasAction=NEXT_ACTION.matcher(after).find();Matcher noisyLaunch=ASR_LAUNCH.matcher(before);boolean asrLaunch=!explicit&&hasAction&&noisyLaunch.find();if(asrLaunch)start=noisyLaunch.start();
   // App names quoted as the object of a search/title are content; quoted app launch labels are instructions.
   if(inQuote(user,start))continue;
   // '搜索如何打开微信' keeps the launch words inside the search question. A later flow boundary ends it.
   if(explicit&&searchQuestionObject(user,start))continue;
   if(i>0&&ALTERNATIVE.matcher(user.substring(all.get(i-1).end,item.start)).matches())continue;
   if(i+1<all.size()&&ALTERNATIVE.matcher(user.substring(item.end,all.get(i+1).start)).matches())continue;
   String immediate=user.substring(Math.max(0,start-32),start);
   if(immediate.matches("(?s).*(?:不要|不用|别|无需|不需要|禁止|不必|不能|不允许|不)(?:\\s|再|去|在|使用|用|帮我|给我)*$"))continue;
   boolean boundary=asrLaunch||before.trim().matches("(?:(?:请|麻烦|帮我|给我|请你|我想要|我想|我要|现在|先|帮忙)\\s*)*")||before.matches("(?s).*[，。；,;\\n]\\s*$")||FLOW.matcher(before).find();
   if(!explicit&&!asrLaunch&&before.matches("(?s).*召开开?\\s*$"))continue;
   // ASR often omits both punctuation and '然后': an exact app name plus an app action is still a new clause.
   boolean asr=!found.isEmpty()&&hasAction;
   if(!explicit&&before.matches("(?s).*(?:搜索|搜一搜|搜一下|查找|查询)\\s*$"))continue;
   if(!explicit&&!((boundary||asr)&&hasAction))continue;
   // '搜索打开微信教程' names a tutorial, whereas '然后打开微信' is a launch instruction.
   String tail=after.replaceFirst("^[」”\"』》]","").trim();if(!hasAction&&tail.matches("^(?:教程|教学|攻略|的视频|介绍|怎么用).*"))continue;
   found.add(new Mention(item.app,start,item.end));
  }
  return found;
 }
 static List<Mention> rawMentions(String user,Collection<String> selectedNames){
  if(user==null||user.isEmpty())return Collections.emptyList();
  Set<String> names=new HashSet<>(Arrays.asList("WPS Office","WPS","哔哩哔哩","B站","淘宝","京东","拼多多","美团外卖","美团","饿了么","大麦"));
  for(String name:selectedNames)if(name!=null&&name.trim().length()>=2&&name.trim().length()<=120)names.add(name.trim());
  ArrayList<String> ordered=new ArrayList<>(names);ordered.sort((a,b)->{int length=Integer.compare(b.length(),a.length());return length==0?a.compareTo(b):length;});ArrayList<String> patterns=new ArrayList<>();for(String name:ordered)patterns.add(Pattern.quote(name));
  Matcher scan=Pattern.compile(String.join("|",patterns),Pattern.CASE_INSENSITIVE).matcher(user);ArrayList<Mention> all=new ArrayList<>();
  while(scan.find()){
   String raw=scan.group();if(asciiWordEdge(user,scan.start(),scan.end(),raw))continue;
   String name=raw;if(name.toLowerCase(Locale.ROOT).startsWith("wps"))name="WPS";if(name.equalsIgnoreCase("b站"))name="哔哩哔哩";
   all.add(new Mention(name,scan.start(),scan.end()));
  }
  return all;
 }
 private static boolean asciiWordEdge(String text,int start,int end,String name){
  return asciiWord(name.charAt(0))&&start>0&&asciiWord(text.charAt(start-1))||asciiWord(name.charAt(name.length()-1))&&end<text.length()&&asciiWord(text.charAt(end));
 }
 private static boolean asciiWord(char ch){return ch>='a'&&ch<='z'||ch>='A'&&ch<='Z'||ch>='0'&&ch<='9'||ch=='_';}
 /** A planner may split one owned document's write/reopen/edit cycle; retain its ownership as one stage. */
 static boolean documentContinuation(String user,String first,String next){
  if(user==null||first==null||next==null||!user.matches("(?s).*(?:新建|创建|起草|生成).{0,24}(?:文档|合同|模板|文件).*"))return false;
  if(!first.matches("(?s).*(?:新建|创建|起草|生成).{0,24}(?:文档|合同|模板|文件).*")||!next.matches("(?s).*(?:修改|编辑|调整|更改|改为|改成|只改).*"))return false;
  // An independent new file, Save As or a second contract must retain its own stage.
  if(next.matches("(?s).*(?:另建|另存为|新建另一|创建另一|再新建|再创建|另外新建|另一个(?:文档|文件|合同)).*"))return false;
  String beginning=next.replaceFirst("^(?:在\\s*)?(?:WPS(?:\\s*Office)?(?:中|里|上)?[，,：:]?\\s*)?","").trim();if(beginning.matches("^(?:新建|创建|起草|生成).*"))return false;
  String name=createdDocumentName(first);if(!name.isEmpty())return normalized(next).contains(normalized(name));
  // Without a named file require an unambiguous reference to this newly-created file, not just the word '合同'.
  return next.matches("(?s).*(?:本次|本轮|刚才|上述|前一步)(?:刚刚|刚|所)?(?:新建|创建|生成|起草)(?:的)?(?:同一份|这份|该)?(?:文档|文件|合同|模板).*" );
 }
 private static String createdDocumentName(String goal){
  Matcher m=Pattern.compile("(?:保存(?:文件名|文件|文档|名称)?为|命名为)\\s*[《「“\"『]?([^\\s，。；,;》」”\"』]{2,100})").matcher(goal);return m.find()?m.group(1):"";
 }
 static Set<String> apps(String user){LinkedHashSet<String> found=new LinkedHashSet<>();for(Mention m:mentions(user))found.add(m.app);return found;}
 /** Resolved package identities are supplied locally; neither a model's app spelling nor set membership establishes order. */
 static String issue(String user,List<Mention> mentions,List<String> identities,List<Stage> stages){
  if(mentions.size()!=identities.size())throw new IllegalArgumentException("Mention identity count");
  int cursor=0,previous=-1,previousLiteralEnd=-1;String previousIdentity="";
  for(int i=0;i<mentions.size();i++){
   Mention mention=mentions.get(i);String identity=identities.get(i);int end=i+1<mentions.size()?mentions.get(i+1).start:user.length();List<String> literals=literals(user.substring(Math.min(mention.end,end),end));
   // A→B→A must revisit A. Adjacent clauses in the same app may share a stage (e.g. create, edit, save one document).
   int from=previous<0?0:identity.equals(previousIdentity)?cursor:cursor+1;boolean appSeen=false;int chosen=-1,literalEnd=-1;
   for(int p=from;p<stages.size();p++){
    Stage stage=stages.get(p);if(!identity.equals(stage.app))continue;appSeen=true;String text=normalized(stage.goal);int position=p==previous?Math.max(0,previousLiteralEnd):0;boolean covered=true;
    for(String literal:literals){int match=text.indexOf(literal,position);if(match<0&&p==previous&&identity.equals(previousIdentity)){
      // Reusing the same named document/target in a later operation is legal; distinct targets retain order.
      int old=text.indexOf(literal);if(old>=0&&old+literal.length()==previousLiteralEnd)match=old;
     }if(match<0){covered=false;break;}position=match+literal.length();}
    if(covered){chosen=p;literalEnd=literals.isEmpty()?-1:position;break;}
   }
   if(chosen<0)return appSeen?"任务计划遗漏或颠倒「"+mention.app+"」中的明确对象，未开始执行，请重新描述":"任务计划遗漏应用或改变先后顺序「"+mention.app+"」，未开始执行，请重新描述";
   cursor=chosen;previous=chosen;previousIdentity=identity;previousLiteralEnd=literalEnd;
  }
  return "";
 }
 /** Only exact quoted action objects are enforced. Free paraphrases are not claimed to be semantically verified. */
 private static List<String> literals(String clause){
  ArrayList<String> result=new ArrayList<>();Matcher verb=Pattern.compile("(?:搜索|搜一搜|搜一下|查找|查一下|查询|播放|新建|创建|命名为|保存为|修改|编辑)").matcher(clause);if(!verb.find())return result;
  Matcher quote=Pattern.compile("《([^》]{1,120})》|「([^」]{1,120})」|“([^”]{1,120})”|\"([^\"]{1,120})\"|『([^』]{1,120})』").matcher(clause);while(quote.find())if(quote.start()>=verb.end())for(int g=1;g<=5;g++)if(quote.group(g)!=null){String value=normalized(quote.group(g));if(!value.isEmpty())result.add(value);break;}return result;
 }
 private static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[\\s《》「」“”\"『』]","");}
}
