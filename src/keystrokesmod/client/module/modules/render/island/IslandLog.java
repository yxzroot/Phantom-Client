package keystrokesmod.client.module.modules.render.island;
import java.util.*;import java.util.logging.*;
public final class IslandLog {
    private static final Set<String> logged=new HashSet<>();
    private IslandLog(){}
    public static synchronized void once(String key,Throwable error){if(logged.size()<64&&logged.add(key))Logger.getLogger("Phantom/DynamicIsland").log(Level.WARNING,key,error);}
}
