package studio.jiayu.neoagent;
import android.content.Context;
import org.json.*;
import java.net.URI;
final class Config {
 String endpoint="https://api.deepseek.com",model="deepseek-flash",protocol="OpenAI",key=""; boolean vision=true,checkout=false; int maxSteps=24;
 JSONObject json()throws Exception{return new JSONObject().put("endpoint",endpoint).put("model",model).put("protocol",protocol).put("vision",vision).put("maxSteps",maxSteps);}
 String keyId(){return "key:"+endpoint;}
 void validate()throws Exception{URI u=new URI(endpoint); if(!"https".equals(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new Exception("接口必须是 HTTPS 地址，不能含密钥或查询参数"); if(model.trim().isEmpty()||key.trim().isEmpty())throw new Exception("请填写模型和 API Key");}
 void save(Context c)throws Exception{validate();new Vault(c).put(keyId(),key);c.getSharedPreferences("settings",0).edit().putString("config",json().toString()).commit();}
 static Config load(Context c){Config x=new Config();try{JSONObject o=new JSONObject(c.getSharedPreferences("settings",0).getString("config","{}"));x.endpoint=o.optString("endpoint",x.endpoint);x.model=o.optString("model",x.model);x.protocol=o.optString("protocol",x.protocol);x.vision=o.optBoolean("vision",true);x.maxSteps=Math.max(1,Math.min(40,o.optInt("maxSteps",24)));x.key=new Vault(c).get(x.keyId());}catch(Exception e){}return x;}
}
