# Control patch provenance

The source in `src/com/pkmngen/game/OdinAgent.java` adapts the game-side wheel
action and camera zoom logic from the user-provided
`PokeWilds-v0.8.11-RadialWheel-ZoomControls.patch` in the project folder. It is
built against the exact official PokeWilds 0.8.11 game JAR, verified by SHA-256
in `build.sh`. The official game JAR is not modified or bundled in the APK.

The proposal's `OdinInputHook` and its replacement Android `TouchControls` are
not included. This agent has no bytecode transformer or general input hook. It
only accepts wheel, zoom, map shortcut, and controller Start commands from the Android app; the existing touch
overlay and gamepad mappings remain in place. None of the proposal's image
assets are included.

The proposal did not establish redistribution terms. This record describes
technical origin and is not a license grant.

`MapShortcut.java` adds a direct entry to the stock `DrawMiniMap` screen,
based on inspection of the official 0.8.11 decompiled map and menu lifecycle.
It preserves the stock teleport and B-button close paths, with a synthetic
return menu restoring gameplay for direct entry. It makes no save changes.

Run `bash test.sh /path/to/pokewilds.jar` for the headless map lifecycle checks.
The test fixtures suppress the asset-loading static initializers of Player,
Battle, and PkmnMap; production classes and the APK are never altered by those
fixtures. Rendering and teleport animation still require device validation.

`ControllerStart.java` routes controller Start to the game action or an explicit
text-editor confirmation, without sending a typed character when Start has a
letter-key mapping. This path is enabled only with Controller button prompts.
