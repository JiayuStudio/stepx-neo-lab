package studio.jiayu.neoagent;
import android.content.Context;import org.json.*;import java.io.*;
/** Trusted packaged skills; no arbitrary file paths, downloaded code or new mutation authority. */
final class DeviceSkills {
 static final String[] IDS={"core","documents","search","commerce","games"};
 static boolean known(String id){for(String item:IDS)if(item.equals(id))return true;return false;}
 static JSONArray list()throws Exception{JSONArray all=new JSONArray();for(String id:IDS)all.put(new JSONObject().put("id",id).put("uri","neo-skill://"+id).put("name",id.equals("core")?"目标执行":id.equals("documents")?"自主文档写作":id.equals("search")?"搜索与播放":id.equals("commerce")?"购物与关注":"小游戏视觉决策").put("source","bundled").put("version","1.0"));return all;}
 static String read(Context c,String id)throws Exception{if(!known(id))throw new Exception("未知技能，不能读取任意文件");try(InputStream in=c.getAssets().open("agent-skills/"+id+".md")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>16000)throw new Exception("技能内容过长");}return out.toString("UTF-8");}}
 static String[] select(String pkg,boolean game){return game?new String[]{"core","search","games"}:select(pkg);}
 static String[] select(String pkg){return DocumentScope.applies(pkg)?new String[]{"core","documents"}:AppCatalog.isCommerce(pkg)?new String[]{"core","search","commerce"}:new String[]{"core","search"};}
}
