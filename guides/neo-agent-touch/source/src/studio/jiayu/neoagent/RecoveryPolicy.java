package studio.jiayu.neoagent;
final class RecoveryPolicy {
 private int recoveries;
 static boolean personal(String reason){return reason!=null&&reason.matches("(?is).*(验证码|登录|密码|付款|支付|提交订单|协议|条款|授权|权限|账号|隐私|安全设置|删除|发送|评论|点赞|关注|想看).*" );}
 boolean retry(String reason){return !personal(reason)&&recoveries++<2;}
 void progressed(){recoveries=0;}
}
