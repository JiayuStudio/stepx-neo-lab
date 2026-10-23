package com.pkmngen.game;

import java.awt.Component;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

/** Android host dialog bridge; the desktop game still decides whether to save or generate. */
public final class ZhDialogs {
 private static final Path REQUEST=Paths.get("neo-dialog.request"),RESPONSE=Paths.get("neo-dialog.response");
 private ZhDialogs(){}
 public static int showConfirmDialog(Component parent,Object message,String title,int option){return request("confirm",title,String.valueOf(message),option);}
 public static void showMessageDialog(Component parent,Object message){request("message","游戏提示",String.valueOf(message),-1);}
 public static void showMessageDialog(Component parent,Object message,String title,int kind){request("message",title,String.valueOf(message),kind);}
 public static boolean closeRequested(){
  if(Files.isRegularFile(Paths.get("neo-load.request"))){
   try{if(Game.staticGame.isMapLoaded())NeoBridge.saveForReload(Game.staticGame);return true;}
   catch(Exception e){try{Files.deleteIfExists(Paths.get("neo-load.request"));}catch(IOException ignored){}showMessageDialog(null,"当前进度保存失败，未切换世界。请保留游戏并重试。");return false;}
  }
  if(PkmnMap.PeriodicSave.isSaveOld()&&Game.staticGame.isMapLoaded()){
   int choice=showConfirmDialog(null,"要保存游戏进度吗？","保存并退出",0);
   if(choice==2)return false;
   if(choice==0)Game.staticGame.saveGame();
  }
  return true;
 }
 private static synchronized int request(String kind,String title,String message,int option){
  String id=UUID.randomUUID().toString();Properties p=new Properties();p.setProperty("id",id);p.setProperty("kind",kind);p.setProperty("title",title==null?"游戏提示":title);p.setProperty("message",message);p.setProperty("option",Integer.toString(option));
  try{
   Files.deleteIfExists(RESPONSE);Path tmp=Paths.get("neo-dialog.tmp");
   try(Writer out=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){p.store(out,"Native game dialog");}
   Files.move(tmp,REQUEST,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
   for(int i=0;i<6000;i++){
    if(Files.isRegularFile(RESPONSE)){
     Properties answer=new Properties();try(Reader in=Files.newBufferedReader(RESPONSE,StandardCharsets.UTF_8)){answer.load(in);}
     if(id.equals(answer.getProperty("id"))){int result=Integer.parseInt(answer.getProperty("result","1"));Files.deleteIfExists(REQUEST);Files.deleteIfExists(RESPONSE);return result;}
    }
    Thread.sleep(100);
   }
  }catch(InterruptedException e){Thread.currentThread().interrupt();}
  catch(Exception e){System.err.println("Neo dialog unavailable: "+e.getClass().getSimpleName());}
  try{Files.deleteIfExists(REQUEST);}catch(IOException ignored){}
  return message.contains("保存游戏进度")?2:1;
 }
}
