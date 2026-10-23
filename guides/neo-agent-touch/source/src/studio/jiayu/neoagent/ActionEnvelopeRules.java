package studio.jiayu.neoagent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Bounded structural diagnostics. Never includes model text, exception messages or unknown keys. */
final class ActionEnvelopeRules {
 private static final Set<String> ACTIONS=new HashSet<>(Arrays.asList("tap","long_press","swipe","type","submit_search","scroll","back","wait","finish","ask","document_write","document_check","document_focus","document_replace","purchase_check","purchase_submit","purchase_pay","observe","skills_list","skills_read"));
 private static final Set<String> FIELDS=new HashSet<>(Arrays.asList("action","id","x","y","x2","y2","text","find","replacement","memory","direction","message","name","arguments","jsonrpc","method","params","tool"));
 private static final Set<String> CODES=new HashSet<>(Arrays.asList("json_syntax","json_structure","envelope_invalid","action_missing","action_type","action_unknown","id_type","id_range","id_required","field_missing","field_type","field_length","coordinate_type","coordinate_range","direction_invalid","skill_unknown","memory_type","memory_length","argument_unknown","argument_type","tool_unknown","envelope_field"));
 static final class Invalid extends Exception {
  final String code,field;
  Invalid(String code,String field){super(feedback(code,field));this.code=CODES.contains(code)?code:"envelope_invalid";this.field=field==null||field.isEmpty()?"":FIELDS.contains(field)?field:"other";}
  String safeFeedback(){return feedback(code,field);}
 }
 private static String feedback(String code,String field){
  String safe=field==null||field.isEmpty()?"":FIELDS.contains(field)?field:"other";
  String label=safe.isEmpty()?"参数":safe;
  if("json_syntax".equals(code))return "JSON无法解析；只输出一个完整JSON对象，不附说明文字。";
  if("json_structure".equals(code)||"envelope_field".equals(code))return "JSON结构或必填字段不符；按当前工具目录输出一个动作对象。";
  if("action_missing".equals(code))return "缺少action；只输出一个当前支持的动作对象。";
  if("action_type".equals(code))return "action必须是文本。";
  if("action_unknown".equals(code)||"tool_unknown".equals(code))return "动作或工具名称不在当前工具目录中；不要创造新动作。";
  if("id_required".equals(code))return "该专用操作必须使用真实节点id，不能用坐标代替。";
  if("id_type".equals(code))return "节点id必须是整数，不能带#、小数、说明或占位符。";
  if("id_range".equals(code))return "节点id须在1至10000范围内，并使用最新观察中的实际编号。";
  if("coordinate_type".equals(code))return "坐标必须是数值，不使用文字、对象或空值。";
  if("coordinate_range".equals(code))return "坐标须为0至1000内的有限数值。";
  if("direction_invalid".equals(code))return "direction只能为up或down。";
  if("skill_unknown".equals(code))return "技能id必须取自当前内置技能列表。";
  if("memory_type".equals(code))return "memory必须是文本。";
  if("memory_length".equals(code))return "memory超过允许长度，请缩短后仅输出一个动作。";
  if("field_missing".equals(code))return "缺少必填字段："+label+"。";
  if("field_type".equals(code)||"argument_type".equals(code))return label+"类型不符，请遵循当前工具参数类型。";
  if("field_length".equals(code))return label+"长度超出本工具允许范围。";
  if("argument_unknown".equals(code))return "包含当前工具未定义的参数；只保留工具目录中的字段。";
  return "候选动作结构无效；按当前工具目录只输出一个JSON对象。";
 }
 static Invalid classify(Throwable failure){
  Throwable current=failure;
  for(int i=0;current!=null&&i<8;i++,current=current.getCause())if(current instanceof Invalid)return (Invalid)current;
  // JSONException alone cannot distinguish invalid syntax from a missing/wrongly typed field.
  current=failure;for(int i=0;current!=null&&i<8;i++,current=current.getCause())if("org.json.JSONException".equals(current.getClass().getName()))return new Invalid("json_structure","");
  return new Invalid("envelope_invalid","");
 }
 static String action(boolean present,Object value)throws Invalid{
  if(!present)throw new Invalid("action_missing","action");
  if(!(value instanceof String))throw new Invalid("action_type","action");
  if(!ACTIONS.contains(value))throw new Invalid("action_unknown","action");
  return (String)value;
 }
 static int id(Object value)throws Invalid{
  if(value instanceof Number){double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<1||n>10000)throw new Invalid("id_range","id");if(n!=Math.rint(n))throw new Invalid("id_type","id");return (int)n;}
  // Preserve the existing legacy decoder compatibility; MCP still requires numeric arguments.
  if(value instanceof String&&((String)value).matches("[1-9][0-9]{0,3}"))return Integer.parseInt((String)value);
  throw new Invalid("id_type","id");
 }
 static String text(boolean present,Object value,String field,int min,int max)throws Invalid{
  if(!present)throw new Invalid("field_missing",field);
  if(!(value instanceof String))throw new Invalid("memory".equals(field)?"memory_type":"field_type",field);
  String text=(String)value;if(text.length()<min||text.length()>max)throw new Invalid("memory".equals(field)?"memory_length":"field_length",field);
  return text;
 }
 static int coordinate(boolean present,Object value,String field)throws Invalid{
  if(!present)throw new Invalid("field_missing",field);
  // Keep Action's prior numeric-string compatibility. The MCP integer schema remains stricter.
  double n;
  if(value instanceof Number)n=((Number)value).doubleValue();
  else if(value instanceof String)try{n=Double.parseDouble((String)value);}catch(NumberFormatException invalid){throw new Invalid("coordinate_type",field);}
  else throw new Invalid("coordinate_type",field);
  if(!Double.isFinite(n)||n<0||n>1000)throw new Invalid("coordinate_range",field);
  return (int)n;
 }
 static String direction(boolean present,Object value)throws Invalid{
  if(!present)return "down";
  if(!(value instanceof String))throw new Invalid("field_type","direction");
  if(!"up".equals(value)&&!"down".equals(value))throw new Invalid("direction_invalid","direction");
  return (String)value;
 }
 static String unfence(String raw)throws Invalid{
  if(raw==null)throw new Invalid("json_syntax","");String value=raw.trim();
  if(value.startsWith("```")){int newline=value.indexOf('\n');if(newline<0)throw new Invalid("json_syntax","");value=value.substring(newline+1);int closing=value.lastIndexOf("```");if(closing>=0){if(!value.substring(closing+3).trim().isEmpty())throw new Invalid("json_syntax","");value=value.substring(0,closing);}}
  return value;
 }
 /** Values are summarized only as a fixed type label, never toString(). */
 static String kind(Object value){
  if(value==null||"org.json.JSONObject$1".equals(value.getClass().getName()))return "null";
  if(value instanceof String)return "string";if(value instanceof Boolean)return "boolean";
  if(value instanceof Number){double n=((Number)value).doubleValue();return !Double.isFinite(n)?"number_nonfinite":n==Math.rint(n)?"integer":"number_fraction";}
  if(value instanceof java.util.Map||"org.json.JSONObject".equals(value.getClass().getName()))return "object";
  if(value instanceof java.util.List||value.getClass().isArray()||"org.json.JSONArray".equals(value.getClass().getName()))return "array";
  return "other";
 }
 static String lengthBucket(int length){return length<=0?"0":length<=64?"1-64":length<=256?"65-256":length<=1024?"257-1024":length<=4096?"1025-4096":length<=16384?"4097-16384":">16384";}
}
