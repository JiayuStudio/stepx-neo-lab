package studio.jiayu.neoagent;
import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;

/** Secrets never leave native code after entry; Android Keystore key is not exportable. */
final class Vault {
 private final Context ctx;
 Vault(Context c){ctx=c;}
 private SecretKey key() throws Exception {
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);
  if(!k.containsAlias("neo-agent-api-v1")){
   KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
   g.init(new KeyGenParameterSpec.Builder("neo-agent-api-v1",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
  }
  return (SecretKey)k.getKey("neo-agent-api-v1",null);
 }
 void put(String id,String value)throws Exception{
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
  String s=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(value.getBytes("UTF-8")),Base64.NO_WRAP);
  if(!ctx.getSharedPreferences("vault",0).edit().putString(id,s).commit())throw new Exception("密钥保存失败");
 }
 String get(String id)throws Exception{
  String s=ctx.getSharedPreferences("vault",0).getString(id,"");if(s.isEmpty())return "";
  String[]p=s.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],0)));return new String(c.doFinal(Base64.decode(p[1],0)),"UTF-8");
 }
 boolean has(String id){return ctx.getSharedPreferences("vault",0).contains(id);}
 void remove(String id){ctx.getSharedPreferences("vault",0).edit().remove(id).commit();}
}
