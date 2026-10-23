package studio.jiayu.neoagent;
/** A save dialog or editor header cannot prove reopening from the file list. */
final class DocumentReopen {
 int written,read;boolean listed,pending;
 void reset(){written=read=0;listed=pending=false;}
 void wrote(int revision){written=revision;listed=pending=false;}
 void listObserved(){if(!pending)listed=true;}
 boolean openRequested(){if(!listed)return false;listed=false;pending=true;return true;}
 void invalidateRead(){read=0;}
 void bodyRead(){if(pending){read=written;pending=false;}}
 boolean currentRead(){return written>0&&read==written&&!pending;}
}
