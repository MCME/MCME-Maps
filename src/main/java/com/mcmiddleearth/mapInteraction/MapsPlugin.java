package com.mcmiddleearth.mapInteraction;

import com.mcmiddleearth.base.bukkit.AbstractPaperPlugin;
import com.mcmiddleearth.base.core.message.Message;
import com.mcmiddleearth.mapInteraction.map.MapManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public final class MapsPlugin extends AbstractPaperPlugin {

    private static MapsPlugin plugin;

    private MapManager mapManager;

    public static MapsPlugin getInstance() {
        return plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void enable() {
        plugin = this;
        saveDefaultConfig();
        mapManager = new MapManager();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register( Commands.literal("mcmemaps")
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                mapManager.reload();
                                return 0;
                            }))
                    .build());
        });
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
