package com.pkmngen.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Optional radial and zoom commands. Existing Android and native input handling stays in place. */
public final class OdinAgent {
    private static final boolean RADIAL = Boolean.getBoolean("controls.radial");
    private static final boolean PROMPTS = Boolean.getBoolean("controls.prompts");
    private static final boolean MAP = Boolean.getBoolean("controls.map");
    private static final boolean ZOOM = Boolean.getBoolean("controls.zoom");
    private static final boolean NATIVE_PIXELS = Boolean.getBoolean("controls.nativePixels");
    private static final File DIR = new File(System.getProperty("odin.controls.dir","/tmp/odin-controls"));
    private static final String[] MOVES={"BUILD","CUT","FLY","SURF","DIG","RIDE","SMASH","TELEPORT","FLASH","CHARM","POWER","REPEL","ATTACK","HEADBUTT","PAINT"};
    private static final AtomicBoolean QUEUED=new AtomicBoolean();
    private static final AtomicReference<Properties> PENDING_STATE=new AtomicReference<Properties>();
    private static Properties lastWrittenState;
    private static long lastStateWrite;
    private static long position=0, wheelId=0, lastHeartbeat=0, requestedWheel=0;
    private static Game heldGame;
    private static boolean savedMove, savedInput, wheelOpen;
    private static float zoomLevel=1.0f, mapZoom=1.0f;
    private static DrawMiniMap zoomMap;
    private static Game cameraGame;
    private static Game pixelGame;
    private static int pixelWidth,pixelHeight;
    private static Game surfGame;
    private static SurfSpriteState surfSprites;
    private static String error="";
    private static long errorUntil=0;
    private static final Set<String> reported=Collections.synchronizedSet(new HashSet<String>());

