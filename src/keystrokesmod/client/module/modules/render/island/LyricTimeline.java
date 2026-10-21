package keystrokesmod.client.module.modules.render.island;

import java.util.*;
import java.util.regex.*;

/** Immutable LRC/enhanced-LRC timeline. Malformed entries are ignored independently. */
public final class LyricTimeline {
    public static final LyricTimeline EMPTY=new LyricTimeline(new Line[0]);
    private static final Pattern TIME=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]"),WORD=Pattern.compile("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>"),OFFSET=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE);
    public static final class Line {
        public final double time;public final String text;private final double[] wordTimes;private final int[] wordEnds;
        public final LyricWords words;
        private final double estimatedDuration;
        Line(double time,String text,double[] times,int[] ends){this.time=time;this.text=text;wordTimes=times;wordEnds=ends;
            int words=0;boolean inWord=false;for(int i=0;i<text.length();i++){boolean word=!Character.isWhitespace(text.charAt(i));if(word&&!inWord)words++;inWord=word;}
            estimatedDuration=Math.max(1.6,Math.min(8,words*.38+text.length()*.018));
            this.words=new LyricWords(text,time,times,ends);
        }
        public double vocalEnd(double next){return Math.min(next,wordTimes.length>0?wordTimes[wordTimes.length-1]+.9:time+estimatedDuration);}
        /** Glyph fraction, using explicit word timing or character-weighted interpolation. */
        public double fill(double position,double end){
            if(text.isEmpty()||position<=time)return 0;
            // A long timestamp interval is not proof of silence. Line-only LRC has no
            // vocal-end information: retain the line and interpolate until the next cue.
            if(wordTimes.length==0)return Math.max(0,Math.min(1,(position-time)/Math.max(.1,end-time)));
            int previous=0;for(int i=0;i<wordTimes.length;i++){
                double stop=i+1<wordTimes.length?wordTimes[i+1]:end;
                if(position<stop){double t=Math.max(0,Math.min(1,(position-wordTimes[i])/Math.max(.05,stop-wordTimes[i])));return(previous+(wordEnds[i]-previous)*t)/text.length();}previous=wordEnds[i];
            }return 1;
        }
    }
    private final Line[] lines;
    public final boolean approximate;
    private LyricTimeline(Line[] lines){this(lines,false);}
    private LyricTimeline(Line[] lines,boolean approximate){this.lines=lines;this.approximate=approximate;}
    public boolean hasWords(){for(Line line:lines)if(line.words.start.length>0)return true;return false;}
    /** Plain text has no measured timestamps. Preserve every word and explicitly mark estimates. */
    public static LyricTimeline estimate(String text,double duration){
        if(text==null||text.length()>2000000)return EMPTY;List<String> content=new ArrayList<>();List<Double> weights=new ArrayList<>();double total=0;
        for(String raw:text.replace("\r","").split("\n")){String s=raw.trim();if(s.isEmpty()||s.matches("\\[[^\\]]{1,80}\\]"))continue;content.add(s);double weight=Math.max(1,Math.sqrt(s.codePointCount(0,s.length())));weights.add(weight);total+=weight;}
        if(content.isEmpty())return EMPTY;double span=Double.isFinite(duration)&&duration>0?duration:Math.max(30,content.size()*4.0),cursor=0;Line[] out=new Line[content.size()];
        for(int i=0;i<out.length;i++){out[i]=new Line(span*cursor/total,content.get(i),new double[0],new int[0]);cursor+=weights.get(i);}return new LyricTimeline(out,true);
    }
    public int size(){return lines.length;}
    public Line line(int index){return index>=0&&index<lines.length?lines[index]:null;}
    public int index(double seconds){int lo=0,hi=lines.length-1,result=-1;while(lo<=hi){int mid=(lo+hi)>>>1;if(lines[mid].time<=seconds){result=mid;lo=mid+1;}else hi=mid-1;}return result;}
    private static double seconds(Matcher m){double fraction=m.group(3)==null?0:Double.parseDouble("0."+m.group(3));int s=Integer.parseInt(m.group(2));return s<60?Integer.parseInt(m.group(1))*60+s+fraction:Double.NaN;}
    public static LyricTimeline parse(String lrc){
        if(lrc==null||lrc.length()>2000000)return EMPTY;
        TreeMap<Double,Line> parsed=new TreeMap<>();double offset=0;Matcher off=OFFSET.matcher(lrc);if(off.find())try{offset=Long.parseLong(off.group(1))/1000D;}catch(NumberFormatException ignored){}
        for(String raw:lrc.split("\\r?\\n")){
            Matcher tags=TIME.matcher(raw);List<Double> times=new ArrayList<>();int last=0;
            while(tags.find()){double t=seconds(tags);if(Double.isFinite(t))times.add(Math.max(0,t+offset));last=tags.end();}
            if(times.isEmpty())continue;String body=raw.substring(last);Matcher words=WORD.matcher(body);StringBuilder clean=new StringBuilder();List<Double> wt=new ArrayList<>();List<Integer> ends=new ArrayList<>();int cursor=0;
            while(words.find()){
                clean.append(body,cursor,words.start());double stamp=seconds(words)+offset;
                if(Double.isFinite(stamp)&&(wt.isEmpty()||stamp>=wt.get(wt.size()-1))){if(!wt.isEmpty())ends.add(clean.length());wt.add(stamp);}
                cursor=words.end();
            }
            clean.append(body.substring(cursor));if(!wt.isEmpty())ends.add(clean.length());String text=clean.toString();
            double[] a=new double[wt.size()];int[] b=new int[a.length];for(int i=0;i<a.length;i++){a[i]=wt.get(i);b[i]=ends.get(i);}
            for(double t:times){Line previous=parsed.get(t);if(previous==null||previous.text.trim().isEmpty()||!text.trim().isEmpty())parsed.put(t,new Line(t,text,a,b));}
        }
        return parsed.isEmpty()?EMPTY:new LyricTimeline(parsed.values().toArray(new Line[0]));
    }
}
