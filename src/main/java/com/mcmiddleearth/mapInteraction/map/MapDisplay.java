package com.mcmiddleearth.mapInteraction.map;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.marker.Marker;
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

public class MapDisplay implements Listener {

    private final Map map;
    private final HashMap<Player, TextDisplay> entities = new HashMap<>();
    private final HashMap<Player, com.mcmiddleearth.mapInteraction.map.marker.Marker> markers = new HashMap<>();

    private final boolean showCoordinates = false;

    public MapDisplay(Map map) {
        this.map = map;
    }

    public void clear() {
        entities.forEach((player,entity) -> entity.remove());
        entities.clear();
        markers.forEach((player, marker) -> {
            player.hideEntity(MapsPlugin.getInstance(),marker.getEntity());
        });
        markers.clear();
    }
    @EventHandler
    public void playerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(player.getLocation().distance(map.getCenter()) < 8) {
//Logger.getGlobal().info("move inside");
            Position position = map.getTargetPosition(player);
            if(position != null) {
//Logger.getGlobal().info("Position: "+position.getWorldX()+" "+map.getCenter().getY()+1+" "+position.getWorldZ());
                Marker marker = map.getMarker(position);
                Marker lastMarker = markers.get(player);
                if(marker != null) {
Logger.getGlobal().info("Target marker: "+marker.getPlainText());
                    if(lastMarker != marker) {
Logger.getGlobal().info("show entity: "+marker.getPlainText());
                        if(lastMarker != null) {
                            player.hideEntity(MapsPlugin.getInstance(), lastMarker.getEntity());
                        }
                        player.showEntity(MapsPlugin.getInstance(),marker.getEntity());
                        markers.put(player, marker);
                    }
                } else {
                    if(lastMarker != null) {
Logger.getGlobal().info("hide entity: "+lastMarker.getPlainText());
                        player.hideEntity(MapsPlugin.getInstance(),lastMarker.getEntity());
                        markers.remove(player);
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
                Marker marker = markers.get(player);
                if(marker != null) {
                    player.hideEntity(MapsPlugin.getInstance(), marker.getEntity());
                    markers.remove(player);
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
        Marker lastMarker = markers.get(player);
        if(lastMarker != null) {
            player.hideEntity(MapsPlugin.getInstance(), lastMarker.getEntity());
            markers.remove(player);
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