    public static void premain(String args, Instrumentation instrumentation) {
        DIR.mkdirs();
        Thread thread=new Thread(new Runnable(){public void run(){
            while(true){
                try{
                    try{flushState();}catch(Throwable t){report(t);}
                    if(Gdx.app!=null && QUEUED.compareAndSet(false,true)){
                        Gdx.app.postRunnable(new Runnable(){public void run(){
                            try{tick();}catch(Throwable t){report(t);release();writeFailure(t);}finally{QUEUED.set(false);}
                        }});
                    }
                    Thread.sleep(40);
                }catch(Throwable t){report(t);try{Thread.sleep(250);}catch(InterruptedException e){return;}}
            }
        }},"Odin-controls-IPC");
        thread.setDaemon(true);thread.start();
    }
    private static void report(Throwable t){
        String s=t.getClass().getName()+": "+t.getMessage();
        if(reported.add(s)){System.err.println("[Odin controls] "+s);t.printStackTrace();}
    }
    private static void tick() throws Exception {
        Game g=Game.staticGame;
        if(surfGame!=g){surfGame=g;surfSprites=null;}
        if(surfSprites!=null&&!surfSprites.isActive(g.player)){
            surfSprites.restore(g.player);
            if(g.player!=null)refreshPlayerSprite(g.player);
            surfSprites=null;
        }
        if(heldGame!=null && (heldGame!=g || !ready(g))) release();
        File commands=new File(DIR,"commands");
        if(commands.isFile())try(RandomAccessFile f=new RandomAccessFile(commands,"r")){
            if(f.length()<position)position=0;
            f.seek(position);String s;int n=0;
            while(n++<100 && (s=f.readLine())!=null){
                if(f.getFilePointer()==f.length() && !s.endsWith(";"))break;
                position=f.getFilePointer();
                String[] a=s.replace(";","").split(" ");
                try{command(g,a);}catch(Throwable t){report(t);error="Control action unavailable: "+t.getClass().getSimpleName();errorUntil=System.currentTimeMillis()+5000;release();}
            }
        }
        if((wheelOpen||requestedWheel!=0) && System.currentTimeMillis()-lastHeartbeat>1800)release();
        if(requestedWheel!=0&&!wheelOpen&&canOpen(g)){wheelId=requestedWheel;requestedWheel=0;hold(g);wheelOpen=true;}
        if(ZOOM)updateCameraContext(g);
        if(wheelOpen){clearInput();if(heldGame!=null)heldGame.playerCanMove=false;InputProcessor.acceptInput=false;}
        snapshot(g);
    }
    private static void command(Game g,String[] a){
        if(a.length==0)return;
        String op=a[0];long id=a.length>1?Long.parseLong(a[1]):0;
        if("BEAT".equals(op)){if(wheelOpen&&id==wheelId||id==requestedWheel)lastHeartbeat=System.currentTimeMillis();return;}
        if("RESET".equals(op)){release();return;}
        if(PROMPTS && "START".equals(op)) {
            long age = System.currentTimeMillis() - id;
            if (age >= 0 && age < 1000 && !wheelOpen && requestedWheel == 0) ControllerStart.request(g);
            return;
        }
        if(MAP && "MAP".equals(op)) {
            long age = System.currentTimeMillis() - id;
            if (age >= 0 && age < 1000 && !wheelOpen && requestedWheel == 0) MapShortcut.request(g);
            return;
        }
        if(RADIAL&&"OPEN".equals(op)){
            release();wheelId=id;requestedWheel=id;lastHeartbeat=System.currentTimeMillis();
            if(canOpen(g)){requestedWheel=0;hold(g);wheelOpen=true;}
            return;
        }
        if("CANCEL".equals(op)){if(wheelOpen&&id==wheelId||id==requestedWheel)release();return;}
        if(RADIAL&&"CLOSE".equals(op)){
            if(id==requestedWheel){requestedWheel=0;return;}
            if(!wheelOpen||id!=wheelId)return;
            int selection=a.length>2?Integer.parseInt(a[2]):-1;
            Game target=heldGame;release();
            if(selection>=0&&selection<MOVES.length&&canOpen(target))activate(target,selection);
            return;
        }
        if(ZOOM&&"ZOOM".equals(op)&&ready(g)&&!wheelOpen&&canZoom(g)){
            updateCameraContext(g);
            // Nearest-neighbor art shimmers when a source pixel spans a
            // fractional number of framebuffer pixels. Quantize each step.
            int base=Math.max(1,Math.round(Gdx.graphics.getWidth()/g.cam.viewportWidth));
            if(zoomMap!=null){
                int minimum=Math.max(1,(int)Math.ceil(base*.25f));
                int maximum=base*4;
                int current=Math.round(mapZoom*base);
                int pixels=Math.round(current*(id>0?1.25f:.8f));
                if(pixels==current)pixels+=id>0?1:-1;
                mapZoom=(float)Math.max(minimum,Math.min(maximum,pixels))/base;
            }else{
                int minimum=Math.max(1,Math.min(base-1,(int)Math.ceil(base*.65f)));
                int maximum=Math.max(base,(int)Math.floor(base*1.6f));
                int pixels=Math.round(zoomLevel*base)+(id>0?1:-1);
                zoomLevel=(float)Math.max(minimum,Math.min(maximum,pixels))/base;
            }
            // libGDX zoom is reciprocal to the donor's magnification.
            g.cam.zoom=1.0f/(zoomMap==null?zoomLevel:mapZoom);g.cam.update();return;
        }
    }
    private static boolean ready(Game g){return g!=null&&g.player!=null&&g.map!=null&&g.cam!=null&&g.actionStack!=null&&g.player.pokemon!=null&&g.battle!=null;}
    private static boolean hasMenu(Game g){
        for(Action a:g.actionStack)if(a instanceof Menu && !(a instanceof BridgeContext))return true;
        return false;
    }
    private static boolean canOpen(Game g){
        if(!ready(g)||!g.playerCanMove||!g.player.acceptInput||!InputProcessor.acceptInput||g.battle.drawAction!=null||g.player.isSleeping||hasMenu(g))return false;
        for(Action a:g.actionStack){
            if(a instanceof PlayerMoving||a instanceof PlayerRunning)return false;
            String n=a.getClass().getName();if(n.contains("DisplayText")||n.contains("Outro")||n.contains("Intro")||n.contains("Teleport"))return false;
        }
        return true;
    }
    private static boolean canZoom(Game g){
        if(g.battle.drawAction!=null)return false;
        DrawMiniMap map=null;
        for(Action a:g.actionStack)if(a instanceof DrawMiniMap){map=(DrawMiniMap)a;break;}
        for(Action a:g.actionStack)if(a instanceof Menu && !(a instanceof BridgeContext)){
            // The Start-menu map inserts its own DrawText overlay. Other
            // menus still take priority over shoulder zoom.
            if(map==null||(a!=map&&a!=map.drawText))return false;
        }
        if(map!=null)return !map.disabled;
        return g.playerCanMove && g.player.acceptInput && !"BUILD".equals(g.player.currFieldMove)&&!"DIG".equals(g.player.currFieldMove);
    }
    private static void updateCameraContext(Game g){
        if(!ready(g))return;
        updatePixelViewport(g);
        if(cameraGame!=g){cameraGame=g;zoomMap=null;zoomLevel=1.0f;mapZoom=1.0f;}
        DrawMiniMap current=null;for(Action a:g.actionStack)if(a instanceof DrawMiniMap){current=(DrawMiniMap)a;break;}
        if(current!=zoomMap){zoomMap=current;if(current!=null)mapZoom=1.0f;g.cam.zoom=1.0f/(current==null?zoomLevel:mapZoom);g.cam.update();}
    }
    private static void updatePixelViewport(Game g){
        if(!NATIVE_PIXELS)return;
        int width=Gdx.graphics.getWidth(),height=Gdx.graphics.getHeight();
        if(g==pixelGame&&width==pixelWidth&&height==pixelHeight)return;
        try{
            Field field=Game.class.getDeclaredField("viewport");field.setAccessible(true);
            ScreenViewport viewport=(ScreenViewport)field.get(g);
            // The original Auto canvas is 432 high at 2 source pixels per
            // world pixel. Keep its 216-world-pixel view at native resolution.
            int scale=Math.max(1,Math.round(height/216f));
            viewport.setUnitsPerPixel(1f/scale);g.scale=scale;
            viewport.update(width,height);g.cam.update();
            pixelGame=g;pixelWidth=width;pixelHeight=height;
        }catch(Exception e){throw new IllegalStateException("Cannot set native pixel scale",e);}
    }
    private static int eligible(Game g,String move){
        if(!ready(g))return -1;
        for(int i=0;i<g.player.pokemon.size();i++){
            Pokemon p=g.player.pokemon.get(i);
            if(p!=null&&!p.isEgg&&p.currentStats!=null&&p.currentStats.get("hp")!=null&&p.currentStats.get("hp")>0&&p.hms!=null&&p.hms.contains(move))return i;
        }return -1;
    }
    private static void hold(Game g){heldGame=g;savedMove=g.playerCanMove;savedInput=InputProcessor.acceptInput;g.playerCanMove=false;clearInput();InputProcessor.acceptInput=false;lastHeartbeat=System.currentTimeMillis();}
    private static void release(){
        if(heldGame!=null){heldGame.playerCanMove=savedMove;InputProcessor.acceptInput=savedInput;}
        clearInput();heldGame=null;wheelOpen=false;requestedWheel=0;
    }
    private static void clearInput(){
        try{for(Field f:InputProcessor.class.getDeclaredFields())if(java.lang.reflect.Modifier.isStatic(f.getModifiers())&&f.getType()==boolean.class&&(f.getName().endsWith("Pressed"))){f.setAccessible(true);f.setBoolean(null,false);}}catch(Exception e){report(e);}
    }
    private static void activate(Game g,int selected){
        String move=MOVES[selected];int index=eligible(g,move);
        if(index<0){error="A healthy party Pokemon with "+move+" is required.";errorUntil=System.currentTimeMillis()+3500;return;}
        Pokemon p=g.player.pokemon.get(index);
        // FLASH is passive in upstream 0.8.11: following with a FLASH-capable
        // Pokemon invokes the game's own light/ghost/campfire-aura behavior.
        String nativeMove="FLASH".equals(move)?"FOLLOW":move;
        DrawPokemonMenu.allPokemon=g.player.pokemon;
        DrawPokemonMenu.currIndex=index;DrawPokemonMenu.scrollIndex=0;
        BridgeContext context=new BridgeContext(index);
        DrawPokemonMenu.SelectedMenu menu=new DrawPokemonMenu.SelectedMenu(context,p);
        SurfSpriteState previousSurf=surfSprites;
        SurfSpriteState enteringSurf="SURF".equals(nativeMove)&&
            (previousSurf==null||previousSurf.pokemon!=p)?SurfSpriteState.capture(g.player,p):null;
        Map<String,Sprite> previousSprites=g.player.standingSprites;
        boolean previousMove=g.playerCanMove;
        g.playerCanMove=false;
        Action action;
        try{action=menu.getAction(g,nativeMove,context);}catch(Throwable t){g.playerCanMove=previousMove;throw t;}
        if(action==null){g.playerCanMove=previousMove;error="This game does not expose "+move+".";errorUntil=System.currentTimeMillis()+5000;return;}
        if(previousSurf!=null&&!previousSurf.isActive(g.player)){
            previousSurf.restore(g.player);surfSprites=null;
        }
        if(enteringSurf!=null&&enteringSurf.isActive(g.player))surfSprites=enteringSurf;
        if(previousSprites!=g.player.standingSprites)refreshPlayerSprite(g.player);
        if(g.player.hmPokemon!=null)refreshPokemonSprite(g.player.hmPokemon);
        // Native ExitAfterActions and menu transitions need a non-null return
        // context. It contains no invented game logic and draws no party menu.
        g.insertAction(radialTransition(action,new IdentityHashMap<Action,Action>()));
    }
    private static void refreshPlayerSprite(Player player){
        Sprite sprite=player.standingSprites.get(player.dirFacing);
        if(sprite!=null)player.currSprite=new Sprite(sprite);
    }
    private static void refreshPokemonSprite(Pokemon pokemon){
        Sprite sprite=pokemon.standingSprites.get(pokemon.dirFacing);
        if(sprite!=null)pokemon.currOwSprite=sprite;
    }
    /** Preserve the normal Pokémon frames that the stock SURF swap discards. */
    static final class SurfSpriteState {
        final Pokemon pokemon;
        final Map<String,Sprite> standing,moving,alternate;
        private SurfSpriteState(Pokemon pokemon,Map<String,Sprite> standing,
                                Map<String,Sprite> moving,Map<String,Sprite> alternate){
            this.pokemon=pokemon;this.standing=standing;this.moving=moving;this.alternate=alternate;
        }
        static SurfSpriteState capture(Player player,Pokemon pokemon){
            boolean currentlySwapped=player.hmPokemon==pokemon&&!player.currFieldMove.isEmpty();
            return new SurfSpriteState(pokemon,
                currentlySwapped?player.standingSprites:pokemon.standingSprites,
                currentlySwapped?player.movingSprites:pokemon.movingSprites,
                currentlySwapped?player.altMovingSprites:pokemon.altMovingSprites);
        }
        boolean isActive(Player player){
            return player!=null&&player.hmPokemon==pokemon&&"SURF".equals(player.currFieldMove);
        }
        void restore(Player player){
            boolean onPlayer=player!=null&&player.hmPokemon==pokemon&&!player.currFieldMove.isEmpty();
            if(onPlayer){player.standingSprites=standing;player.movingSprites=moving;player.altMovingSprites=alternate;}
            else{pokemon.standingSprites=standing;pokemon.movingSprites=moving;pokemon.altMovingSprites=alternate;refreshPokemonSprite(pokemon);}
        }
    }
    public static final class BridgeContext extends Menu {
        BridgeContext(int index){super();currIndex=index;disabled=true;}
        @Override public String getCamera(){return "gui";}
        @Override public void step(Game g){
            if(goAway||!disabled){g.actionStack.remove(this);if(!goAway)g.playerCanMove=true;}
        }
    }
    private static void snapshot(Game g) throws Exception {
        Properties p=new Properties();p.setProperty("protocol","1");p.setProperty("ready",Boolean.toString(ready(g)));
        p.setProperty("canOpen",Boolean.toString(RADIAL&&!wheelOpen&&canOpen(g)));
        p.setProperty("wheel",wheelOpen?Long.toString(wheelId):"0");p.setProperty("zoom",Integer.toString(Math.round(zoomLevel*100)));
        p.setProperty("shoulder",ZOOM&&ready(g)&&canZoom(g)?"zoom":"cycle");
        if(System.currentTimeMillis()<errorUntil)p.setProperty("error",error);
        for(int i=0;i<MOVES.length;i++){
            int index=eligible(g,MOVES[i]);p.setProperty("available."+i,Boolean.toString(index>=0));
            if(index>=0){Pokemon mon=g.player.pokemon.get(index);p.setProperty("species."+i,mon.specie.name);p.setProperty("shiny."+i,Boolean.toString(mon.isShiny));}
        }
        PENDING_STATE.set(p);
    }
    private static void writeFailure(Throwable t){Properties p=new Properties();p.setProperty("protocol","1");p.setProperty("ready","false");p.setProperty("error","Game bridge unavailable: "+t.getClass().getSimpleName());PENDING_STATE.set(p);}
    private static void flushState() throws Exception {
        Properties state=PENDING_STATE.getAndSet(null);
        if(state==null)return;
        long now=System.currentTimeMillis();
        if(state.equals(lastWrittenState)&&now>=lastStateWrite&&now-lastStateWrite<500)return;
        Properties payload=new Properties();payload.putAll(state);payload.setProperty("time",Long.toString(now));
        write(payload);
        lastWrittenState=state;lastStateWrite=now;
    }
    private static void write(Properties p)throws Exception{
        File temp=new File(DIR,"state.tmp"),target=new File(DIR,"state");try(FileOutputStream out=new FileOutputStream(temp)){p.store(out,"Odin controls bridge");}
        Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
    }

