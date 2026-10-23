package com.pkmngen.game;

import local.pokewilds.bugfix.ControllerConfirm;

/** Checks the real guest action's routing, without rendering editor assets. */
public final class ControllerStartTest {
    static void pulse(Game game) {
        ControllerStart.request(game);
        for (Action action : game.actionStack.toArray(new Action[0]))
            if (action instanceof ControllerStart) action.step(game);
    }
    public static void main(String[] args) throws Exception {
        Game game = MapShortcutTest.world();
        ControllerStart.request(game);
        ControllerStart.request(game);
        MapShortcutTest.check(game.actionStack.size() == 1, "queued Start is deduplicated");
        pulse(game);
        MapShortcutTest.check(InputProcessor.startJustPressed && InputProcessor.startPressed, "ordinary Start reaches gameplay");
        MapShortcutTest.check(game.actionStack.isEmpty(), "Start is a one-frame action");
        InputProcessor.startJustPressed = InputProcessor.startPressed = false;
        InputProcessor.acceptInput = false;
        pulse(game);
        MapShortcutTest.check(!InputProcessor.startJustPressed, "blocked input ignores Start");
        InputProcessor.acceptInput = true;
        Pokemon.SetNickname nickname = MapShortcutTest.empty(Pokemon.SetNickname.class);
        game.actionStack.add(nickname);
        pulse(game);
        MapShortcutTest.check(!InputProcessor.startJustPressed, "editor confirmation cannot also open a menu");
        MapShortcutTest.check(ControllerConfirm.consume(false, nickname), "controller confirms nickname");
        MapShortcutTest.check(!ControllerConfirm.consume(false, nickname), "confirmation consumed once");
        game.actionStack.clear();
        Tile.SetSignText sign = MapShortcutTest.empty(Tile.SetSignText.class);
        game.actionStack.add(sign);
        pulse(game);
        MapShortcutTest.check(ControllerConfirm.consume(false, sign), "controller confirms sign");
        sign.disabled = true;
        pulse(game);
        MapShortcutTest.check(!ControllerConfirm.consume(false, sign), "disabled editor cannot confirm again");
        System.out.println("PASS controller Start gameplay/editor routing and one-shot confirmation");
    }
}
