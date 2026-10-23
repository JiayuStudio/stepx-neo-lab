package studio.jiayu.neoagent;

/** Quiet speech can produce decoder progress without crossing the energy threshold. */
final class VoiceEndpointRules {
 static final long INITIAL_SILENCE_MS=10000,TRAILING_SILENCE_MS=3200;
 private final long began;
 private long activity;
 private boolean heard;
 private int decoderUpdates;
 private String partial="";
 VoiceEndpointRules(long now){began=now;activity=now;}
 void audio(int rms,long now){if(rms>500)progress(now);}
 void segment(String text,long now){partial="";if(!VoiceSession.normalize(text).isEmpty()){decoderUpdates++;progress(now);}}
 void partial(String text,long now){String value=VoiceSession.normalize(text);if(value.isEmpty()){partial="";return;}if(!value.equals(partial)){decoderUpdates++;progress(now);partial=value;}}
 private void progress(long now){heard=true;activity=Math.max(activity,now);}
 boolean shouldStop(long now){return heard?now-activity>=TRAILING_SILENCE_MS:now-began>=INITIAL_SILENCE_MS;}
 boolean heard(){return heard;}
 long lastActivity(){return activity;}
 int decoderUpdates(){return decoderUpdates;}
}
