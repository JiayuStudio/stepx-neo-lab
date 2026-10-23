package studio.jiayu.neoagent;
final class CaptureRules {
 static boolean retryable(int error){return error==1||error==3;}
 static boolean retryTimeout(int failures,long elapsed){return failures==1&&elapsed<12000;}
 static long spacing(long last,long now){return last==0?0:Math.max(0,350-(now-last));}
 static boolean uniform(int[] pixels){
  if(pixels==null||pixels.length==0)return true;
  int first=pixels[0];
  for(int pixel:pixels)for(int shift:new int[]{0,8,16})if(Math.abs(((pixel>>>shift)&255)-((first>>>shift)&255))>3)return false;
  return true;
 }
 static String message(int error){if(error==6)return "当前应用禁止截图，请在手机处理；未绕过系统保护";if(error==2)return "截图权限不可用，请检查系统无障碍状态";if(retryable(error))return "截图暂未就绪，等待后自动重试";return "截图失败（"+error+"），请检查显示状态";}
}
