package com.mcmiddleearth.mapInteraction;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.logging.Logger;

public class PlayerListener implements Listener {

    private final Map map;
    private final HashMap<Player, TextDisplay> entities = new HashMap<>();
    private final HashMap<Player, WarpData> warps = new HashMap<>();

    private final boolean showCoordinates = false;

    public PlayerListener(Map map) {
        this.map = map;
    }

    public void disable() {
        entities.forEach((player,entity) -> entity.remove());
        entities.clear();
        warps.forEach((player,warp) -> {
            player.hideEntity(MapsPlugin.getInstance(),warp.getEntity());
        });
        warps.clear();
    }
    @EventHandler
    public void playerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(player.getLocation().distance(map.getCenter()) < 8) {
//Logger.getGlobal().info("move inside");
            Position position = map.getTargetPosition(player);
            if(position != null) {
//Logger.getGlobal().info("Position: "+position.getWorldX()+" "+map.getCenter().getY()+1+" "+position.getWorldZ());
                WarpData warp = map.getWarp(position);
                WarpData lastWarp = warps.get(player);
                if(warp != null) {
Logger.getGlobal().info("Target warp: "+warp.getName());
                    if(lastWarp != warp) {
Logger.getGlobal().info("show entity: "+warp.getName());
                        if(lastWarp != null) {
                            player.hideEntity(MapsPlugin.getInstance(), lastWarp.getEntity());
                        }
                        player.showEntity(MapsPlugin.getInstance(),warp.getEntity());
                        warps.put(player, warp);
                    }
                } else {
                    if(lastWarp != null) {
Logger.getGlobal().info("hide entity: "+lastWarp.getName());
                        player.hideEntity(MapsPlugin.getInstance(),lastWarp.getEntity());
                        warps.remove(player);
                    }
                }
                if(showCoordinates) {
                    TextDisplay entity = entities.get(player);
                    Location entityPosition = new Location(player.getWorld(), position.getWorldX(),
                            map.getCenter().getY() + 1,
                            position.getWorldZ());
                    if (entity == null) {
//Logger.getGlobal().info("Create entity");
                        entity = (TextDisplay) player.getWorld()
                                .spawnEntity(entityPosition,
                                        EntityType.TEXT_DISPLAY);
                        entities.put(player, entity);
                        entity.setShadowed(true);
                        entity.setBillboard(Display.Billboard.CENTER);
                        entity.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                                new Quaternionf(0, 0, 0, 1),
                                new Vector3f(0.5f, 0.5f, 0.5f),
                                new Quaternionf(0, 0, 0, 1)));
                    } else {
                        entity.teleport(entityPosition);
                    }
                    entity.text(Component.text(String.format("x: %1$.2f  z: %2$.2f", position.getMapX(), position.getMapZ())));
                }
            } else {
                if(showCoordinates) {
                    TextDisplay entity = entities.get(player);
                    if (entity != null) {
                        Logger.getGlobal().info("remove entity");
                        entities.remove(player);
                        entity.remove();
                    }
                }
                WarpData warp = warps.get(player);
                if(warp != null) {
                    player.hideEntity(MapsPlugin.getInstance(), warp.getEntity());
                    warps.remove(player);
                }
            }
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = entities.get(player);
        if(entity != null) {
            entity.remove();
        }
        WarpData lastWarp = warps.get(player);
        if(lastWarp != null) {
            player.hideEntity(MapsPlugin.getInstance(), lastWarp.getEntity());
            warps.remove(player);
        }
    }

/*    @EventHandler
    public void playerInteract(PlayerInteractAtEntityEvent event) {
        if(event.getRightClicked().equals(map.getEntity())) {
            event.getPlayer().sendMessage(String.format("World: x: %1$.2f  y: %2$.2f  z: %3$.2f"
                    +"\nMap:   x: %4$.2f  z: %5$.2f",
                    event.getClickedPosition().getX(),
                    event.getClickedPosition().getY(),
                    event.getClickedPosition().getZ(),
                    map.getTransformation().getMapX(event.getClickedPosition().getX()),
                    map.getTransformation().getMapZ(event.getClickedPosition().getZ())));
            RayTraceResult result = event.getPlayer().getWorld().rayTraceEntities(event.getPlayer().getEyeLocation(),
                                                                event.getPlayer().getEyeLocation().getDirection(),5,
                                                                entity -> entity instanceof Interaction);
            if(result != null && map.getEntity().equals(result.getHitEntity())) {
                event.getPlayer().sendMessage(String.format("*Hit: x: %1$.2f  y: %2$.2f  z: %3$.2f"
                                +"\n*Map: x: %4$.2f  z: %5$.2f",
                        result.getHitPosition().getX(),
                        result.getHitPosition().getY(),
                        result.getHitPosition().getZ(),
                        map.getTransformation().getMapX(result.getHitPosition().getX()),
                        map.getTransformation().getMapZ(result.getHitPosition().getZ())));
            } else {
                event.getPlayer().sendMessage(String.format("Eye: x: %1$.2f y: %2$.2f z: %3$.2f "
                        +"\nDirection: x: %4$.2f y: %5$.2f z: %6$.2f"
                        +"\nRay trace result: "+result,
                        event.getPlayer().getEyeLocation().getX(),
                        event.getPlayer().getEyeLocation().getY(),
                        event.getPlayer().getEyeLocation().getZ(),
                        event.getPlayer().getEyeLocation().getDirection().getX(),
                        event.getPlayer().getEyeLocation().getDirection().getY(),
                        event.getPlayer().getEyeLocation().getDirection().getZ()));
            }
        } else {
            event.getPlayer().sendMessage("Other Interaction");
        }
    }*/

}
