package studio.jiayu.neoagent;
public class CaptureRulesTest {public static void main(String[] args){
 if(!CaptureRules.retryable(3)||!CaptureRules.retryable(1)||CaptureRules.retryable(2)||CaptureRules.retryable(6)||CaptureRules.spacing(1000,1100)!=250||CaptureRules.spacing(1000,1600)!=0||CaptureRules.message(3).contains("保护"))throw new AssertionError();
 if(!CaptureRules.uniform(new int[]{0xff000000,0xff000000})||!CaptureRules.uniform(new int[]{0xffffffff,0xfffefefe})||!CaptureRules.uniform(new int[]{0xff121212,0xff141414})||!CaptureRules.uniform(new int[]{0xff121212,0x00121212})||!CaptureRules.uniform(null)||!CaptureRules.uniform(new int[0]))throw new AssertionError("Blank image was sent as usable pixels");
 if(CaptureRules.uniform(new int[]{0xff121212,0xffffffff})||CaptureRules.uniform(new int[]{0xff000000,0xff000080})||CaptureRules.uniform(new int[]{0xff202020,0xff203020}))throw new AssertionError("Visible image content rejected");
 System.out.println("PASS: screenshot transient/protected errors, spacing and 9 blank-frame boundaries");
}}
