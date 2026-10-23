package local.pokewilds.standalone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/** Edge anchored controls that only consume touches landing on a button. */
public final class TouchControls extends View implements InputManager.InputDeviceListener {
    public interface KeySink { boolean send(int keyCode, int action); }

    public static final int TOUCH_AUTO = 0;
    public static final int TOUCH_ON = 1;
    public static final int TOUCH_OFF = 2;
    public static final String[] TOUCH_MODE_LABELS = {"Auto", "On", "Off"};
    private static final String PREFS = "touch_controls";
    private static final String MODE_KEY = "mode";
    private static final int NONE = -1;
    private static final int TOGGLE = -2;
    private static final int ROTATE = -3;
    private static final int UTILITY = -100;
    private final int[] dpadKeys = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF[] dpad = {new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF aButton = new RectF(), bButton = new RectF(), startButton = new RectF();
    private final RectF l1Button = new RectF(), r1Button = new RectF(), toggleButton = new RectF(), rotateButton = new RectF();
    private final RectF l2Button = new RectF(), r2Button = new RectF(), selectButton = new RectF();
    private final TouchInputState inputState = new TouchInputState(
        (key, down) -> sendKey(key, down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP));
    private final KeySink keySink;
    private final Runnable cancelInput;
    private final Runnable menuAction;
    private final Runnable rotateAction;
    private final java.util.function.Consumer<String> utilityAction;
    private final RectF[] utilityButtons=new RectF[NeoConsoleLayout.UTILITY_LABELS.length];
    private NeoConsoleLayout console;
    private final InputManager inputManager;
    private final Handler inputHandler = new Handler(Looper.getMainLooper());
    private final float density;
    private int touchMode;
    private boolean controllerConnected;
    private boolean controlsHidden;
    private ControlPatchBridge controlPatches;
    private int radialPointer = -1;

    public TouchControls(Context context, KeySink sink, Runnable cancelInput,Runnable menuAction,Runnable rotateAction,java.util.function.Consumer<String> utilityAction) {
        super(context);
        keySink = sink;
        this.cancelInput = cancelInput;
        this.menuAction=menuAction;
        this.rotateAction=rotateAction;this.utilityAction=utilityAction;
        for(int i=0;i<utilityButtons.length;i++)utilityButtons[i]=new RectF();
        density = getResources().getDisplayMetrics().density;
        inputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        touchMode = readTouchMode(context);
        controllerConnected = hasController();
        setWillNotDraw(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public TouchControls(Context context, AttributeSet attrs) { this(context, null, null, null, null, null); }

    public static int readTouchMode(Context context) {
        int mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(MODE_KEY, TOUCH_AUTO);
        return isValidMode(mode) ? mode : TOUCH_AUTO;
    }

    public static void saveTouchMode(Context context, int mode) {
        if (!isValidMode(mode)) mode = TOUCH_AUTO;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(MODE_KEY, mode).apply();
    }

    public static String touchModeLabel(int mode) {
        return isValidMode(mode) ? TOUCH_MODE_LABELS[mode] : TOUCH_MODE_LABELS[TOUCH_AUTO];
    }

    private static boolean isValidMode(int mode) { return mode >= TOUCH_AUTO && mode <= TOUCH_OFF; }

    public void setTouchMode(int mode) {
        releaseAll();
        touchMode = isValidMode(mode) ? mode : TOUCH_AUTO;
        controlsHidden = false;
        refreshControllerState();
        invalidate();
    }

    public int getTouchMode() { return touchMode; }

    void setControlPatches(ControlPatchBridge bridge) { controlPatches = bridge; invalidate(); }

    /** Sends ACTION_UP for every key currently held by a finger. */
    public void releaseAll() {
        if (cancelInput != null) cancelInput.run();
        radialPointer = -1;
        inputState.releaseAll();
    }

    public void refreshControllerState() {
        boolean connected = hasController();
        if (connected != controllerConnected) {
            controllerConnected = connected;
            releaseAll();
            invalidate();
        }
    }

    private boolean isVisibleByMode() { return touchMode == TOUCH_ON || (touchMode == TOUCH_AUTO && !controllerConnected); }
    private boolean shouldDrawControls() { return isVisibleByMode() && !controlsHidden; }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (inputManager != null) inputManager.registerInputDeviceListener(this, inputHandler);
        refreshControllerState();
    }

    @Override protected void onDetachedFromWindow() {
        releaseAll();
        if (inputManager != null) inputManager.unregisterInputDeviceListener(this);
        super.onDetachedFromWindow();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        releaseAll();
        layoutButtons(width, height);
    }

    private float dp(float value) { return value * density; }

    private void set(RectF r,NeoConsoleLayout.Box b){r.set(b.x,b.y,b.x+b.w,b.y+b.h);}
    private RectF rect(NeoConsoleLayout.Box b){return new RectF(b.x,b.y,b.x+b.w,b.y+b.h);}
    private void layoutButtons(int width,int height){
        console=new NeoConsoleLayout(width,height,density);
        NeoConsoleLayout.Box cross=console.dpad;float cell=cross.w/3,x=cross.x,y=cross.y;
        dpad[0].set(x+cell,y,x+2*cell,y+1.45f*cell);
        dpad[1].set(x+cell,y+1.55f*cell,x+2*cell,y+3*cell);
        dpad[2].set(x,y+cell,x+1.45f*cell,y+2*cell);
        dpad[3].set(x+1.55f*cell,y+cell,x+3*cell,y+2*cell);
        set(aButton,console.a);set(bButton,console.b);set(selectButton,console.select);set(startButton,console.start);
        set(l1Button,console.l);set(r1Button,console.r);set(toggleButton,console.menu);set(rotateButton,console.rotate);
        for(int i=0;i<utilityButtons.length;i++)set(utilityButtons[i],console.utilities[i]);l2Button.setEmpty();r2Button.setEmpty();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(console==null||touchMode==TOUCH_OFF)return;
        drawConsole(canvas);
        if(shouldDrawControls()){
            drawCross(canvas);drawAbRecess(canvas);drawRoundKey(canvas,aButton,"A",KeyEvent.KEYCODE_BUTTON_A);
            drawRoundKey(canvas,bButton,"B",KeyEvent.KEYCODE_BUTTON_B);
            drawPill(canvas,selectButton,"SELECT",KeyEvent.KEYCODE_BUTTON_SELECT);
            drawPill(canvas,startButton,"START",KeyEvent.KEYCODE_BUTTON_START);
            if(!console.portrait){drawButton(canvas,l1Button,"L");drawButton(canvas,r1Button,"R");}
        }
        for(int i=0;i<utilityButtons.length;i++)drawUtility(canvas,i);
        if(controlPatches!=null&&controlPatches.wheelShown())drawRadialWheel(canvas);
    }
    private void label(Canvas c,String value,float x,float y,float size,int color){
        textPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));textPaint.setTextSize(size);
        textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setColor(color);c.drawText(value,x,y,textPaint);
    }
    private Path roundedShell(RectF bounds,float normal,float bottomRight){
        Path p=new Path();p.addRoundRect(bounds,new float[]{normal,normal,normal,normal,bottomRight,bottomRight,normal,normal},Path.Direction.CW);return p;
    }
    private void drawConsole(Canvas c){
        float w=getWidth(),h=getHeight(),u=console.unit;
        c.save();c.clipOutRect(rect(console.screen));
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xffdedee2,0xffc4c5ce,0xffb8bac6},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,paint);paint.setShader(null);
        Path body=roundedShell(new RectF(5*u,5*u,w-5*u,h-5*u),console.portrait?14*u:65*u,console.portrait?70*u:65*u);
        paint.setColor(0xff9699a8);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*u);c.drawPath(body,paint);
        paint.setColor(0xffedeef2);paint.setStrokeWidth(u);c.drawLine(20*u,36*u,w-20*u,36*u,paint);paint.setStyle(Paint.Style.FILL);
        RectF bezel=rect(console.bezel);paint.setColor(0xff9395a1);c.drawPath(roundedShell(bezel,14*u,console.portrait?39*u:18*u),paint);
        bezel.inset(2*u,2*u);paint.setColor(0xff444653);c.drawPath(roundedShell(bezel,13*u,console.portrait?37*u:17*u),paint);
        paint.setColor(0xff181b23);RectF edge=rect(console.screen);edge.inset(-3*u,-3*u);c.drawRect(edge,paint);
        if(console.portrait){
            float captionY=console.bezel.y+17*u;
            label(c,"DOT MATRIX WITH STEREO SOUND",w*.57f,captionY,6.7f*u,0xffd4d4df);
            paint.setStrokeWidth(1.4f*u);paint.setColor(0xff6c304c);
            c.drawLine(20*u,captionY-7*u,65*u,captionY-7*u,paint);c.drawLine(w-47*u,captionY-7*u,w-20*u,captionY-7*u,paint);
            paint.setColor(0xff282c3d);c.drawLine(20*u,captionY-3*u,65*u,captionY-3*u,paint);
            c.drawLine(w-47*u,captionY-3*u,w-20*u,captionY-3*u,paint);
            paint.setColor(0xfffa6673);c.drawCircle(24*u,captionY+2*u,2.5f*u,paint);
            label(c,"BATTERY",44*u,captionY+4*u,4.7f*u,0xffd6d7df);
            float brandY=console.bezel.y+console.bezel.h+28*u;
            textPaint.setTextAlign(Paint.Align.LEFT);textPaint.setColor(0xff242b50);
            textPaint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));textPaint.setTextSize(12*u);c.drawText("POKÉWILDS",16*u,brandY,textPaint);
            textPaint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC));textPaint.setTextSize(23*u);c.drawText("GAME BOY",102*u,brandY,textPaint);
            label(c,"NEO",w-24*u,brandY,6*u,0xff434a68);
            // A shallow moulded seam and the large lower corner echo the original shell.
            paint.setColor(0xffa8aab6);paint.setStrokeWidth(u);c.drawLine(11*u,console.utilityTop-5*u,w-11*u,console.utilityTop-5*u,paint);
            label(c,"POWER · ON",50*u,22*u,6*u,0xff626a82);
        }else label(c,"NEO ADVANCE",w/2,console.utilityTop-3*u,6*u,0xff303653);
        c.save();float sx=console.portrait?w*.79f:w*.92f,sy=console.portrait?h*.89f:h*.77f;c.rotate(-18,sx,sy);
        for(int i=0;i<6;i++){
            float left=sx+(i-2.5f)*10*u,top=sy-21*u;
            paint.setColor(0xffececf0);c.drawRoundRect(left-u,top-u,left+5*u,sy+23*u,3*u,3*u,paint);
            paint.setColor(0xff505563);c.drawRoundRect(left,top,left+4*u,sy+21*u,2*u,2*u,paint);
            paint.setColor(0xff858a98);c.drawRoundRect(left+2*u,top+u,left+4*u,sy+20*u,u,u,paint);
        }
        c.restore();c.restore();
    }
    private void drawAbRecess(Canvas c){
        float u=console.unit,dx=aButton.centerX()-bButton.centerX(),dy=aButton.centerY()-bButton.centerY();
        float cx=(aButton.centerX()+bButton.centerX())/2,cy=(aButton.centerY()+bButton.centerY())/2;
        c.save();c.rotate((float)Math.toDegrees(Math.atan2(dy,dx)),cx,cy);
        float half=((float)Math.hypot(dx,dy)+aButton.width()+15*u)/2;
        paint.setColor(0xffb0b2bf);c.drawRoundRect(cx-half,cy-aButton.height()/2-8*u,cx+half,cy+aButton.height()/2+8*u,40*u,40*u,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(u);paint.setColor(0xffe7e7ec);c.drawRoundRect(cx-half,cy-aButton.height()/2-8*u,cx+half,cy+aButton.height()/2+8*u,40*u,40*u,paint);paint.setStyle(Paint.Style.FILL);c.restore();
    }
    private void drawCross(Canvas c){
        NeoConsoleLayout.Box b=console.dpad;float k=b.w/3;
        
        Path cross=new Path();cross.addRect(b.x+k,b.y,b.x+2*k,b.y+b.h,Path.Direction.CW);
        Path horizontal=new Path();horizontal.addRect(b.x,b.y+k,b.x+b.w,b.y+2*k,Path.Direction.CW);cross.op(horizontal,Path.Op.UNION);
        paint.setColor(0xff11151f);c.drawPath(cross,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.4f*console.unit);paint.setColor(0xff515567);c.drawPath(cross,paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<4;i++)if(inputState.isHeld(dpadKeys[i])){paint.setColor(0xff55596c);c.drawRoundRect(dpad[i],3,3,paint);}
        paint.setColor(0xff292e3d);c.drawCircle(b.cx(),b.cy(),k*.28f,paint);
        for(int i=0;i<4;i++)label(c,i==0?"▲":i==1?"▼":i==2?"◀":"▶",dpad[i].centerX(),dpad[i].centerY()+3*console.unit,8*console.unit,0xff414653);
    }
    private void drawRoundKey(Canvas c,RectF r,String name,int key){
        float u=console.unit;RectF ring=new RectF(r);ring.inset(-4*u,-4*u);paint.setColor(0xff9195aa);c.drawOval(ring,paint);
        paint.setColor(0xff161b28);c.drawOval(r,paint);RectF face=new RectF(r);face.inset(2*u,2*u);face.offset(0,inputState.isHeld(key)?2*u:-u);
        paint.setShader(new LinearGradient(face.left,face.top,face.right,face.bottom,inputState.isHeld(key)?0xff703350:0xffad477d,0xff672743,Shader.TileMode.CLAMP));c.drawOval(face,paint);paint.setShader(null);
        label(c,name,r.centerX(),r.bottom+17*u,12*u,0xff262e4e);
    }
    private void drawPill(Canvas c,RectF area,String name,int key){
        float u=console.unit;c.save();c.rotate(-13,area.centerX(),area.centerY());RectF r=new RectF(area);r.inset(6*u,Math.max(0,(area.height()-14*u)/2));
        paint.setColor(0xff85899c);RectF ring=new RectF(r);ring.inset(-3*u,-3*u);c.drawRoundRect(ring,10*u,10*u,paint);
        paint.setColor(inputState.isHeld(key)?0xff53596b:0xff777d91);c.drawRoundRect(r,8*u,8*u,paint);
        paint.setColor(0xffbdc1d4);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(u);c.drawRoundRect(r,8*u,8*u,paint);paint.setStyle(Paint.Style.FILL);
        label(c,name,area.centerX(),area.bottom+8*u,9*u,0xff303952);c.restore();
    }

    private void drawRadialWheel(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * .37f;
        paint.setColor(Color.argb(195, 8, 11, 18));
        canvas.drawCircle(cx, cy, radius * 1.12f, paint);
        for (int i = 0; i < ControlPatchBridge.MOVES.length; i++) {
            double middle = -Math.PI / 2 + i * Math.PI * 2 / ControlPatchBridge.MOVES.length;
            double start = middle - Math.PI / ControlPatchBridge.MOVES.length;
            Path sector = new Path(); sector.moveTo(cx, cy);
            for (int point = 0; point <= 8; point++) {
                double angle = start + 2 * Math.PI / ControlPatchBridge.MOVES.length * point / 8;
                sector.lineTo(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
            }
            sector.close();
            paint.setColor(i == controlPatches.selected() ? Color.rgb(96, 132, 174)
                : i % 2 == 0 ? Color.rgb(38, 43, 53) : Color.rgb(30, 35, 44));
            canvas.drawPath(sector, paint);
            textPaint.setColor(controlPatches.available(i) ? Color.WHITE : Color.GRAY);
            textPaint.setTextSize(dp(10)); textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ControlPatchBridge.MOVES[i], cx + (float) Math.cos(middle) * radius * .70f,
                cy + (float) Math.sin(middle) * radius * .70f, textPaint);
        }
        paint.setColor(Color.rgb(12, 15, 21)); canvas.drawCircle(cx, cy, radius * .28f, paint);
        textPaint.setColor(Color.WHITE); textPaint.setTextSize(dp(12));
        int selected = controlPatches.selected();
        canvas.drawText(selected < 0 ? "SELECT" : ControlPatchBridge.MOVES[selected], cx, cy, textPaint);
    }

    private void drawUtility(Canvas c,int index){
        RectF r=utilityButtons[index];float u=console.unit;
        paint.setColor(0xffb4b7c4);c.drawRoundRect(r,5*u,5*u,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(u);paint.setColor(0xffe8e8ef);c.drawRoundRect(r,5*u,5*u,paint);paint.setStyle(Paint.Style.FILL);
        String text=NeoConsoleLayout.UTILITY_COMMANDS[index].equals("rotate")?(console.portrait?"横屏":"竖屏"):NeoConsoleLayout.UTILITY_LABELS[index];
        textPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));textPaint.setTextSize((console.portrait?9.5f:8.5f)*u);textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setColor(0xff252f4b);
        Paint.FontMetrics f=textPaint.getFontMetrics();c.drawText(text,r.centerX(),r.centerY()-(f.ascent+f.descent)/2,textPaint);
    }
    private void drawButton(Canvas canvas, RectF rect, String label) {
        float u=console.unit;paint.setColor(0xffb2b5c2);canvas.drawRoundRect(rect,7*u,7*u,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(u);paint.setColor(0xffe9e9ef);canvas.drawRoundRect(rect,7*u,7*u,paint);
        paint.setStyle(Paint.Style.FILL);textPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        textPaint.setColor(0xff303952);textPaint.setTextSize((label.length()>1?10:13)*u);textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics=textPaint.getFontMetrics();canvas.drawText(label,rect.centerX(),rect.centerY()-(metrics.ascent+metrics.descent)/2,textPaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if(touchMode==TOUCH_OFF)return false;
        int action = event.getActionMasked();
        if (controlPatches != null && controlPatches.radialEnabled()) {
            int pointer = event.getPointerId(event.getActionIndex());
            if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)
                && shouldDrawControls() && l2Button.contains(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()))) {
                inputState.releaseAll(); radialPointer = pointer;
                inputState.down(pointer, KeyEvent.KEYCODE_BUTTON_L2); invalidate(); return true;
            }
            if (radialPointer == pointer && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                && !controlPatches.wheelShown()) {
                inputState.up(pointer); radialPointer = -1; return true;
            }
            if (controlPatches.wheelShown()) {
                if (action == MotionEvent.ACTION_MOVE) {
                    for (int i = 0; i < event.getPointerCount(); i++)
                        if (event.getPointerId(i) != radialPointer
                            || !l2Button.contains(event.getX(i), event.getY(i)))
                            controlPatches.selectTouch(event.getX(i), event.getY(i), getWidth(), getHeight());
                } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    controlPatches.selectTouch(event.getX(event.getActionIndex()), event.getY(event.getActionIndex()), getWidth(), getHeight());
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                    if (pointer == radialPointer) { radialPointer = -1; inputState.up(pointer); }
                } else if (action == MotionEvent.ACTION_CANCEL) releaseAll();
                invalidate(); return true;
            }
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int index = event.getActionIndex(), pointer = event.getPointerId(index);
            int key = hitKey(event.getX(index), event.getY(index));
            if (key<=UTILITY&&key>UTILITY-utilityButtons.length){releaseAll();if(utilityAction!=null)utilityAction.accept(NeoConsoleLayout.UTILITY_COMMANDS[UTILITY-key]);return true;}
            if (key == ROTATE) { releaseAll(); if(rotateAction!=null)rotateAction.run(); return true; }
            if (key == TOGGLE) { releaseAll(); if(menuAction!=null)menuAction.run(); return true; }
            if (key == NONE || !shouldDrawControls()) return false;
            inputState.down(pointer, key); performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int pointer = event.getPointerId(i);
                int nextKey = shouldDrawControls() ? hitKey(event.getX(i), event.getY(i)) : NONE;
                if (nextKey == TOGGLE || nextKey == ROTATE || nextKey<=UTILITY) nextKey = NONE;
                inputState.move(pointer, nextKey);
            }
            invalidate(); return inputState.hasCapturedPointers();
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            inputState.up(event.getPointerId(event.getActionIndex())); if (action == MotionEvent.ACTION_UP) performClick(); invalidate(); return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) { releaseAll(); return true; }
        return false;
    }

    private int hitKey(float x, float y) {
        for(int i=0;i<utilityButtons.length;i++)if(utilityButtons[i].contains(x,y)){
            String command=NeoConsoleLayout.UTILITY_COMMANDS[i];if(command.equals("l"))return KeyEvent.KEYCODE_BUTTON_L1;if(command.equals("r"))return KeyEvent.KEYCODE_BUTTON_R1;return UTILITY-i;
        }
        if (rotateButton.contains(x, y)) return ROTATE;
        if (toggleButton.contains(x, y)) return TOGGLE;
        for (int i = 0; i < dpad.length; i++) if (dpad[i].contains(x, y)) return dpadKeys[i];
        if (aButton.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_A;
        if (bButton.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_B;
        if (startButton.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_START;
        if (selectButton.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_SELECT;
        if (l1Button.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_L1;
        if (r1Button.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_R1;
        if (l2Button.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_L2;
        if (r2Button.contains(x, y)) return KeyEvent.KEYCODE_BUTTON_R2;
        return NONE;
    }

    private void sendKey(int key, int action) { if (keySink != null) keySink.send(key, action); }

    private boolean hasController() {
        for (int id : InputDevice.getDeviceIds()) { InputDevice device = InputDevice.getDevice(id); if (device == null) continue;
            int sources = device.getSources(); if (TouchInputState.isControllerSource(sources,
                InputDevice.SOURCE_GAMEPAD, InputDevice.SOURCE_JOYSTICK)) return true; }
        return false;
    }

    @Override public void onInputDeviceAdded(int deviceId) { refreshControllerState(); }
    @Override public void onInputDeviceRemoved(int deviceId) { refreshControllerState(); }
    @Override public void onInputDeviceChanged(int deviceId) { refreshControllerState(); }
}
