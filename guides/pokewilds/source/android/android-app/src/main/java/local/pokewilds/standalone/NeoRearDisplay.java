package local.pokewilds.standalone;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.io.IOException;
import java.lang.reflect.Method;

/** Owns only this app's custom frame and restores its persisted display mode. */
final class NeoRearDisplay {
    private final Object device;
    private final SharedPreferences prefs;
    private final Method frame,clear,getMode,setMode;
    NeoRearDisplay(Context context)throws Exception {
        prefs=context.getSharedPreferences("neo-pet",0);
        device=context.getSystemService("display_ext");
        if(device==null)throw new IOException("此设备没有 Neo 背屏接口");
        Class<?> c=device.getClass();
        frame=c.getMethod("setBackScreenAgentFrame",int[].class,Bundle.class);
        clear=c.getMethod("clearBackScreenAgentFrame");getMode=c.getMethod("getBackScreenDisplayMode");
        setMode=c.getMethod("setBackScreenDisplayMode",int.class);
    }
    synchronized void acquire()throws Exception {
        restore();
        if(!(Boolean)device.getClass().getMethod("isBackScreenEnabled").invoke(device))
            throw new IOException("请先在系统设置中开启背屏");
        int mode=(Integer)getMode.invoke(device);
        if(!prefs.edit().putInt("originalMode",mode).putBoolean("restore",true).commit())
            throw new IOException("无法保存背屏恢复信息");
        setMode.invoke(device,1);
    }
    synchronized void send(int[] pixels)throws Exception {frame.invoke(device,pixels,null);}
    synchronized void restore()throws Exception {
        if(!prefs.getBoolean("restore",false))return;
        clear.invoke(device);
        if((Integer)getMode.invoke(device)==1)setMode.invoke(device,prefs.getInt("originalMode",0));
        prefs.edit().putBoolean("restore",false).commit();
    }
    static void recover(Context context){
        if(NeoPetService.running||!context.getSharedPreferences("neo-pet",0).getBoolean("restore",false))return;
        try{new NeoRearDisplay(context).restore();NeoPetService.status="已恢复系统背屏";}
        catch(Exception e){NeoPetService.status="背屏恢复待重试："+reason(e);}
    }
    static String reason(Throwable e){
        while(e.getCause()!=null)e=e.getCause();
        String m=e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;
    }
}
