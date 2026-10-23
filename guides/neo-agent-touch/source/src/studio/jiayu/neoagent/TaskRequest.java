package studio.jiayu.neoagent;
/** Original user intent stays separate from the model's rewritten goal. */
final class TaskRequest {
 final String original,target;final boolean checkout,sharedComparison;
 TaskRequest(String pkg,String original,String proposal,boolean proposedCheckout){this(pkg,original,proposal,proposedCheckout,false);}
 /** A locally derived shared comparison only preserves its public search target and denies checkout. */
 TaskRequest(String pkg,String original,String proposal,boolean proposedCheckout,boolean sharedComparison){this.original=original==null?"":original;this.sharedComparison=sharedComparison;target=sharedComparison||AppAccess.category(pkg).equals("购物")?SearchTarget.product(this.original,proposal):SearchTarget.fromUser(this.original);checkout=!sharedComparison&&AppCatalog.isCommerce(pkg)&&(ShoppingIntent.preview(this.original)||PurchasePolicy.grant(this.original,pkg).order);}
}
