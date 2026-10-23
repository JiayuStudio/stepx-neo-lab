package studio.jiayu.neoagent;
import java.util.regex.*;import java.math.BigDecimal;
/** Read-only scalar facts: no addresses, identifiers, account data, or clickable payment coordinates. */
final class CommerceReadbackRules {
 static final Pattern AMOUNT=Pattern.compile("^(实付|应付|合计|总计|共计|立即支付|提交订单)(?:金额)?[:：]?\\s*(?:[¥￥]\\s*([0-9]{1,6}(?:\\.[0-9]{1,2})?)|([0-9]{1,6}(?:\\.[0-9]{1,2})?)\\s*元)(?=$|\\s|[|｜]|优惠前|共减|已减|含|包|[（(])");
 static String[] amount(String text){if(text==null||text.length()>100||text.matches("(?is).*(收货|地址|送至|配送|联系人|银行|信用卡|花呗|分期|免息|保险|保障|延保|利息|贷款|授信|定金|每期|身份证|[0-9]{7,}|@|https?://).*"))return null;
  Matcher m=AMOUNT.matcher(FooterLabelRules.normalizeCurrency(text.replaceAll("\\s+","")));if(!m.find())return null;try{BigDecimal n=new BigDecimal(m.group(2)==null?m.group(3):m.group(2));if(n.signum()<0||n.compareTo(new BigDecimal("999999"))>0)return null;return new String[]{m.group(1),n.stripTrailingZeros().toPlainString()};}catch(Exception ignored){return null;}}
 static int quantity(String text){if(text==null)return 0;Matcher m=Pattern.compile("^(?:购买数量|数量)[:：]?\\s*([1-9][0-9]{0,2})\\s*(?:件|个|份)?$").matcher(text.replaceAll("\\s+",""));if(!m.matches())m=Pattern.compile("^[−一-]\\s*([1-9][0-9]{0,2})\\s*[+＋]$").matcher(text.replaceAll("\\s+",""));return m.matches()?Integer.parseInt(m.group(1)):0;}
}
