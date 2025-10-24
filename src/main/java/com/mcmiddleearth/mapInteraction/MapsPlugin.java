package com.mcmiddleearth.mapInteraction;

import com.mcmiddleearth.base.bukkit.AbstractPaperPlugin;
import com.mcmiddleearth.base.core.message.Message;
import com.mcmiddleearth.mapInteraction.map.MapManager;
import com.mcmiddleearth.mapInteraction.map.animation.AnimationListener;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.ticxo.modelengine.api.events.ModelRegistrationEvent;
import com.ticxo.modelengine.api.generator.ModelGenerator;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

public final class MapsPlugin extends AbstractPaperPlugin implements Listener {

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
        Bukkit.getPluginManager().registerEvents(this, this);
        //Bukkit.getPluginManager().registerEvents(new AnimationListener(), this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register( Commands.literal("mcmemaps")
                    .requires(source -> source.getSender().hasPermission("mcmemaps.manager"))
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                mapManager.reload();
                                return 0;
                            }))
                    .then(Commands.literal("cleanup")
                            .then(Commands.argument("radius", IntegerArgumentType.integer(0,10))
                                    .requires(source -> source.getSender() instanceof Player)
                                    .executes(context -> {
                                        Location loc = ((Player)context.getSource().getSender()).getLocation();
                                        loc.getWorld().getNearbyEntitiesByType(Interaction.class,
                                                loc,context.getArgument("radius",Integer.class))
                                                .forEach(Entity::remove);
                                        return 0;
                                    })))
                    .build());
        });
    }

    @EventHandler
    public void onModelEngineLoad(ModelRegistrationEvent event) {
        if(event.getPhase().equals(ModelGenerator.Phase.FINISHED)) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if(mapManager != null) {
                    this.getMcmeLogger().info("Unloading maps...");
                    mapManager.disable();
                }
                this.getMcmeLogger().info("Loading maps...");
                mapManager = new MapManager();
                this.getMcmeLogger().info("Finished map loading.");
            },600);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if(mapManager!=null)
            mapManager.checkUnload(event.getChunk());
    }

    @Override
    public void disable() {
        if(mapManager != null) {
            mapManager.disable();
        }
        HandlerList.unregisterAll((Plugin)this);
    }

    @Override
    public Message getMessagePrefix() {
        return  createMessage().add("[MapInteraction] ");
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public static MapsPlugin getPlugin() {
        return plugin;
    }
}
