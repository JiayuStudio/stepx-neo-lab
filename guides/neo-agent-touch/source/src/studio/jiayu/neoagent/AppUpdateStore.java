package studio.jiayu.neoagent;

import android.content.*;
import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;

/** One private, hash-bound APK. Never resolves a caller-provided filesystem path. */
final class AppUpdateStore {
 static final String PREFS="verified-app-update";
 static File directory(Context c)throws IOException{File d=new File(c.getFilesDir(),"verified-update");if(!d.isDirectory()&&!d.mkdirs())throw new IOException("无法创建更新缓存");return d;}
 static File staged(Context c)throws IOException{return new File(directory(c),"update.apk");}
 static String hash(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("更新已取消");if(n>0)d.update(b,0,n);}}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 static Uri uri(Context c){String token=c.getSharedPreferences(PREFS,0).getString("token","");return token.matches("[0-9a-f-]{36}")?Uri.parse("content://"+c.getPackageName()+".updates/verified/"+token+"/update.apk"):null;}
 static void clear(Context c){Uri old=uri(c);if(old!=null)c.revokeUriPermission(old,Intent.FLAG_GRANT_READ_URI_PERMISSION);c.getSharedPreferences(PREFS,0).edit().clear().commit();}
 static void bind(Context c,UpdateRules.Candidate candidate,String digest)throws IOException{if(!c.getSharedPreferences(PREFS,0).edit().putString("token",UUID.randomUUID().toString()).putString("sha256",digest).putLong("size",candidate.size).putString("version",candidate.version).putLong("versionCode",candidate.versionCode).putString("tag",candidate.tag).putString("name",candidate.name).putString("url",candidate.url).commit())throw new IOException("无法保存更新校验结果");}
 static File verified(Context c,Uri u)throws Exception{Uri expected=uri(c);if(expected==null||!expected.equals(u))throw new FileNotFoundException("更新文件不可用");SharedPreferences p=c.getSharedPreferences(PREFS,0);File f=staged(c);long bytes=p.getLong("size",0);String digest=p.getString("sha256","");if(!UpdateRules.validSize(bytes)||!UpdateRules.digest(digest).equals(digest)||!f.isFile()||f.length()!=bytes||!hash(f).equals(digest))throw new FileNotFoundException("更新文件校验未通过");return f;}
}
