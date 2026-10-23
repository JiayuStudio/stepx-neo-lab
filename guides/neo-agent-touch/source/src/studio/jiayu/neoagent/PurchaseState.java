package studio.jiayu.neoagent;
/** Write-ahead state; pending is never retried as though nothing happened. */
final class PurchaseState {
 static final String EMPTY="empty",SUBMIT_PENDING="submit_pending",ORDER_VERIFIED="order_verified",PAY_PENDING="pay_pending",PAID="paid";
 String state=EMPTY,run="",merchant="",item="",delivery="",order="",previousOrder="";int amount,cap;long armedAt;
 boolean pending(){return state.equals(SUBMIT_PENDING)||state.equals(PAY_PENDING);}
 void armSubmit(String newRun,String pkg,PurchasePolicy.Grant grant,PurchaseFacts f,long now){
  if(newRun==null||newRun.isEmpty()||!PurchasePolicy.validOrder(grant,f)||!f.confirmPage||!(state.equals(EMPTY)||state.equals(PAID)&&!run.equals(newRun)))throw new IllegalStateException("订单状态或原生金额未核验，不重复提交");
  previousOrder=order;run=newRun;merchant=pkg;item=f.item;delivery=f.deliveryKey;amount=f.total;cap=grant.cap;order=f.orderKey;armedAt=now;state=SUBMIT_PENDING;
 }
 boolean reconcile(String pkg,PurchaseFacts f,long now){
  if(state.equals(EMPTY)||state.equals(PAID)||!merchant.equals(pkg)||f.total!=amount||f.orderKey.isEmpty()||f.orderKey.equals(previousOrder)||f.unsafe)return false;
  if(state.equals(SUBMIT_PENDING)){
   if(!f.submitted&&!f.awaitingPayment&&!f.paid)return false;
   if(!item.equals(f.item)||f.quantity!=1||f.itemCount!=1)return false;
   if(!order.isEmpty()){if(!order.equals(f.orderKey))return false;}else if(f.orderAt<armedAt-5000||f.orderAt>now+5000)return false;
   order=f.orderKey;state=f.paid?PAID:ORDER_VERIFIED;return true;
  }
  if((state.equals(ORDER_VERIFIED)||state.equals(PAY_PENDING))&&order.equals(f.orderKey)&&f.paid&&!f.awaitingPayment){state=PAID;return true;}return false;
 }
 void armPay(String currentRun,String pkg,PurchasePolicy.Grant grant,PurchaseFacts f,long now){
  if(!state.equals(ORDER_VERIFIED)||!run.equals(currentRun)||!merchant.equals(pkg)||!grant.pay||!grant.order||grant.cap<amount||f.total!=amount||f.total<1||f.total>PurchasePolicy.HARD_CAP_CENTS||!f.orderKey.equals(order)||!f.cashier||f.unsafe)throw new IllegalStateException("付款金额或订单身份未核验，不重复付款");
  armedAt=now;state=PAY_PENDING;
 }
 boolean paidFor(String currentRun){return state.equals(PAID)&&run.equals(currentRun);}
}
