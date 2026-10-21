package keystrokesmod.client.config;

import com.google.gson.*;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.config.ConfigModuleManager;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Compatible replacement for Raven's profile manager. Disk work occurs on actions, not draw calls. */
public class ConfigManager {
    public interface State { JsonObject snapshot();void apply(JsonObject modules); }
    public final ConfigModuleManager configModuleManager;
    public static boolean applyingConfig;
    private final File configDirectory;
    private final State state;
    private final ArrayList<Config> configs=new ArrayList<>();
    private Config config;
    public ConfigManager(){this(new File(Minecraft.getMinecraft().mcDataDir,"keystrokes/configs"),new LiveState(),true);}
    /** Isolated directory/state injection for real filesystem regression tests. */
    public ConfigManager(File directory,State state,boolean legacyMenus){
        configDirectory=directory.getAbsoluteFile();this.state=state;configModuleManager=legacyMenus?new ConfigModuleManager():null;
        if(!configDirectory.isDirectory()&&!configDirectory.mkdirs())throw new IllegalStateException("Couldn't create the config folder.");
        config=new Config(new File(configDirectory,"default.bplus"));
        if(!config.file.exists()||config.file.length()==0)save(config);
        discoverConfigs();
    }
    private static JsonObject copy(JsonObject data){return new JsonParser().parse(data.toString()).getAsJsonObject();}
    public static boolean isOutdated(File file){JsonObject data=new Config(file).getData();return data==null||!data.has("modules")||!data.get("modules").isJsonObject();}
    public void discoverConfigs(){
        File[] files=configDirectory.listFiles();if(files==null)throw new IllegalStateException("Couldn't read the config folder.");
        Arrays.sort(files,(a,b)->a.getName().compareToIgnoreCase(b.getName()));
        ArrayList<Config> found=new ArrayList<>();
        for(File file:files)if(file.isFile()&&file.getName().toLowerCase(Locale.ROOT).endsWith(".bplus")&&!isOutdated(file)){
            Config item=null;for(Config previous:configs)if(previous.file.equals(file)){item=previous;break;}
            if(item==null)item=config!=null&&config.file.equals(file)?config:new Config(file);found.add(item);
        }
        boolean changed=configs.size()!=found.size();if(!changed)for(int i=0;i<found.size();i++)if(configs.get(i)!=found.get(i)){changed=true;break;}
        configs.clear();configs.addAll(found);
        if(changed&&configModuleManager!=null&&Raven.configManager!=null&&Raven.clickGui!=null)configModuleManager.updater(configs);
    }
    public Config getConfig(){return config;}
    /** Cached stable objects: never refresh the folder from a frame getter. */
    public ArrayList<Config> getConfigs(){return configs;}
    public void save(){save(config);}
    public void save(Config target){
        owned(target);JsonObject data=target.getData();
        if(data==null){JsonObject active=config==null?null:config.getData();data=active==null?new JsonObject():copy(active);}else data=copy(data);
        if(!data.has("author"))data.addProperty("author","Unknown");if(!data.has("notes"))data.addProperty("notes","");
        data.addProperty("lastEditTime",System.currentTimeMillis());JsonObject live=state.snapshot();
        if(live==null)throw new IllegalStateException("Client settings aren't available yet.");
        JsonObject merged=data.has("modules")&&data.get("modules").isJsonObject()?copy(data.getAsJsonObject("modules")):new JsonObject();
        for(Map.Entry<String,JsonElement> entry:live.entrySet())merged.add(entry.getKey(),entry.getValue());data.add("modules",merged);target.save(data);
    }
    public Config createConfig(String name){
        File target=newFile(name);Config created=new Config(target);save(created);config=created;discoverConfigs();return created;
    }
    public void copyConfig(Config source,String name){
        owned(source);JsonObject data=source.getData();if(data==null)throw new IllegalStateException("The source config is empty or invalid.");
        Config destination=new Config(newFile(name));destination.save(copy(data));discoverConfigs();
    }
    public void setConfig(Config target){
        owned(target);JsonObject data=target.getData();
        if(data==null||!data.has("modules")||!data.get("modules").isJsonObject())throw new IllegalArgumentException("That config is empty or invalid; nothing was loaded.");
        JsonObject before=state.snapshot();boolean previous=applyingConfig;applyingConfig=true;
        try{state.apply(copy(data.getAsJsonObject("modules")));config=target;}
        catch(RuntimeException|LinkageError failure){
            try{state.apply(before);}catch(RuntimeException|LinkageError rollback){failure.addSuppressed(rollback);}
            throw new IllegalStateException("Couldn't load that config; your previous setup was restored.",failure);
        }finally{applyingConfig=previous;}
    }
    public void loadConfigByName(String name){
        String normalized=normalizeName(name);discoverConfigs();
        for(Config item:configs)if(item.getName().equalsIgnoreCase(normalized)){setConfig(item);return;}
        throw new IllegalArgumentException("Config not found or invalid: "+normalized);
    }
    public void resetConfig(){
        boolean before=applyingConfig;applyingConfig=true;
        try{if(Raven.moduleManager!=null)for(Module module:Raven.moduleManager.getConfigModules()){module.resetToDefaults();module.postApplyConfig();}save();}
        finally{applyingConfig=before;}
    }
    public void deleteConfig(Config target){
        owned(target);Config replacement=null;
        // Retain the live setup when deleting the active profile; never reset modules or overwrite a different profile.
        if(config!=null&&config.file.equals(target.file)){
            String name="Recovered-"+System.currentTimeMillis();replacement=new Config(newFile(name));save(replacement);
        }
        try{Files.move(target.file.toPath(),target.file.toPath().resolveSibling(target.file.getName()+".deleted-"+System.currentTimeMillis()));}
        catch(IOException failure){throw new IllegalStateException("Couldn't remove that config; its file has been kept.",failure);}
        if(replacement!=null)config=replacement;discoverConfigs();
    }
    private File newFile(String input){
        String name=normalizeName(input);File target=new File(configDirectory,name+".bplus");
        File[] all=configDirectory.listFiles();if(all==null)throw new IllegalStateException("Couldn't read the config folder.");
        for(File file:all)if(file.getName().equalsIgnoreCase(target.getName()))throw new IllegalArgumentException("A config with that name already exists.");
        return target;
    }
    public static String normalizeName(String input){
        String name=input==null?"":input.trim();if(name.toLowerCase(Locale.ROOT).endsWith(".bplus"))name=name.substring(0,name.length()-6).trim();
        if(name.isEmpty()||name.length()>48||name.equals(".")||name.equals("..")||name.endsWith("."))throw new IllegalArgumentException("Enter a config name (1-48 characters).");
        for(int i=0;i<name.length();i++)if(name.charAt(i)<32||"\\/:*?\"<>|".indexOf(name.charAt(i))>=0)throw new IllegalArgumentException("The name cannot contain path separators or special filename characters.");
        String device=name.split("\\.",2)[0].toUpperCase(Locale.ROOT);
        if(device.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]"))throw new IllegalArgumentException("Choose a different config name.");return name;
    }
    private void owned(Config target){
        try{if(target==null||!target.file.getCanonicalFile().getParentFile().equals(configDirectory.getCanonicalFile()))throw new IllegalArgumentException("Config must be inside the config folder.");}
        catch(IOException failure){throw new IllegalArgumentException("Couldn't resolve the config path.",failure);}
    }
    private static final class LiveState implements State {
        public JsonObject snapshot(){
            JsonObject modules=new JsonObject();if(Raven.moduleManager==null)return modules;
            for(Module module:Raven.moduleManager.getConfigModules())modules.add(module.getName(),module.getConfigAsJson());return modules;
        }
        public void apply(JsonObject data){
            if(Raven.moduleManager==null)throw new IllegalStateException("Module manager isn't ready.");
            List<Module> modules=new ArrayList<>(Raven.moduleManager.getConfigModules());
            for(Module module:modules)if(data.has(module.getName())){
                JsonElement saved=data.get(module.getName());if(!saved.isJsonObject())throw new IllegalArgumentException("Invalid module data: "+module.getName());
                JsonObject merged=module.getConfigAsJson();merge(merged,saved.getAsJsonObject());module.applyConfigFromJson(merged);
            }
        }
        private static void merge(JsonObject current,JsonObject saved){
            for(Map.Entry<String,JsonElement> e:saved.entrySet()){
                if(e.getValue().isJsonObject()&&current.has(e.getKey())&&current.get(e.getKey()).isJsonObject())merge(current.getAsJsonObject(e.getKey()),e.getValue().getAsJsonObject());
                else current.add(e.getKey(),e.getValue());
            }
        }
    }
}