    private static Action radialTransition(Action action,IdentityHashMap<Action,Action> visited){
        if(action==null)return null;if(visited.containsKey(action))return visited.get(action);
        Action result=action;
        if(action instanceof DrawPokemonMenu.SelectedMenu.ExitAfterActions){DrawPokemonMenu.SelectedMenu.ExitAfterActions a=(DrawPokemonMenu.SelectedMenu.ExitAfterActions)action;result=new RadialExit(a.prevMenu,a.nextAction);}
        else if(action instanceof DrawPokemonMenu.Outro){DrawPokemonMenu.Outro a=(DrawPokemonMenu.Outro)action;result=new ClearOutro(a.prevMenu,a.duration,a.nextAction);}
        visited.put(action,result);result.nextAction=radialTransition(result.nextAction,visited);
        if(result instanceof SplitAction){try{Field f=SplitAction.class.getDeclaredField("nextAction2");f.setAccessible(true);f.set(result,radialTransition((Action)f.get(result),visited));}catch(Exception e){throw new IllegalStateException("Could not adapt radial transition",e);}}
        return result;
    }
    /** Same return/menu timing as stock, without painting the absent party menu black. */
    static final class ClearOutro extends DrawPokemonMenu.Outro {
        ClearOutro(Menu previous,int frames,Action next){super(previous);duration=frames;nextAction=next;bgSprite.setAlpha(0f);}
        @Override public void step(Game g){
            if(prevMenu!=null)prevMenu.step(g);
            duration--;
            // No fullscreen sprite draw at all, independent of the active palette shader.
            if(duration<=0){
                if(prevMenu!=null){g.insertAction(prevMenu);prevMenu.disabled=false;prevMenu.drawArrowWhite=false;}
                g.insertAction(nextAction);g.actionStack.remove(this);
            }
        }
    }
    static final class RadialExit extends DrawPokemonMenu.SelectedMenu.ExitAfterActions {
        RadialExit(Menu previous,Action next){super(previous,next);}
        @Override public void step(Game g){
            if(firstStep){g.insertAction(nextAction);firstStep=false;}
            if(prevMenu!=null)prevMenu.step(g);
            Action a=nextAction;while(a!=null&&!g.actionStack.contains(a))a=a.nextAction;
            if(a==null){
                if(prevMenu!=null)DrawPokemonMenu.lastIndex=prevMenu.currIndex;
                g.actionStack.remove(this);g.insertAction(new ClearOutro(null,34,null));
                g.insertAction(new WaitFrames(g,30,new SetField(g,"playerCanMove",true,null)));
            }
        }
    }
}
