package studio.jiayu.neoagent;
/** Original user intent stays separate from the model's rewritten goal. */
final class TaskRequest {
 final String original,target;final boolean checkout;
 TaskRequest(String pkg,String original,String proposal,boolean proposedCheckout){this.original=original==null?"":original;target=AppAccess.category(pkg).equals("购物")?SearchTarget.product(this.original,proposal):SearchTarget.fromUser(this.original);checkout=AppCatalog.isCommerce(pkg)&&(ShoppingIntent.preview(this.original)||PurchasePolicy.grant(this.original,pkg).order);}
}
