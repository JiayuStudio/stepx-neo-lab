package studio.jiayu.neoagent;

import java.util.*;

/** The pure validators below are called directly by the Android Action constructor. */
public final class ActionEnvelopeRulesTest {
 static int checks;
 interface Job {void run()throws Exception;}
 static void yes(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 static void invalid(String code,String field,Job job)throws Exception{
  try{job.run();throw new AssertionError("expected "+code);}catch(ActionEnvelopeRules.Invalid failure){
   yes(code.equals(failure.code)&&field.equals(failure.field),"specific structural failure "+code+"/"+field);
   yes(!failure.safeFeedback().isEmpty()&&failure.getMessage().equals(failure.safeFeedback()),"only fixed safe feedback is exposed");
  }
 }
 public static void main(String[] args)throws Exception{
  for(String name:new String[]{"tap","long_press","swipe","type","submit_search","scroll","back","wait","finish","ask","document_write","document_check","document_focus","document_replace","purchase_check","purchase_submit","purchase_pay","observe","skills_list","skills_read"})yes(name.equals(ActionEnvelopeRules.action(true,name)),"known action remains supported");
  invalid("action_missing","action",()->ActionEnvelopeRules.action(false,null));
  for(Object value:new Object[]{null,3,true,new HashMap<>()})invalid("action_type","action",()->ActionEnvelopeRules.action(true,value));
  for(String value:new String[]{"","tap long_press","tap id=6","click","shell","device.tap"})invalid("action_unknown","action",()->ActionEnvelopeRules.action(true,value));
  for(Object value:new Object[]{1,10000,6L,6.0,"6","9999"})yes(ActionEnvelopeRules.id(value)==Integer.parseInt(value.toString().replace(".0","")),"old numeric/legacy-id compatibility");
  for(Object value:new Object[]{0,-1,10001,Double.NaN,Double.POSITIVE_INFINITY})invalid("id_range","id",()->ActionEnvelopeRules.id(value));
  for(Object value:new Object[]{null,true,1.5,"#6","node6","6.0","6\"","0","01","10000",new Object()})invalid("id_type","id",()->ActionEnvelopeRules.id(value));
  yes("".equals(ActionEnvelopeRules.text(true,"","text",0,500)),"empty ordinary type keeps its previous structural contract");
  yes(ActionEnvelopeRules.text(true,"x".repeat(500),"text",0,500).length()==500,"ordinary input limit unchanged");
  invalid("field_missing","text",()->ActionEnvelopeRules.text(false,null,"text",0,500));
  invalid("field_type","text",()->ActionEnvelopeRules.text(true,true,"text",0,500));
  invalid("field_length","text",()->ActionEnvelopeRules.text(true,"x".repeat(501),"text",0,500));
  invalid("field_length","find",()->ActionEnvelopeRules.text(true,"","find",1,2000));
  invalid("memory_type","memory",()->ActionEnvelopeRules.text(true,new Object(),"memory",0,4096));
  invalid("memory_length","memory",()->ActionEnvelopeRules.text(true,"x".repeat(4097),"memory",0,4096));
  yes(ActionEnvelopeRules.coordinate(true,0,"x")==0&&ActionEnvelopeRules.coordinate(true,1000,"y")==1000,"coordinate bounds unchanged");
  yes(ActionEnvelopeRules.coordinate(true,12.8,"x")==12&&ActionEnvelopeRules.coordinate(true,"12.8","x")==12,"Action coordinate compatibility unchanged; MCP integer schema remains separate");
  invalid("field_missing","x",()->ActionEnvelopeRules.coordinate(false,null,"x"));
  for(Object value:new Object[]{null,true,"bad",new Object()})invalid("coordinate_type","x",()->ActionEnvelopeRules.coordinate(true,value,"x"));
  for(Object value:new Object[]{-1,1001,Double.NaN,Double.NEGATIVE_INFINITY,"Infinity"})invalid("coordinate_range","x",()->ActionEnvelopeRules.coordinate(true,value,"x"));
  yes("down".equals(ActionEnvelopeRules.direction(false,null)),"default scroll direction unchanged");
  yes("up".equals(ActionEnvelopeRules.direction(true,"up")),"up supported");
  invalid("field_type","direction",()->ActionEnvelopeRules.direction(true,3));
  invalid("direction_invalid","direction",()->ActionEnvelopeRules.direction(true,"left"));
  String object="{\"action\":\"wait\"}";
  yes(object.equals(ActionEnvelopeRules.unfence("  "+object+"  ")),"ordinary object preparation");
  yes(object.equals(ActionEnvelopeRules.unfence("```json\n"+object+"```")),"fenced compatibility unchanged");
  invalid("json_syntax","",()->ActionEnvelopeRules.unfence("```json\n"+object+"``` another-object"));
  invalid("json_syntax","",()->ActionEnvelopeRules.unfence(null));
  invalid("json_syntax","",()->ActionEnvelopeRules.unfence("```json"));

  String sensitive="private-value-that-must-never-be-reflected";
  ActionEnvelopeRules.Invalid scrubbed=new ActionEnvelopeRules.Invalid(sensitive,sensitive);
  yes("envelope_invalid".equals(scrubbed.code)&&"other".equals(scrubbed.field)&&!scrubbed.getMessage().contains(sensitive),"unknown diagnostic codes and field names are not reflected");
  ActionEnvelopeRules.Invalid typed=new ActionEnvelopeRules.Invalid("id_type","id");
  yes(ActionEnvelopeRules.classify(new Exception(sensitive,typed))==typed,"wrapped typed failure survives without wrapper message");
  ActionEnvelopeRules.Invalid generic=ActionEnvelopeRules.classify(new Exception(sensitive));
  yes("envelope_invalid".equals(generic.code)&&!generic.getMessage().contains(sensitive),"unclassified failures remain generic instead of guessed");
  Object opaque=new Object(){@Override public String toString(){throw new AssertionError("diagnostics must not stringify unknown values");}};
  yes("other".equals(ActionEnvelopeRules.kind(opaque)),"unknown values are summarized without toString");
  yes("string".equals(ActionEnvelopeRules.kind(sensitive))&&"integer".equals(ActionEnvelopeRules.kind(6))&&"number_fraction".equals(ActionEnvelopeRules.kind(1.5))&&"number_nonfinite".equals(ActionEnvelopeRules.kind(Double.NaN)),"only fixed scalar type labels leave classifier");
  yes("object".equals(ActionEnvelopeRules.kind(new HashMap<>()))&&"array".equals(ActionEnvelopeRules.kind(new int[]{1}))&&"array".equals(ActionEnvelopeRules.kind(new ArrayList<>()))&&"null".equals(ActionEnvelopeRules.kind(null)),"container and null summaries have no contents");
  int[] lengths={0,1,64,65,256,257,1024,1025,4096,4097,16384,16385};
  String[] buckets={"0","1-64","1-64","65-256","65-256","257-1024","257-1024","1025-4096","1025-4096","4097-16384","4097-16384",">16384"};
  for(int i=0;i<lengths.length;i++)yes(buckets[i].equals(ActionEnvelopeRules.lengthBucket(lengths[i])),"bounded response length category");
  System.out.println("PASS: ActionEnvelopeRules "+checks+" typed validation, unchanged limits and non-reflecting diagnostics checks");
 }
}
