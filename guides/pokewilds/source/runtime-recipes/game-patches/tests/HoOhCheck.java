import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
public class HoOhCheck {
    @SuppressWarnings("unchecked")
    public static void main(String[] a) throws Exception {
        Gdx.files = new Lwjgl3Files();
        Class<?> c = Class.forName("com.pkmngen.game.Pokemon");   // runs the game's real static initializer
        java.util.HashMap<String, String> m = (java.util.HashMap<String, String>) c.getField("baseSpecies").get(null);
        System.out.println("table entries=" + m.size() + "  get(\"ho_oh\")=" + m.get("ho_oh") + "  get(\"hooh\")=" + m.get("hooh")
                + "  get(\"charizard\")=" + m.get("charizard") + "  get(\"charmander\")=" + m.get("charmander"));
    }
}
