package com.pkmngen.game;

/** A direct entry to the stock map, with the stock teleport and close lifecycle. */
final class MapShortcut {
    private MapShortcut() { }

    static boolean safe(Game g) {
        if (g == null || g.player == null || g.map == null || g.cam == null || g.actionStack == null
                || g.battle == null || g.player.pokemon == null || !g.playerCanMove || !g.player.canMove
                || !g.player.acceptInput || !InputProcessor.acceptInput || g.player.isSleeping
                || g.battle.drawAction != null) return false;
        if (g.player.flyingAction != null && g.player.flyingAction.takingOff) return false;
        for (Action a : g.actionStack) {
            if (a instanceof Menu) return false;
            String name = a.getClass().getName();
            if (name.contains("DisplayText") || name.contains("Intro") || name.contains("Outro")
                    || name.contains("Teleport") || name.contains("EnterBuilding")) return false;
        }
        return true;
    }

    static boolean moving(Game g) {
        for (Action a : g.actionStack) if (a instanceof PlayerMoving || a instanceof PlayerRunning || a instanceof Player.Flying.Moving) return true;
        return false;
    }

    static void request(Game g) {
        if (g == null || g.actionStack == null) return;
        for (Action a : g.actionStack) {
            if (a instanceof PendingOpen) return;
            if (a instanceof DrawMiniMap) {
                DrawMiniMap map = (DrawMiniMap) a;
                if (map.disabled || map.firstStep || map.goAway) return;
                // Never dismiss the map underneath a teleport confirmation or other dialog.
                for (Action overlay : g.actionStack) {
                    if (overlay instanceof Menu && overlay != map && overlay != map.drawText) return;
                    if (overlay.getClass().getName().contains("DisplayText")) return;
                }
                close(g, map);
                return;
            }
        }
        if (safe(g)) g.insertAction(new PendingOpen());
    }

    private static void close(Game g, DrawMiniMap map) {
        // Same cleanup and return transition as DrawMiniMap's B handler.
        map.texture.dispose();
        map.warpTileTexture.dispose();
        if (map.prevMenu != null) map.prevMenu.disabled = false;
        g.actionStack.remove(map.drawText);
        g.actionStack.remove(map);
        g.insertAction(new DrawPokemonMenu.Intro(map.prevMenu));
        g.insertAction(new PlayMusic("click1", null));
    }

    static final class PendingOpen extends Action {
        private final long expires = System.currentTimeMillis() + 1000;
        @Override public Layer getLayer() { return Layer.map_1; }
        @Override public void step(Game g) {
            if (!safe(g) || System.currentTimeMillis() > expires) { g.actionStack.remove(this); return; }
            if (moving(g)) return;
            ReturnToWorld back = new ReturnToWorld(g);
            // Finish constructing before freezing movement so a failed asset load cannot strand the player.
            DrawMiniMap map = new DrawMiniMap(g, back);
            DrawMiniMap.Intro intro = new DrawMiniMap.Intro(null, 9, map);
            g.actionStack.remove(this);
            g.playerCanMove = false;
            g.player.canMove = false;
            g.insertAction(intro);
        }
    }

    static final class ReturnToWorld extends Menu {
        private final Player player;
        private final boolean worldMove, playerMove;
        ReturnToWorld(Game g) {
            player = g.player;
            worldMove = g.playerCanMove;
            playerMove = player.canMove;
        }
        @Override public void step(Game g) {
            g.actionStack.remove(this);
            if (g.player != player || goAway) return;
            g.playerCanMove = worldMove;
            player.canMove = playerMove;
        }
    }
}
