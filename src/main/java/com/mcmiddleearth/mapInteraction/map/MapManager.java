package com.mcmiddleearth.mapInteraction.map;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;

public class MapManager {

    private final HashMap<String, Map> maps = new HashMap<>();

    public MapManager() {
        loadMaps();
        //TaskTimer every second to check if chunk of a map is loaded -> load map (if not already loaded)
        // also check if no player within like 4 chunks -> unload map (if not already unloaded
    }

    private void loadMaps() {
        ConfigurationSection mapsConfig = MapsPlugin.getInstance().getConfig().getConfigurationSection("maps");
        if(mapsConfig != null) {
            for(String mapName : mapsConfig.getKeys(false)) {
                ConfigurationSection mapConfig = mapsConfig.getConfigurationSection(mapName);
                if(mapConfig!=null) {
                    ConfigurationSection worldConfig = mapConfig;
                    String copy = mapConfig.getString("copy of");
                    if(copy != null) {
                        ConfigurationSection copyConfig = mapsConfig.getConfigurationSection(copy);
                        if(copyConfig != null) {
                            mapConfig = copyConfig;
                        }
                    }
                    maps.put(mapName, new Map(mapConfig, worldConfig));
                }
            }
        } else {
            MapsPlugin.getInstance().getMcmeLogger().warn("No maps section inf config.yml found. There will be no maps.");
        }
    }

    public void reload() {
        disable();
        maps.clear();
        MapsPlugin.getInstance().reloadConfig();
        loadMaps();
    }

    public void disable() {
        maps.forEach((mapName, map) -> map.remove());
    }

    public HashMap<String, Map> getMaps() {
        return maps;
    }
}
