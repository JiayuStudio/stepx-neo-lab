package studio.jiayu.neoagent;
import org.json.*;
final class Action {
 final String name;final JSONObject data;
 Action(JSONObject o)throws Exception{if(o==null)throw new ActionEnvelopeRules.Invalid("json_structure","");data=o;name=ActionEnvelopeRules.action(o.has("action"),o.opt("action"));
  if(o.has("memory"))ActionEnvelopeRules.text(true,o.opt("memory"),"memory",0,TaskMemory.MAX);
  if(o.has("id")&&!name.equals("skills_read"))o.put("id",ActionEnvelopeRules.id(o.opt("id")));
  if(name.equals("skills_read")){String skill=ActionEnvelopeRules.text(o.has("id"),o.opt("id"),"id",0,Integer.MAX_VALUE);if(!DeviceSkills.known(skill))throw new ActionEnvelopeRules.Invalid("skill_unknown","id");}
  if((name.equals("purchase_submit")||name.equals("purchase_pay"))&&!o.has("id"))throw new ActionEnvelopeRules.Invalid("id_required","id");
  if(name.equals("scroll"))ActionEnvelopeRules.direction(o.has("direction"),o.opt("direction"));
  if(name.equals("tap")||name.equals("long_press")){if(!o.has("id")){coord("x");coord("y");}}
  if(name.equals("swipe")){coord("x");coord("y");coord("x2");coord("y2");}
  if(name.equals("document_replace")){ActionEnvelopeRules.text(o.has("find"),o.opt("find"),"find",1,2000);ActionEnvelopeRules.text(o.has("replacement"),o.opt("replacement"),"replacement",0,2000);}
  if(name.equals("document_write"))ActionEnvelopeRules.text(o.has("text"),o.opt("text"),"text",1,8000);
  if(name.equals("type"))ActionEnvelopeRules.text(o.has("text"),o.opt("text"),"text",0,500);
 }
 int coord(String k)throws Exception{return ActionEnvelopeRules.coordinate(data.has(k),data.opt(k),k);}
 static JSONObject object(String raw)throws ActionEnvelopeRules.Invalid{String json=ActionEnvelopeRules.unfence(raw);Object value;try{JSONTokener tokener=new JSONTokener(json);value=tokener.nextValue();if(tokener.nextClean()!=0)throw new ActionEnvelopeRules.Invalid("json_syntax","");}catch(JSONException syntax){throw new ActionEnvelopeRules.Invalid("json_syntax","");}if(!(value instanceof JSONObject))throw new ActionEnvelopeRules.Invalid("json_structure","");return (JSONObject)value;}
 static Action parse(String s)throws Exception{return new Action(object(s));}
}
