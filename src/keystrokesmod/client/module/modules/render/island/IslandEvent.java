package keystrokesmod.client.module.modules.render.island;

public final class IslandEvent {
    public enum Kind { Toggle,Notification,Combat,Timer,Warning }
    public final Kind kind;public final String key,title,message,icon;
    public final int priority,color;public final long duration;public final boolean enabled;
    public IslandEvent(Kind kind,String key,String title,String message,String icon,int color,int priority,long millis,boolean enabled){
        this.kind=kind;this.key=safe(key);this.title=safe(title);this.message=safe(message);this.icon=safe(icon);
        this.color=color|0xFF000000;this.priority=priority;duration=Math.max(250,Math.min(60000,millis))*1000000L;this.enabled=enabled;
    }
    private static String safe(String s){return s==null?"":s.length()>512?s.substring(0,512):s;}
}
