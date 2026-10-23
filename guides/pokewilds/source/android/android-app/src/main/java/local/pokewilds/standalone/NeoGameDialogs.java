package local.pokewilds.standalone;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** AWT confirmation text is presented by Android, with safe cancel values for each operation. */
final class NeoGameDialogs {
 private final Activity activity;
 private final Runnable releaseInput;
 private final Path request,response;
 private AlertDialog dialog;
 private String activeId,lastAnswered;
 private int cancelResult=1;
 NeoGameDialogs(Activity activity,Runnable releaseInput){this.activity=activity;this.releaseInput=releaseInput;Path game=activity.getFilesDir().toPath().resolve("game");request=game.resolve("neo-dialog.request");response=game.resolve("neo-dialog.response");}
 boolean isShowing(){return dialog!=null;}
 void poll(){
  if(dialog!=null||activity.isFinishing()||!Files.isRegularFile(request))return;
  try{
   if(Files.size(request)>32768)return;
   Properties p=new Properties();try(Reader in=Files.newBufferedReader(request,StandardCharsets.UTF_8)){p.load(in);}
   String id=p.getProperty("id","");if(!id.matches("[a-zA-Z0-9-]{36}")||id.equals(lastAnswered))return;
   String message=p.getProperty("message","");if(message.length()>8192)return;
   activeId=id;releaseInput.run();
   boolean confirm="confirm".equals(p.getProperty("kind"));boolean overwrite=confirm&&(message.contains("同名存档")||message.contains("same name"));boolean save=confirm&&(message.contains("保存游戏进度")||message.contains("Save your progress"));
   cancelResult=save?2:1;
   String title=overwrite?"同名存档已存在":save?"保存并退出":p.getProperty("title","游戏提示");
   if(title.length()>80)title="游戏提示";
   String shown=overwrite?"已有同名世界。覆盖会替换原来的进度。\n\n可以返回修改世界名称，保留原存档。":message;
   AlertDialog.Builder b=new AlertDialog.Builder(activity).setTitle(title).setMessage(shown);
   if(!confirm)b.setPositiveButton("知道了",(d,w)->answer(0));
   else if(overwrite)b.setPositiveButton("覆盖并生成",(d,w)->answer(0)).setNegativeButton("返回修改名称",(d,w)->answer(1));
   else if(save)b.setPositiveButton("保存并退出",(d,w)->answer(0)).setNegativeButton("不保存退出",(d,w)->answer(1)).setNeutralButton("继续游戏",(d,w)->answer(2));
   else b.setPositiveButton("确定",(d,w)->answer(0)).setNegativeButton("取消",(d,w)->answer(1));
   dialog=b.create();dialog.setOnCancelListener(d->answer(cancelResult));dialog.setOnDismissListener(d->{if(activeId!=null)answer(cancelResult);dialog=null;});dialog.show();
  }catch(IOException|RuntimeException e){android.util.Log.w("PokeWildsNeoDialog","Unable to present game confirmation",e);}
 }
 private void answer(int result){
  if(activeId==null)return;
  Properties p=new Properties();p.setProperty("id",activeId);p.setProperty("result",Integer.toString(result));
  try{Path tmp=response.resolveSibling("neo-dialog.answer.tmp");try(Writer out=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){p.store(out,"Native game answer");}Files.move(tmp,response,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);lastAnswered=activeId;activeId=null;}
  catch(IOException e){android.util.Log.w("PokeWildsNeoDialog","Unable to return game answer",e);}
 }
 boolean dispatch(KeyEvent event){
  if(dialog==null)return false;
  if(event.getKeyCode()==KeyEvent.KEYCODE_BUTTON_A){if(event.getAction()==KeyEvent.ACTION_UP)dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();return true;}
  if(event.getKeyCode()==KeyEvent.KEYCODE_BUTTON_B){if(event.getAction()==KeyEvent.ACTION_UP)dialog.cancel();return true;}
  return dialog.dispatchKeyEvent(event);
 }
 void close(){if(dialog!=null){answer(cancelResult);dialog.dismiss();dialog=null;}}
}
