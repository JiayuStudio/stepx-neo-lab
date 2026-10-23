package local.pokewilds.standalone;
/** 391 grayscale cells. Pose blends and continuous transforms share a monotonic clock. */
final class NeoPetAnimator {
    static int[] render(int[][] poses,String action,double t){return render(poses,action,t,false);}
    static int[] render(int[][] poses,String action,double t,boolean chibi){
        boolean sleep=action.equals("sleep"),play=action.equals("play");
        double cycle=t*(sleep?.6:play?6:3),phase=cycle-Math.floor(cycle);
        int p=(int)Math.floor(cycle)%poses.length,q=(p+1)%poses.length;
        if(chibi){double blink=t%5.2;p=blink<.16?1:action.equals("feed")?2:action.equals("pet")?3:play?(int)(t*3)%2==0?3:0:0;p=Math.min(p,poses.length-1);q=p;}
        // Crossfade briefly at pose boundaries instead of an abrupt two-frame flip.
        double blend=Math.max(0,(phase-.65)/.35);blend=0; // Original pixel art: avoid fading pupils into the face during idle pose changes.
        double dx=play?Math.sin(t*3.8)*1.7:0;
        double dy=play?-Math.abs(Math.sin(t*5))*1.7:0;
        if(chibi){dx=Math.rint(dx);dy=Math.rint(dy);}
        int[] out=new int[391];
        for(int y=0;y<23;y++)for(int x=0;x<17;x++){
            double v=sample(poses[p],x-dx,y-dy)*(1-blend)+sample(poses[q],x-dx,y-dy)*blend;
            out[y*17+x]=(int)Math.max(0,Math.min(255,v*(sleep?.64:1)));
        }
        if(action.equals("feed")||action.equals("pet")){
            int[] heart={1,3,17,18,19,20,21,35,36,37,53};int offset=(int)(Math.sin(t*3)*.6)+4;
            for(int i:heart)if(i+offset>=0&&i+offset<391)out[i+offset]=235;
        }
        if(sleep){for(int i:new int[]{47,48,64,80,81})out[i]=180;}
        return out;
    }
    private static double sample(int[] p,double x,double y){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy;
        return at(p,ix,iy)*(1-fx)*(1-fy)+at(p,ix+1,iy)*fx*(1-fy)+at(p,ix,iy+1)*(1-fx)*fy+at(p,ix+1,iy+1)*fx*fy;
    }
    private static int at(int[] p,int x,int y){return x<0||x>=17||y<0||y>=23?0:p[y*17+x];}
}
