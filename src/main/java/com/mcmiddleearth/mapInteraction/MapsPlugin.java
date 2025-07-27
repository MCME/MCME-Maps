package com.mcmiddleearth.mapInteraction;

import com.mcmiddleearth.base.bukkit.AbstractPaperPlugin;
import com.mcmiddleearth.base.core.message.Message;
import com.mcmiddleearth.mapInteraction.map.Map;
import org.bukkit.Bukkit;

public final class MapsPlugin extends AbstractPaperPlugin {

    private static MapsPlugin plugin;

    private Map map;
    private PlayerListener playerListener;

    public static MapsPlugin getInstance() {
        return plugin;
    }

    @Override
    public void enable() {
        plugin = this;
        saveDefaultConfig();
        map = new Map(getConfig());
        playerListener = new PlayerListener(map);
        Bukkit.getPluginManager().registerEvents(playerListener, this);
    }

    @Override
    public void disable() {
        if(map!=null) {
            map.remove();
        }
        playerListener.disable();
    }

    @Override
    public Message getMessagePrefix() {
        return  createMessage().add("[MapInteraction] ");
    }
}
