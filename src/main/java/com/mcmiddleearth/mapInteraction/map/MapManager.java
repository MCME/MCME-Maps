package com.mcmiddleearth.mapInteraction.map;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;

public class MapManager {

    private final HashMap<String, Map> maps = new HashMap<>();

    public MapManager() {
        ConfigurationSection mapsConfig = MapsPlugin.getInstance().getConfig().getConfigurationSection("maps");
        if(mapsConfig != null) {
            for(String mapName : mapsConfig.getKeys(false)) {
                ConfigurationSection mapConfig = mapsConfig.getConfigurationSection(mapName);
                if(mapConfig!=null) {
                    maps.put(mapName, new Map(mapConfig));
                }
            }
        } else {
            MapsPlugin.getInstance().getMcmeLogger().warn("No maps section inf config.yml found. There will be no maps.");
        }
    }

    public void disable() {
        maps.forEach((mapName, map) -> map.remove());
    }
}
