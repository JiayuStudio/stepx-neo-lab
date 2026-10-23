package com.pkmngen.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import java.lang.reflect.Field;
import java.util.ArrayList;
import sun.misc.Unsafe;

/** Exercises lifecycle decisions without creating textures or starting a game window. */
public final class MapShortcutTest {
    static final Unsafe UNSAFE;
    static {
        try { Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); UNSAFE = (Unsafe) f.get(null); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    static <T> T empty(Class<T> type) throws Exception { return type.cast(UNSAFE.allocateInstance(type)); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static Game world() throws Exception {
        Game g = empty(Game.class);
        g.player = empty(Player.class);
        g.player.pokemon = new ArrayList<>();
        g.player.acceptInput = true;
        g.player.canMove = true;
        g.playerCanMove = true;
        g.map = empty(PkmnMap.class);
        g.cam = empty(OrthographicCamera.class);
        g.actionStack = new ArrayList<>();
        g.battle = empty(Battle.class);
        InputProcessor.acceptInput = true;
        return g;
    }
    public static void main(String[] args) throws Exception {
        Game g = world();
        check(MapShortcut.safe(g), "idle world should allow map");
        MapShortcut.request(g);
        MapShortcut.request(g);
        check(g.actionStack.size() == 1 && g.actionStack.get(0) instanceof MapShortcut.PendingOpen, "one pending request");
        PlayerMoving moving = empty(PlayerMoving.class);
        g.actionStack.add(moving);
        check(MapShortcut.moving(g), "walking requires waiting for tile boundary");
        MapShortcut.PendingOpen pending = (MapShortcut.PendingOpen) g.actionStack.get(0);
        pending.step(g);
        check(g.actionStack.contains(pending) && g.playerCanMove, "waiting must not freeze movement");
        g.actionStack.add(new Menu());
        pending.step(g);
        check(!g.actionStack.contains(pending), "dialog/menu cancels queued request");
        g.actionStack.clear();
        Player.Flying flight = empty(Player.Flying.class);
        g.player.flyingAction = flight;
        flight.takingOff = true;
        check(!MapShortcut.safe(g), "takeoff blocks map");
        flight.takingOff = false;
        Player.Flying.Moving flightStep = empty(Player.Flying.Moving.class);
        g.actionStack.add(flightStep);
        check(MapShortcut.safe(g) && MapShortcut.moving(g), "flying movement waits at a safe boundary");
        g.actionStack.clear();
        g.player.flyingAction = null;
        g.player.isSleeping = true;
        check(!MapShortcut.safe(g), "sleep blocks map");
        g.player.isSleeping = false;
        g.playerCanMove = false;
        check(!MapShortcut.safe(g), "transition blocks map");
        g.playerCanMove = true;
        g.battle.drawAction = empty(DrawBattle.class);
        check(!MapShortcut.safe(g), "battle blocks map");
        g.battle.drawAction = null;
        MapShortcut.ReturnToWorld back = new MapShortcut.ReturnToWorld(g);
        g.playerCanMove = g.player.canMove = false;
        g.actionStack.add(back);
        back.step(g);
        check(g.playerCanMove && g.player.canMove && !g.actionStack.contains(back), "close restores ground and flying movement");
        back = new MapShortcut.ReturnToWorld(g);
        g.playerCanMove = g.player.canMove = false;
        back.goAway = true;
        back.step(g);
        check(!g.playerCanMove && !g.player.canMove, "abandoned return cannot resume during teleport");
        g = world();
        DrawMiniMap map = empty(DrawMiniMap.class);
        g.actionStack.add(map);
        map.disabled = true;
        MapShortcut.request(g);
        check(g.actionStack.contains(map), "disabled map ignores Select");
        map.disabled = false;
        Menu teleportConfirmation = new Menu();
        g.actionStack.add(teleportConfirmation);
        MapShortcut.request(g);
        check(g.actionStack.contains(map), "Select cannot dismiss map under teleport confirmation");
        g.actionStack.remove(teleportConfirmation);
        Action dialogue = empty(DisplayText.class);
        g.actionStack.add(dialogue);
        MapShortcut.request(g);
        check(g.actionStack.contains(map), "Select cannot dismiss map under dialogue");
        g.actionStack.remove(dialogue);
        map.goAway = true;
        MapShortcut.request(g);
        check(g.actionStack.contains(map), "teleport transition owns map cleanup");
        System.out.println("PASS map gating, walking queue, cancellation, movement restoration, and overlay close guards");
    }
}
