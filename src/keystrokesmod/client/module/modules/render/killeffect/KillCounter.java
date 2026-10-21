package keystrokesmod.client.module.modules.render.killeffect;

import com.google.gson.JsonObject;
import keystrokesmod.client.module.setting.impl.TickSetting;

/** Ordinary HUD toggle with a backwards-compatible, config-persisted statistic. */
public final class KillCounter extends TickSetting {
    private int kills;
    private String label="Kills: 0";
    public KillCounter(){super("Show total kills",false);}
    public int total(){return kills;}
    public String label(){return label;}
    public void increment(){if(kills<Integer.MAX_VALUE)kills++;label="Kills: "+kills;}
    @Override public JsonObject getConfigAsJson(){JsonObject json=super.getConfigAsJson();json.addProperty("totalKills",kills);return json;}
    @Override public void applyConfigFromJson(JsonObject json){
        super.applyConfigFromJson(json);
        try{if(json.has("totalKills"))kills=(int)Math.max(0,Math.min(Integer.MAX_VALUE,json.get("totalKills").getAsLong()));}catch(RuntimeException ignored){kills=0;}
        label="Kills: "+kills;
    }
    @Override public void resetToDefaults(){super.resetToDefaults();kills=0;label="Kills: 0";}
}
