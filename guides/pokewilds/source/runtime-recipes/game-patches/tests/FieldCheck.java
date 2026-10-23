package local.pokewilds.bugfix;
public class FieldCheck { public static void main(String[] a) throws Exception {
  ClassLoader cl = FieldCheck.class.getClassLoader();
  Class<?> g = Class.forName("com.pkmngen.game.Game", false, cl), m = Class.forName("com.pkmngen.game.PkmnMap", false, cl), p = Class.forName("com.pkmngen.game.Pokemon", false, cl);
  String[] ok = { Hooks.open(g,"actionStack").getType().getSimpleName(), Hooks.open(m,"onscreenPokemon").getType().getSimpleName(),
    Hooks.open(p,"standingAction").getType().getSimpleName(), Hooks.open(p,"drawLower").getType().getSimpleName(), Hooks.open(p,"drawThisFrame").getType().getSimpleName(),
    Hooks.open(p,"nickname").getType().getSimpleName(), Hooks.open(p,"position").getType().getSimpleName() };
  System.out.println("diagnostic fields found: " + String.join(", ", ok));
  Class<?> v = Class.forName("com.badlogic.gdx.math.Vector2", false, cl); System.out.println("Vector2.x public: " + v.getField("x").getType());
}}
