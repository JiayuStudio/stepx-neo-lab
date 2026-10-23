package local.pokewilds.standalone;

/** Single geometry source for native game viewport, console artwork and touch hit areas. */
final class NeoConsoleLayout {
    static final class Box {
        final float x,y,w,h;
        Box(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;}
        float cx(){return x+w/2;} float cy(){return y+h/2;}
    }
    final boolean portrait;
    final Box screen, bezel, dpad, a,b,select,start,l,r,menu,rotate;
    final float unit;
    static final String[] UTILITY_LABELS={"L","R","保存","读档","技能","设置","方向"};
    static final String[] UTILITY_COMMANDS={"l","r","save","load","skills","settings","rotate"};
    final Box[] utilities=new Box[UTILITY_LABELS.length];
    final float utilityTop;
    NeoConsoleLayout(int width,int height,float density){
        float w=width,h=height;portrait=h>w;
        // Proportional shells fit small phones and tall displays without offscreen controls.
        unit=Math.min(density,portrait?Math.min(w/360f,h/780f):h/360f);
        float u=unit;
        if(portrait){
            float sw=w-24*u,sh=Math.min(sw*.90f,h*.46f),top=64*u;
            screen=new Box(12*u,top,sw,sh);
            bezel=new Box(5*u,top-27*u,w-10*u,sh+48*u);
            float controlY=Math.max(top+sh+123*u,h*.635f);
            controlY=Math.min(controlY,h-198*u);
            dpad=new Box(w*.235f-66*u,controlY-66*u,132*u,132*u);
            a=new Box(w*.83f-32*u,controlY-49*u,64*u,64*u);
            b=new Box(w*.64f-32*u,controlY-15*u,64*u,64*u);
            float sy=Math.min(h-100*u,controlY+111*u);
            select=new Box(w*.30f-32*u,sy-18*u,64*u,42*u);
            start=new Box(w*.52f-32*u,sy+4*u,64*u,42*u);
            l=new Box(0,0,0,0);r=new Box(0,0,0,0);
            menu=new Box(0,0,0,0);rotate=new Box(0,0,0,0);
            utilityTop=h-42*u;layoutUtilities(w,u);
        }else{
            float side=Math.max(128*u,w*.18f);
            screen=new Box(side,48*u,w-2*side,h-94*u);
            bezel=new Box(side-16*u,26*u,w-2*side+32*u,h-48*u);
            float x=side*.47f,y=h*.47f;
            dpad=new Box(x-49*u,y-49*u,98*u,98*u);
            a=new Box(w-side*.34f-24*u,y-49*u,48*u,48*u);
            b=new Box(w-side*.80f-24*u,y-17*u,48*u,48*u);
            select=new Box(x-27*u,h*.73f-15*u,54*u,30*u);
            start=new Box(x-27*u,h*.84f-15*u,54*u,30*u);
            l=new Box(22*u,10*u,64*u,27*u);r=new Box(w-86*u,10*u,64*u,27*u);
            menu=new Box(0,0,0,0);rotate=new Box(0,0,0,0);
            utilityTop=h-34*u;layoutUtilities(w,u);
        }
    }
    private void layoutUtilities(float w,float u){
        float gap=4*u,cell=(w-16*u-(utilities.length-1)*gap)/utilities.length;
        for(int i=0;i<utilities.length;i++)utilities[i]=new Box(8*u+i*(cell+gap),utilityTop,cell,30*u);
    }
}
