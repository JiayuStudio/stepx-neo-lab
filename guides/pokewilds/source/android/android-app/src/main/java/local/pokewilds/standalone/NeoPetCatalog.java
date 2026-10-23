package local.pokewilds.standalone;
import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Decode assets once per process, keeping JSON and file I/O out of the frame loop. */
final class NeoPetCatalog {
    static final class Pet {
        final String id,name;final int[][] poses;final boolean chibi;
        Pet(String id,JSONObject o)throws JSONException {
            this.id=id;name=o.optString("name",id);chibi="chibi".equals(o.optString("style"));JSONArray a=o.getJSONArray("frames");poses=new int[a.length()][391];
            for(int j=0;j<a.length();j++){JSONArray f=a.getJSONArray(j);if(f.length()!=391)throw new JSONException("Invalid matrix");for(int i=0;i<391;i++)poses[j][i]=Math.max(0,Math.min(255,f.getInt(i)));}
        }
        public String toString(){return name+" · "+id;}
    }
    final LinkedHashMap<String,Pet> pets=new LinkedHashMap<>();private static NeoPetCatalog instance;
    private NeoPetCatalog(Context c)throws Exception {
        try(InputStream in=c.getAssets().open("neo-pets.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)b.write(buf,0,n);
            JSONObject root=new JSONObject(b.toString("UTF-8"));ArrayList<String> keys=new ArrayList<>();root.keys().forEachRemaining(keys::add);Collections.sort(keys);
            for(String id:keys)pets.put(id,new Pet(id,root.getJSONObject(id)));
        }
    }
    static synchronized NeoPetCatalog get(Context c)throws Exception {if(instance==null)instance=new NeoPetCatalog(c);return instance;}
    Pet find(String id){Pet p=pets.get(id);return p==null?pets.get("machop"):p;}
    static Properties readGame(Context c){
        Properties p=new Properties();File f=new File(c.getFilesDir(),"game/neo-state.properties");
        if(f.isFile())try(Reader r=new InputStreamReader(new FileInputStream(f),StandardCharsets.UTF_8)){p.load(r);}catch(IOException ignored){}
        return p;
    }
    static String resolve(android.content.SharedPreferences prefs,Properties game){
        String id=prefs.getString("pet","machop");
        if(prefs.getBoolean("follow",true))id=game.getProperty("pet."+prefs.getInt("slot",0),prefs.getString("lastPet",id));
        return id;
    }
}
