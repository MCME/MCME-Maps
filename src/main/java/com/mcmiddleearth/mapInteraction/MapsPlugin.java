package com.mcmiddleearth.mapInteraction;

import com.mcmiddleearth.base.bukkit.AbstractPaperPlugin;
import com.mcmiddleearth.base.core.message.Message;
import com.mcmiddleearth.mapInteraction.map.MapManager;

public final class MapsPlugin extends AbstractPaperPlugin {

    private static MapsPlugin plugin;

    private MapManager mapManager;

    public static MapsPlugin getInstance() {
        return plugin;
    }

    @Override
    public void enable() {
        plugin = this;
        saveDefaultConfig();
        mapManager = new MapManager();
    }

    @Override
    public void disable() {
        if(mapManager != null) {
            mapManager.disable();
        }
    }

    @Override
    public Message getMessagePrefix() {
        return  createMessage().add("[MapInteraction] ");
    }
}
