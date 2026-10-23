package studio.jiayu.neoagent;
/** Android multiline body proxy, distinct from filename/search/password inputs. */
final class DocumentInputRules {
 static boolean bodyProxy(int inputType,int imeOptions,boolean nativeWriterFrame,boolean saveDialog){int type=inputType&15,variation=inputType&4080;return nativeWriterFrame&&!saveDialog&&type==1&&variation!=128&&variation!=144&&variation!=224&&(inputType&0x20000)!=0&&(imeOptions&255)==1;}
}
