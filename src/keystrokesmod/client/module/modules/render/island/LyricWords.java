package keystrokesmod.client.module.modules.render.island;

/** Parsed once with the LRC on its worker. Three-word phrases and optional held-vowel variants. */
public final class LyricWords {
    public final int[] start,end;public final String[][] display;
    private final double time;private final int length;private final double[] times;private final int[] ends;
    public LyricWords(String text,double time,double[] times,int[] ends){
        this.time=time;this.times=times;this.ends=ends;length=text.length();
        int count=0;boolean word=false;for(int i=0;i<length;i++){boolean next=!Character.isWhitespace(text.charAt(i));if(next&&!word)count++;word=next;}
        start=new int[count];end=new int[count];display=new String[count][5];int cursor=0;
        for(int i=0;i<count;i++){
            while(cursor<length&&Character.isWhitespace(text.charAt(cursor)))cursor++;start[i]=cursor;
            while(cursor<length&&!Character.isWhitespace(text.charAt(cursor)))cursor++;end[i]=cursor;
            String original=text.substring(start[i],end[i]);display[i][0]=original;int vowel=-1;
            for(int c=0;c<original.length();c++)if("aeiouyäöüаеёиоуыэюяіїє".indexOf(Character.toLowerCase(original.charAt(c)))>=0)vowel=c;
            for(int n=1;n<5;n++){
                if(vowel<0){display[i][n]=original;continue;}
                StringBuilder held=new StringBuilder(original.length()+n);held.append(original,0,vowel+1);for(int c=0;c<n;c++)held.append(original.charAt(vowel));held.append(original.substring(vowel+1));display[i][n]=held.toString();
            }
        }
    }
    private double ending(double end){return end;}
    private double stamp(int character,double end){
        end=ending(end);if(times.length==0)return time+(end-time)*character/Math.max(1,length);
        int from=0;for(int i=0;i<times.length;i++){if(character<ends[i]||i==times.length-1){double stop=i+1<times.length?times[i+1]:end;double f=Math.max(0,Math.min(1,(character-from)/(double)Math.max(1,ends[i]-from)));return times[i]+(stop-times[i])*f;}from=ends[i];}
        return end;
    }
    public double begins(int word,double end){return stamp(start[word],end);}
    public double finishes(int word,double end){return word+1<start.length?begins(word+1,end):ending(end);}
    public int active(double seconds,double end){int result=0;for(int i=1;i<start.length;i++){if(begins(i,end)>seconds)break;result=i;}return result;}
    public double progress(int word,double seconds,double end){return Math.max(0,Math.min(1,(seconds-begins(word,end))/Math.max(.05,finishes(word,end)-begins(word,end))));}
    public float held(int word,double seconds,double end){
        if(times.length==0||display[word][0].equals(display[word][1]))return 0; // Never claim vocal detection from line-only lyrics.
        double begin=begins(word,end),duration=finishes(word,end)-begin;if(duration<1.15)return 0;
        if(word==start.length-1&&duration>8)return 0; // Unknown final-word release is not a measured held note.
        double t=Math.max(0,Math.min(1,(seconds-begin-.35)/Math.max(.2,duration-.35)));t=t*t*(3-2*t);
        return(float)(Math.min(4,(duration-.8)*2)*t);
    }
}
