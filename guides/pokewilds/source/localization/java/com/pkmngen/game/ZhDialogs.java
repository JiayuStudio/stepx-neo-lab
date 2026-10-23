package com.pkmngen.game;

import java.awt.Component;
import java.io.IOException;
import javax.swing.JOptionPane;

/** Native macOS dialogs avoid AWT initialization clashing with glfw_async. */
public final class ZhDialogs {
    private ZhDialogs() {}
    public static int showConfirmDialog(Component ignored, Object message, String title, int option) {
        if(!isMac())return JOptionPane.showConfirmDialog(ignored,message,title,option);
        return nativeDialog("--confirm", title, String.valueOf(message));
    }
    public static void showMessageDialog(Component ignored, Object message) {
        if(!isMac()){JOptionPane.showMessageDialog(ignored,message);return;}
        nativeDialog("--message", "PokeWilds 提示", String.valueOf(message));
    }
    public static void showMessageDialog(Component ignored, Object message, String title, int kind) {
        if(!isMac()){JOptionPane.showMessageDialog(ignored,message,title,kind);return;}
        nativeDialog("--message", title, String.valueOf(message));
    }
    private static boolean isMac(){return System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).contains("mac");}
    private static int nativeDialog(String kind, String title, String message) {
        String launcher=System.getProperty("pokewilds.launcher");
        if(launcher==null) {System.err.println(title+": "+message);return 1;}
        try {
            return new ProcessBuilder(launcher,kind,title,message).inheritIO().start().waitFor()==0?0:1;
        } catch(IOException e) {e.printStackTrace();return 1;}
        catch(InterruptedException e) {Thread.currentThread().interrupt();return 1;}
    }
}
