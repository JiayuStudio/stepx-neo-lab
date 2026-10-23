package local.pokewilds.standalone;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

/** The upstream display/input implementation, hosted inside our application. */
public final class GameActivity extends com.termux.x11.MainActivity implements android.hardware.input.InputManager.InputDeviceListener {
    private android.hardware.input.InputManager controllerInputManager;
    private TouchControls touchControls;
    private ControlPatchBridge controlPatches;
    private boolean pixelAlignedZoom;
    private android.app.AlertDialog gameMenu;
    private GameKeyBindings keyBindings = GameKeyBindings.defaults();
    private int keyboardShortcut;
    private boolean rightTriggerHeld;
    private boolean rightTriggerKeyHeld;
    private boolean rightTriggerAxisHeld;
    private boolean rightTriggerKeySeen;
    private static final int TOUCH_DEVICE = -100;
    private final ControllerButtonState controllerButtons = new ControllerButtonState(this::sendControllerKey);

    private void sendControllerKey(int key, boolean down) { sendXKey(key, down); }
    private final android.os.Handler sessionHandler = new android.os.Handler();
    private final Runnable checkSession = new Runnable() {
        public void run() {
            if (!RuntimeService.active) {
                if(waitForNeoReload()){sessionHandler.postDelayed(this,250);return;}
                RuntimeService.surfaceReady = false; finish(); return;
            }
            RuntimeService.displayFocused=hasWindowFocus();
            if(nativeDialogs!=null)nativeDialogs.poll();
            updateAutoViewport();
            RuntimeService.surfaceReady = getLorieView() != null
                && getLorieView().connected() && getLorieView().getWidth() > 0;
            sessionHandler.postDelayed(this, 500);
        }
    };
    private int viewportWidth, viewportHeight;
    private long lastNeoBack;
    private android.window.OnBackInvokedCallback neoBack;
    private NeoGameDialogs nativeDialogs;
    private boolean neoReloadPending;
    private long neoReloadStarted;
    private void updateAutoViewport() {
        if (getLorieView() == null) return;
        RuntimeOptions options = RuntimeOptions.read(this);
        android.graphics.Rect area = getLorieView().getAvailableRect();
        if (area.width() <= 0 || area.height() <= 0) return;
        int[] size;
        if (!options.autoViewport) size = new int[]{options.width, options.height};
        else if (pixelAlignedZoom && getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE) size = ViewportSize.autoZoom(area.width(), area.height());
        else size = ViewportSize.auto(area.width(), area.height());
        if (size[0] == viewportWidth && size[1] == viewportHeight) return;
        viewportWidth = size[0]; viewportHeight = size[1];
        RuntimeOptions.applyDisplaySize(this, viewportWidth, viewportHeight);
        getLorieView().triggerCallback();
        RuntimeService.requestedViewport = size;
    }
    @Override public void onResume() {
        super.onResume();
        keyBindings = GameKeyBindings.read(this);
        keyboardShortcut = KeyboardShortcut.read(this);
        directionKeys[0] = keyBindings.left; directionKeys[1] = keyBindings.right;
        directionKeys[2] = keyBindings.up; directionKeys[3] = keyBindings.down;
        if (touchControls != null) {
            touchControls.setTouchMode(TouchControls.readTouchMode(this));
        }
        if (controlPatches != null) controlPatches.resume();
        sessionHandler.post(checkSession);
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        RuntimeService.displayFocused = focused;
        if (!focused) {
            controllerButtons.cancelAll();
            if (touchControls != null) touchControls.releaseAll();
        }
        if (controlPatches != null) {
            if (focused) controlPatches.resume(); else controlPatches.suspend();
        }
    }
    @Override public void onPause() {
        sessionHandler.removeCallbacks(checkSession);
        RuntimeService.displayFocused = false;
        RuntimeService.surfaceReady = false;
        controllerButtons.cancelAll();
        if (controlPatches != null) controlPatches.suspend();
        if (touchControls != null) touchControls.releaseAll();
        rightTriggerHeld = false;
        rightTriggerKeyHeld = false;
        rightTriggerAxisHeld = false;
        rightTriggerKeySeen = false;
        releaseGamepadKeys();
        try { new java.io.FileOutputStream(new java.io.File(getFilesDir(), "game/neo-save.request")).close(); } catch (java.io.IOException ignored) {}
        super.onPause();
    }
    @Override public void onInputDeviceAdded(int deviceId) { }
    @Override public void onInputDeviceChanged(int deviceId) { onInputDeviceRemoved(deviceId); }
    @Override public void onInputDeviceRemoved(int deviceId) {
        controllerButtons.cancelOwner("device-" + deviceId);
        directionsByDevice.remove(deviceId);
        rightTriggerHeld = rightTriggerKeyHeld = rightTriggerAxisHeld = rightTriggerKeySeen = false;
    }
    @Override public void onDestroy() {
        if(nativeDialogs!=null)nativeDialogs.close();
        if(android.os.Build.VERSION.SDK_INT>=33&&neoBack!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(neoBack);
        if (controllerInputManager != null) controllerInputManager.unregisterInputDeviceListener(this);
        controllerButtons.cancelAll();
        super.onDestroy();
    }
    private void releaseDirections() {
        directionsByDevice.clear();
    }
    private void releaseGamepadKeys() {
        controllerButtons.cancelAll();
        releaseDirections();
        // Physical D-pad keys can have reached X11 without passing through the
        // analog direction tracker. The dialog receives their eventual key-up.
        for (int key : directionKeys) sendXKey(key, false);
        for (int key : new int[]{keyBindings.a, keyBindings.b, keyBindings.start,
            keyBindings.shoulderLeft, keyBindings.shoulderRight}) sendXKey(key, false);
    }
    @Override protected void onCreate(Bundle state) {
        // Taps must click at the touched screen position so the unchanged
        // Swing save/quit dialog remains usable without a visible X cursor.
        ((com.termux.x11.LorieApp) getApplication()).getPrefs(this).touchMode.put("2");
        int savedOrientation=getSharedPreferences("neo_console",MODE_PRIVATE).getInt("orientation",android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        syncNeoOrientation(savedOrientation);
        super.onCreate(state);
        nativeDialogs=new NeoGameDialogs(this,()->{if(touchControls!=null)touchControls.releaseAll();releaseGamepadKeys();});
        if(android.os.Build.VERSION.SDK_INT>=33){neoBack=this::onBackPressed;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,neoBack);}
        controllerInputManager = (android.hardware.input.InputManager) getSystemService(INPUT_SERVICE);
        controllerInputManager.registerInputDeviceListener(this, sessionHandler);
        if (getLorieView() != null)
            getLorieView().setOnKeyListener((view, code, event) -> handleKey(event));
        android.view.View exit = findViewById(com.termux.x11.R.id.exit_button);
        if (exit != null) exit.setOnClickListener(v -> onBackPressed());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        android.view.View content = findViewById(android.R.id.content);
        content.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,orr,ob)->layoutNeoScreen(r-l,b-t));
        content.post(()->layoutNeoScreen(content.getWidth(),content.getHeight()));
        PatchOptions patches = PatchOptions.read(this);
        pixelAlignedZoom = patches.zoom;
        if (content instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) content).setMotionEventSplittingEnabled(true);
            touchControls = new TouchControls(this, this::dispatchTouchKey,
                () -> controllerButtons.cancelOwner("touch"), this::onBackPressed, this::toggleNeoOrientation, this::runNeoUtility);
            if (patches.controlsEnabled()) {
                controlPatches = new ControlPatchBridge(this, patches.radial, patches.zoom, patches.map, patches.prompts,
                    () -> { if (touchControls != null) touchControls.invalidate(); });
                touchControls.setControlPatches(controlPatches);
            }
            ((android.view.ViewGroup) content).addView(touchControls, new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }
    private void layoutNeoScreen(int w,int h) {
        android.view.View frame=findViewById(com.termux.x11.R.id.frame);
        if(frame==null||w<=0||h<=0)return;
        NeoConsoleLayout layout=new NeoConsoleLayout(w,h,getResources().getDisplayMetrics().density);
        boolean useShell=TouchControls.readTouchMode(this)!=TouchControls.TOUCH_OFF;
        NeoConsoleLayout.Box box=useShell?layout.screen:new NeoConsoleLayout.Box(0,0,w,h);
        android.widget.FrameLayout.LayoutParams p=(android.widget.FrameLayout.LayoutParams)frame.getLayoutParams();
        int width=Math.round(box.w),height=Math.round(box.h),left=Math.round(box.x),top=Math.round(box.y);
        if(p.width==width&&p.height==height&&p.leftMargin==left&&p.topMargin==top)return;
        p.width=width;p.height=height;p.leftMargin=left;p.rightMargin=0;p.topMargin=top;p.bottomMargin=0;
        p.gravity=android.view.Gravity.TOP|android.view.Gravity.LEFT;frame.setLayoutParams(p);
    }

    private void runNeoUtility(String command){
        if(command.startsWith("move:")){chooseFieldMove(Integer.parseInt(command.substring(5)));return;}
        switch(command){
            case "save":try{new java.io.FileOutputStream(new java.io.File(getFilesDir(),"game/neo-save.request")).close();android.widget.Toast.makeText(this,"已请求保存，将在安全状态完成",0).show();}catch(java.io.IOException e){android.widget.Toast.makeText(this,"暂时无法请求保存",0).show();}break;
            case "settings":showNeoMenu();break;
            case "skills":showFieldMoves();break;
            case "load":showNeoLoad();break;
            case "pet":startActivity(new Intent(this,NeoPetActivity.class));break;
            case "keyboard":toggleKeyboard();break;
            case "saves":startActivity(new Intent(this,LauncherActivity.class).putExtra(LauncherActivity.MANAGE_SAVES_EXTRA,true));break;
            case "quit":startService(new Intent(this,RuntimeService.class).setAction(RuntimeService.QUIT));break;
            case "rotate":toggleNeoOrientation();break;
            case "auto":applyNeoOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);android.widget.Toast.makeText(this,"已跟随系统方向",0).show();break;
        }
    }
    private void chooseFieldMove(int index){
        if(controlPatches==null||!controlPatches.radialEnabled()){android.widget.Toast.makeText(this,"请在设置中开启野外技能",0).show();return;}
        controlPatches.wheel("neo-direct",true);
        if(controlPatches.wheelShown()){controlPatches.chooseFromMenu(index);controlPatches.wheel("neo-direct",false);}
        else android.widget.Toast.makeText(this,"当前状态无法使用这项技能",0).show();
    }
    private void showNeoLoad(){
        if(gameMenu!=null)return;
        java.io.File directory=new java.io.File(getFilesDir(),"game");java.util.Properties state=new java.util.Properties();
        try(java.io.Reader in=new java.io.InputStreamReader(new java.io.FileInputStream(new java.io.File(directory,"neo-state.properties")),java.nio.charset.StandardCharsets.UTF_8)){state.load(in);}catch(java.io.IOException ignored){}
        boolean loaded=!"0".equals(state.getProperty("count","0"));
        if(loaded&&!"explore".equals(state.getProperty("activity"))){android.widget.Toast.makeText(this,"请先结束战斗或关闭游戏内菜单，再读取存档",0).show();return;}
        java.io.File[] files=directory.listFiles(f->f.getName().endsWith(".sav")&&java.nio.file.Files.isDirectory(f.toPath(),java.nio.file.LinkOption.NOFOLLOW_LINKS));
        if(files==null||files.length==0){android.widget.Toast.makeText(this,"还没有可读取的世界存档",0).show();return;}
        java.util.Arrays.sort(files,java.util.Comparator.comparing(java.io.File::getName));String[] names=new String[files.length];for(int i=0;i<files.length;i++)names[i]=files[i].getName().substring(0,files[i].getName().length()-4);
        if(touchControls!=null)touchControls.releaseAll();releaseGamepadKeys();
        gameMenu=new android.app.AlertDialog.Builder(this).setTitle(loaded?"读取存档 · 先保存当前世界":"读取存档").setItems(names,(d,index)->{
            try{
                java.util.Properties request=new java.util.Properties();request.setProperty("id",java.util.UUID.randomUUID().toString());request.setProperty("file",files[index].getName());
                java.nio.file.Path temp=directory.toPath().resolve("neo-load.tmp");try(java.io.Writer out=java.nio.file.Files.newBufferedWriter(temp,java.nio.charset.StandardCharsets.UTF_8)){request.store(out,"Load saved world");}
                java.nio.file.Files.move(temp,directory.toPath().resolve("neo-load.request"),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                if(loaded){neoReloadPending=true;neoReloadStarted=android.os.SystemClock.uptimeMillis();startService(new Intent(this,RuntimeService.class).setAction(RuntimeService.QUIT));}
                android.widget.Toast.makeText(this,"正在读取 "+names[index],0).show();
            }catch(java.io.IOException e){android.widget.Toast.makeText(this,"无法读取这个存档",0).show();}
        }).setNegativeButton("取消",null).create();gameMenu.setOnDismissListener(d->gameMenu=null);gameMenu.show();
    }
    private boolean waitForNeoReload(){
        if(!neoReloadPending)return false;
        if(android.os.SystemClock.uptimeMillis()-neoReloadStarted>45000){neoReloadPending=false;android.widget.Toast.makeText(this,"重启读档超时，可从开始页面继续",0).show();return false;}
        if(RuntimeService.instance==null){neoReloadPending=false;viewportWidth=viewportHeight=0;startService(new Intent(this,RuntimeService.class));}
        return true;
    }
    private void toggleNeoOrientation(){
        boolean portrait=getResources().getConfiguration().orientation!=android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        applyNeoOrientation(portrait?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    private void applyNeoOrientation(int orientation){
        if(touchControls!=null)touchControls.releaseAll();releaseGamepadKeys();
        getSharedPreferences("neo_console",MODE_PRIVATE).edit().putInt("orientation",orientation).apply();
        // The upstream window manager also reads this preference on configuration changes.
        syncNeoOrientation(orientation);
        // configChanges keeps this Activity, the X11 surface host and native game session alive.
        setRequestedOrientation(orientation);
    }
    private void syncNeoOrientation(int orientation){
        String name=orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?"landscape":
            orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?"portrait":"auto";
        ((com.termux.x11.LorieApp)getApplication()).getPrefs(this).forceOrientation.put(name);
    }
    private void toggleKeyboard() {
        if (getLorieView() != null) getLorieView().toggleKeyboardVisible();
    }
    private boolean dispatchTouchKey(int keyCode, int action) {
        if (gameMenu != null) return true;
        routeControllerButton(controllerEvent(TOUCH_DEVICE, keyCode, action), true);
        return true;
    }

    private static android.view.KeyEvent controllerEvent(int device, int key, int action) {
        long now = android.os.SystemClock.uptimeMillis();
        return new android.view.KeyEvent(now, now, action, key, 0, 0, device, 0, 0,
            android.view.InputDevice.SOURCE_GAMEPAD);
    }

    private boolean routeControllerButton(android.view.KeyEvent event, boolean touch) {
        if (!TouchInputState.isControllerSource(event.getSource(), android.view.InputDevice.SOURCE_GAMEPAD,
                android.view.InputDevice.SOURCE_JOYSTICK)) return false;
        if (event.getAction() != android.view.KeyEvent.ACTION_DOWN && event.getAction() != android.view.KeyEvent.ACTION_UP)
            return false;
        String owner = touch ? "touch" : "device-" + event.getDeviceId();
        return controllerButtons.route(owner, event.getKeyCode(),
            event.getAction() == android.view.KeyEvent.ACTION_DOWN, event.getRepeatCount(), event.isCanceled(),
            () -> pressControllerButton(new android.view.KeyEvent(event), touch));
    }

    private ControllerButtonState.Release pressControllerButton(android.view.KeyEvent down, boolean touch) {
        int key = down.getKeyCode();
        int shortcut = touch ? KeyboardShortcut.touchChoice(keyboardShortcut) : keyboardShortcut;
        if (KeyboardShortcut.matchesKey(shortcut, key)) {
            if (touch) toggleKeyboard(); else handleKeyboardShortcut(down);
            return canceled -> { if (!touch) handleKeyboardShortcut(releaseEvent(down, canceled)); };
        }
        if (RuntimeService.saveDialogVisible) {
            if (key == android.view.KeyEvent.KEYCODE_BUTTON_A)
                return controllerButtons.holdKey(android.view.KeyEvent.KEYCODE_SPACE);
            if (handleSaveDialogKey(key, android.view.KeyEvent.ACTION_DOWN, 0)) return canceled -> { };
        }
        if (controlPatches != null && controlPatches.routeKey(down))
            return canceled -> controlPatches.routeKey(releaseEvent(down, canceled));
        int mapped = mapButton(key);
        if (mapped != key || key == android.view.KeyEvent.KEYCODE_BUTTON_L2
            || key == android.view.KeyEvent.KEYCODE_BUTTON_R2 || key == android.view.KeyEvent.KEYCODE_BUTTON_SELECT
            || (key >= android.view.KeyEvent.KEYCODE_DPAD_UP && key <= android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
            return controllerButtons.holdKey(mapped);
        return null;
    }

    private static android.view.KeyEvent releaseEvent(android.view.KeyEvent down, boolean canceled) {
        android.view.KeyEvent up = android.view.KeyEvent.changeAction(down, android.view.KeyEvent.ACTION_UP);
        return canceled ? android.view.KeyEvent.changeFlags(up, up.getFlags() | android.view.KeyEvent.FLAG_CANCELED) : up;
    }
    private boolean sendXKey(int keyCode, boolean down) {
        return getLorieView() != null && getLorieView().connected()
            && getLorieView().sendKeyEvent(0, keyCode, down);
    }
    private boolean handleSaveDialogKey(int keyCode, int action, int repeatCount) {
        if (!RuntimeService.saveDialogVisible) return false;
        boolean left = keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT || keyCode == keyBindings.left;
        boolean right = keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == keyBindings.right;
        if (left || right) {
            if (action == android.view.KeyEvent.ACTION_DOWN && repeatCount == 0) {
                if (left) sendXKey(android.view.KeyEvent.KEYCODE_SHIFT_LEFT, true);
                sendXKey(android.view.KeyEvent.KEYCODE_TAB, true);
                sendXKey(android.view.KeyEvent.KEYCODE_TAB, false);
                if (left) sendXKey(android.view.KeyEvent.KEYCODE_SHIFT_LEFT, false);
            }
            return true;
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_BUTTON_A || keyCode == keyBindings.a) {
            if (repeatCount == 0) sendXKey(android.view.KeyEvent.KEYCODE_SPACE,
                action == android.view.KeyEvent.ACTION_DOWN);
            return true;
        }
        return false;
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if(nativeDialogs!=null&&nativeDialogs.isShowing())return nativeDialogs.dispatch(event);
        if (gameMenu != null) {
            if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
                if (event.getAction() == android.view.KeyEvent.ACTION_UP) gameMenu.dismiss();
                return true;
            }
            return gameMenu.dispatchKeyEvent(event);
        }
        // Lorie normally consumes Back to toggle its soft keyboard. Own it here
        // so the handheld's Back button always reaches the game's quit flow.
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == android.view.KeyEvent.ACTION_UP) onBackPressed();
            return true;
        }
        if (routeControllerButton(event, false)) return true;
        if (handleSaveDialogKey(event.getKeyCode(), event.getAction(), event.getRepeatCount())) return true;
        return super.dispatchKeyEvent(event);
    }
    @Override public boolean handleKey(android.view.KeyEvent event) {
        if(event.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK){
            if(event.getAction()==android.view.KeyEvent.ACTION_UP&&!event.isCanceled())onBackPressed();
            return true;
        }
        if(nativeDialogs!=null&&nativeDialogs.isShowing())return true;
        // LorieView forwards some physical buttons before Activity.dispatchKeyEvent.
        if (gameMenu != null) return true;
        if (routeControllerButton(event, false)) return true;
        if (handleSaveDialogKey(event.getKeyCode(), event.getAction(), event.getRepeatCount())) return true;
        return super.handleKey(event);
    }
    private boolean handleKeyboardShortcut(android.view.KeyEvent event) {
        if (KeyboardShortcut.matchesKey(keyboardShortcut, event.getKeyCode())) {
            if (keyboardShortcut == KeyboardShortcut.RIGHT_TRIGGER) {
                rightTriggerKeySeen = true;
                rightTriggerAxisHeld = false;
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) rightTriggerKeyHeld = true;
                else if (event.getAction() == android.view.KeyEvent.ACTION_UP) rightTriggerKeyHeld = false;
                updateRightTrigger();
            } else if (event.getAction() == android.view.KeyEvent.ACTION_UP && !event.isCanceled()) toggleKeyboard();
            return true;
        }
        return false;
    }
    private int mapButton(int key) {
        switch (key) {
            case android.view.KeyEvent.KEYCODE_DPAD_UP: return keyBindings.up;
            case android.view.KeyEvent.KEYCODE_DPAD_DOWN: return keyBindings.down;
            case android.view.KeyEvent.KEYCODE_DPAD_LEFT: return keyBindings.left;
            case android.view.KeyEvent.KEYCODE_DPAD_RIGHT: return keyBindings.right;
            case android.view.KeyEvent.KEYCODE_BUTTON_A: return keyBindings.a;
            case android.view.KeyEvent.KEYCODE_BUTTON_B: return keyBindings.b;
            case android.view.KeyEvent.KEYCODE_BUTTON_START: return keyBindings.start;
            case android.view.KeyEvent.KEYCODE_BUTTON_L1: return keyBindings.shoulderLeft;
            case android.view.KeyEvent.KEYCODE_BUTTON_R1: return keyBindings.shoulderRight;
            case android.view.KeyEvent.KEYCODE_BUTTON_X: return keyBindings.shoulderLeft;
            case android.view.KeyEvent.KEYCODE_BUTTON_Y: return keyBindings.shoulderRight;
            default: return key;
        }
    }
    private final java.util.Map<Integer, boolean[]> directionsByDevice = new java.util.HashMap<>();
    private void updateRightTrigger() {
        boolean held = rightTriggerKeyHeld || rightTriggerAxisHeld;
        if (held && !rightTriggerHeld) toggleKeyboard();
        rightTriggerHeld = held;
    }
    private final int[] directionKeys = {android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
        android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN};
    @Override public boolean dispatchGenericMotionEvent(android.view.MotionEvent event) {
        if (gameMenu == null && controlPatches != null && controlPatches.routeMotion(event)) return true;
        return super.dispatchGenericMotionEvent(event);
    }
    @Override public boolean onGenericMotionEvent(android.view.MotionEvent event) {
        if (touchControls != null) touchControls.refreshControllerState();
        if (gameMenu != null) return true;
        if (controlPatches != null && controlPatches.routeMotion(event)) return true;
        if ((event.getSource() & android.view.InputDevice.SOURCE_JOYSTICK) != 0 && event.getAction() == android.view.MotionEvent.ACTION_MOVE) {
            if (keyboardShortcut == KeyboardShortcut.RIGHT_TRIGGER && !rightTriggerKeySeen) {
                float trigger = Math.max(event.getAxisValue(android.view.MotionEvent.AXIS_RTRIGGER),
                    event.getAxisValue(android.view.MotionEvent.AXIS_GAS));
                if (!rightTriggerAxisHeld && trigger > .6f) rightTriggerAxisHeld = true;
                else if (rightTriggerAxisHeld && trigger < .3f) rightTriggerAxisHeld = false;
                updateRightTrigger();
            }
            float x=event.getAxisValue(android.view.MotionEvent.AXIS_HAT_X), y=event.getAxisValue(android.view.MotionEvent.AXIS_HAT_Y);
            if (x == 0) x=event.getAxisValue(android.view.MotionEvent.AXIS_X);
            if (y == 0) y=event.getAxisValue(android.view.MotionEvent.AXIS_Y);
            boolean[] directions = directionsByDevice.computeIfAbsent(event.getDeviceId(), id -> new boolean[4]);
            boolean[] next={x < -.3f, x > .3f, y < -.3f, y > .3f};
            for(int i=0;i<4;i++) if(next[i]!=directions[i]) {
                directions[i]=next[i];
                int[] controllerDirections = {android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                    android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN};
                routeControllerButton(controllerEvent(event.getDeviceId(), controllerDirections[i],
                    next[i] ? android.view.KeyEvent.ACTION_DOWN : android.view.KeyEvent.ACTION_UP), false);
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }
    // The upstream super implementation toggles its keyboard; this host owns Quit.
    @android.annotation.SuppressLint("MissingSuperCall")
    @Override public void onBackPressed() {
        long now=android.os.SystemClock.uptimeMillis();
        if(now-lastNeoBack<300)return;lastNeoBack=now;
        android.util.Log.d("PokeWildsNeoUi","Open handheld menu");
        if (gameMenu != null) { gameMenu.dismiss(); return; }
        // Open after the current Back event finishes; Android 16 can otherwise
        // deliver the same release to the newly focused dialog and dismiss it.
        sessionHandler.postDelayed(this::showNeoMenu,120);
    }
    private void showNeoMenu(){
        if(gameMenu!=null||isFinishing())return;
        if (getLorieView() != null) getLorieView().setKeyboardVisible(false);
        if (touchControls != null) touchControls.releaseAll();
        releaseGamepadKeys();
        gameMenu = new android.app.AlertDialog.Builder(this).setTitle("荒野菜单")
            .setItems(new String[]{"继续冒险", "输入文字", "设置与存档", "保存并退出", "背屏伙伴", "野外技能", "跟随系统旋转"}, (d, which) -> {
                if(which==6) applyNeoOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                else if(which==5) sessionHandler.postDelayed(this::showFieldMoves,150);
                else if (which == 4) startActivity(new Intent(this, NeoPetActivity.class));
                else if (which == 1 && getLorieView() != null) getLorieView().post(this::toggleKeyboard);
                else if (which == 2) startActivity(new Intent(this, LauncherActivity.class)
                    .putExtra(LauncherActivity.MANAGE_SAVES_EXTRA, true));
                else if (which == 3) startService(new Intent(this, RuntimeService.class).setAction(RuntimeService.QUIT));
            }).create();
        gameMenu.setOnDismissListener(d -> gameMenu = null);
        gameMenu.show();
    }
    private void showFieldMoves(){
        if(controlPatches==null||!controlPatches.radialEnabled()){
            android.widget.Toast.makeText(this,"请先在设置中开启野外技能",0).show();return;
        }
        String[] names={"建造","居合斩","飞翔","冲浪","挖洞","骑乘","碎岩","瞬间移动","闪光","魅惑","力量","驱散","攻击","头锤","涂色"};
        new android.app.AlertDialog.Builder(this).setTitle("野外技能")
          .setItems(names,(d,i)->sessionHandler.postDelayed(()->{
              controlPatches.resume();controlPatches.wheel("neo-menu",true);
              if(controlPatches.wheelShown()){controlPatches.chooseFromMenu(i);controlPatches.wheel("neo-menu",false);}
              else android.widget.Toast.makeText(this,"当前状态无法使用野外技能",0).show();
          },200)).show();
    }
    private void confirmForceStop() {
        new android.app.AlertDialog.Builder(this).setTitle("Force-stop recovery?")
            .setMessage("Use this only when normal Quit cannot close the game. Unsaved progress may be lost.")
            .setPositiveButton("Force-stop", (d, w) -> {
                RuntimeService.status = "Force-stopping game…";
                startService(new Intent(this, RuntimeService.class).setAction(RuntimeService.STOP));
            })
            .setNegativeButton("Keep playing", null).show();
    }
}
