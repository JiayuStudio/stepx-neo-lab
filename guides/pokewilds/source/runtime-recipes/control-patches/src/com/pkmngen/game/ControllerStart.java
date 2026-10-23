package com.pkmngen.game;

/** Delivers controller Start without typing its keyboard mapping into a text editor. */
final class ControllerStart extends Action {
    static void request(Game g) {
        if (g == null || g.actionStack == null) return;
        for (Action a : g.actionStack) if (a instanceof ControllerStart) return;
        g.insertAction(new ControllerStart());
    }

    // InputProcessor is map_5000. Apply after it has polled input, before gameplay and GUI actions.
    @Override public Layer getLayer() { return Layer.map_500; }

    @Override public void step(Game g) {
        g.actionStack.remove(this);
        if (!InputProcessor.acceptInput) return;
        for (Action a : g.actionStack) {
            if (a instanceof Pokemon.SetNickname) {
                Pokemon.SetNickname editor = (Pokemon.SetNickname) a;
                if (!editor.disabled && !editor.done) confirm(editor);
                return;
            }
            if (a instanceof Tile.SetSignText) {
                Tile.SetSignText editor = (Tile.SetSignText) a;
                if (!editor.disabled && !editor.done) confirm(editor);
                return;
            }
        }
        InputProcessor.startJustPressed = true;
        InputProcessor.startPressed = true;
    }

    private static void confirm(Object editor) {
        try {
            Class.forName("local.pokewilds.bugfix.ControllerConfirm")
                .getMethod("request", Object.class).invoke(null, editor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Controller prompt confirmation unavailable", e);
        }
    }
}
