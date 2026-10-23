package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.pkmngen.game.util.SpriteProxy;
import com.pkmngen.game.util.TextureCache;
import com.google.gson.*;
import java.util.*;
import java.util.regex.*;

/** Display-only localization: serialized IDs and gameplay comparisons stay original. */
public final class ZhText {
    private static Map<String,String> exact;
    private static Map<String,String> terms;
    private static List<Rule> rules;
    private static final Map<String,String> CACHE=new LinkedHashMap<String,String>(512,0.75f,true){protected boolean removeEldestEntry(Map.Entry<String,String> e){return size()>4096;}};
    private static final Pattern SLOT=Pattern.compile("\\{(\\d+)(?:,[^}]*)?}");
    private static final class Rule {
        final Pattern pattern; final String translated; final List<Integer> slots;
        Rule(String original,String translated) {
            this.translated=translated; slots=new ArrayList<>();
            Matcher m=SLOT.matcher(original); StringBuilder regex=new StringBuilder("^"); int end=0;
            while(m.find()) {
                regex.append(Pattern.quote(original.substring(end,m.start()))).append("(.*?)");
                slots.add(Integer.parseInt(m.group(1))); end=m.end();
            }
            regex.append(Pattern.quote(original.substring(end))).append("$");
            pattern=Pattern.compile(regex.toString(),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        }
    }
    private static String normalized(String s) {
        return s.replace("«","'s").replace("»","'t").replace('\u2019','\'')
            .replace('_',' ').trim().replaceAll("\\s+"," ");
    }
    private static String canon(String s) { return normalized(s).toLowerCase(Locale.ROOT); }
    private static synchronized void load() {
        if(exact!=null) return;
        exact=new HashMap<>();terms=new HashMap<>();rules=new ArrayList<>();
        JsonObject data=JsonParser.parseString(Gdx.files.internal("localization/display.json").readString("UTF-8")).getAsJsonObject();
        for(Map.Entry<String,JsonElement> e:data.getAsJsonObject("exact").entrySet()) exact.put(canon(e.getKey()),e.getValue().getAsString());
        for(Map.Entry<String,JsonElement> e:data.getAsJsonObject("terms").entrySet()) terms.put(canon(e.getKey()),e.getValue().getAsString());
        for(JsonElement e:data.getAsJsonArray("patterns")) {
            JsonArray pair=e.getAsJsonArray(); rules.add(new Rule(canon(pair.get(0).getAsString()),pair.get(1).getAsString()));
        }
    }
    public static String translate(String source) {
        if(source==null||source.isEmpty())return source;
        load();synchronized(CACHE){String cached=CACHE.get(source);if(cached!=null)return cached;String translated=translate(source,0).replace("\uE000","").replace("\uE001","");CACHE.put(source,translated);return translated;}
    }
    public static void reportError(String message,Throwable error) {
        System.err.println(message);error.printStackTrace();
        Game.saveErrorLogAndNotifyUser(message,error);
    }
    private static String translate(String source,int depth) {
        if(depth>3)return source;
        if(source.startsWith("\uE000")&&source.endsWith("\uE001"))return source.substring(1,source.length()-1);
        String key=canon(source); String result=exact.get(key);
        if(result==null)result=terms.get(key);
        if(result!=null) return result;
        // Preserve a literal leading/trailing space used around concatenated names.
        for(Rule rule:rules) {
            Matcher m=rule.pattern.matcher(normalized(source));if(!m.matches())continue;
            Map<Integer,String> vals=new HashMap<>();
            for(int i=0;i<rule.slots.size();i++)vals.putIfAbsent(rule.slots.get(i),translate(m.group(i+1),depth+1));
            Matcher slot=SLOT.matcher(rule.translated);StringBuffer out=new StringBuffer();
            while(slot.find()) {
                String v=vals.getOrDefault(Integer.parseInt(slot.group(1)),"");
                if(slot.group().contains(",choice,"))v=(v.equalsIgnoreCase("lower")||v.equals("降低")||v.equals("0"))?"降低":"提高";
                slot.appendReplacement(out,Matcher.quoteReplacement(v));
            }
            slot.appendTail(out);return out.toString();
        }
        return source;
    }
    public static char displayCharAt(String source,int index) {
        String text=translate(source);return index<text.length()?text.charAt(index):' ';
    }
    public static int displayLength(String source) {return translate(source).length();}
    public static char displayFieldCharAt(String source,int index) {return displayCharAt(ZhNames.fieldMoveName(source),index);}
    public static int displayFieldLength(String source) {return displayLength(ZhNames.fieldMoveName(source));}
    public static String dialogue(String source) {
        String text=translate(source);
        if(!text.matches("(?s).*[\\p{IsHan}].*"))return text;
        text=text.replace("\n"," ").replaceAll(" +"," ").trim();
        StringBuilder out=new StringBuilder();
        for(int i=0;i<text.length();i+=17) {
            if(i>0)out.append(' ');out.append(text, i,Math.min(i+17,text.length()));
        }
        return out.toString();
    }
    public static void install(Game game) {
        String chars=Gdx.files.internal("localization/glyphs.txt").readString("UTF-8");
        Texture black=TextureCache.get(Gdx.files.internal("localization/cjk-black.png"));
        Texture white=TextureCache.get(Gdx.files.internal("localization/cjk-white.png"));
        black.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        white.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        for(int i=0;i<chars.length();i++) {
            char c=chars.charAt(i); int x=(i%64)*16,y=(i/64)*16;
            SpriteProxy normal=new SpriteProxy(Color.LIGHT_GRAY,black,x,y,16,16);normal.setSize(8,8);
            SpriteProxy inverse=new SpriteProxy(Color.LIGHT_GRAY,white,x,y,16,16);inverse.setSize(8,8);
            game.textDict.put(c,normal);game.transparentDict.put(c,normal);game.textDictInverse.put(c,inverse);
        }
        System.out.println("Simplified Chinese glyphs installed: "+chars.length());
        ZhReferenceFont.install(game);
    }
}
