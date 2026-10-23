package studio.jiayu.neoagent;
/** Product-only identity check. Keeps every original character; permits two reordered Chinese phrases. */
final class ProductTargetRules {
 static boolean matches(String context,String target){if(MutationRules.targetOnPage(context,target))return true;String c=MutationRules.normalized(context),t=MutationRules.normalized(target);if(!t.matches("[\\u4e00-\\u9fff]{6,24}"))return false;for(int split=2;split<=t.length()-2;split++)if(Math.max(split,t.length()-split)>=4&&c.contains(t.substring(0,split))&&c.contains(t.substring(split)))return true;return false;}
}
