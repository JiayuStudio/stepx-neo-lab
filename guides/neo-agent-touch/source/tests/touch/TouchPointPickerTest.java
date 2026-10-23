package studio.jiayu.neoagent;
import java.util.*;
public final class TouchPointPickerTest {
 public static void main(String[] args){
  TouchPointPicker picker=new TouchPointPicker(new Random(7));Set<String> used=new HashSet<>();
  for(int i=0;i<24;i++){int[] p=picker.pick(102,202,198,248,150,225,12);check(p[0]>=138&&p[0]<=162&&p[1]>=213&&p[1]<=237,"bounded offset");check(used.add(Arrays.toString(p)),"unique in run");check(p[0]!=150||p[1]!=225,"not anchor");}
  picker.reset();for(int i=0;i<100;i++){int[] p=picker.pick(0,0,3,100,1,50,12);check(p[0]>=0&&p[0]<3,"narrow target");if(i==24)break;}
  picker.reset();int[] edge=picker.pick(0,0,1080,2400,0,0,12);check(edge[0]>=0&&edge[1]>=0,"screen edge");
  picker.reset();boolean stopped=false;try{picker.pick(5,5,6,6,5,5,12);}catch(IllegalArgumentException expected){stopped=true;}check(stopped,"tiny target stops");
  picker.reset();Set<String> small=new HashSet<>();for(int i=0;i<3;i++)check(small.add(Arrays.toString(picker.pick(0,0,2,2,0,0,2))),"small unique");stopped=false;try{picker.pick(0,0,2,2,0,0,2);}catch(IllegalArgumentException expected){stopped=true;}check(stopped,"exhaustion stops");
  System.out.println("PASS: bounds, 24 unique task touches, narrow target, screen edge, tiny target, exhaustion");
 }
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}
