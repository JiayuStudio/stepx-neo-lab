package studio.jiayu.neoagent;
import org.json.*;
/** One encrypted merchant-transaction journal, independent of task stop/restart. */
final class PurchaseLedger {
 final Vault vault;final String key;
 PurchaseLedger(android.content.Context c,boolean synthetic){vault=new Vault(c);key=synthetic?"small-purchase-fixture-v1":"small-purchase-ledger-v1";}
 String raw()throws Exception{return vault.get(key);}
 void raw(String value)throws Exception{vault.put(key,value);}
 PurchaseState load()throws Exception{String text=raw();PurchaseState s=new PurchaseState();if(text.isEmpty())return s;JSONObject o=new JSONObject(text);if(o.getInt("version")!=1)throw new Exception("购买记录版本无法核验");s.state=o.getString("state");s.run=o.getString("run");s.merchant=o.getString("merchant");s.item=o.getString("item");s.delivery=o.getString("delivery");s.order=o.getString("order");s.previousOrder=o.optString("previous_order");s.amount=o.getInt("amount");s.cap=o.getInt("cap");s.armedAt=o.getLong("at");if(!java.util.Arrays.asList(PurchaseState.EMPTY,PurchaseState.SUBMIT_PENDING,PurchaseState.ORDER_VERIFIED,PurchaseState.PAY_PENDING,PurchaseState.PAID).contains(s.state)||!s.state.equals(PurchaseState.EMPTY)&&(s.amount<1||s.amount>500||s.cap<s.amount||s.cap>500||s.run.isEmpty()))throw new Exception("购买记录不完整，未继续提交");return s;}
 void save(PurchaseState s)throws Exception{raw(new JSONObject().put("version",1).put("state",s.state).put("run",s.run).put("merchant",s.merchant).put("item",s.item).put("delivery",s.delivery).put("order",s.order).put("previous_order",s.previousOrder).put("amount",s.amount).put("cap",s.cap).put("at",s.armedAt).toString());}
}
