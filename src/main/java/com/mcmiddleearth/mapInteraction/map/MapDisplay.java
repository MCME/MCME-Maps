package com.mcmiddleearth.mapInteraction.map;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.marker.Marker;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;

public class MapDisplay implements Listener {

    private final Map map;
    private final HashMap<Player, TextDisplay> coordEntities = new HashMap<>();
    private final HashMap<Player, Marker> markers = new HashMap<>();
    private final BukkitTask task;

    public MapDisplay(Map map) {
        this.map = map;
        task = new BukkitRunnable()  {
            int counter=0;
            @Override
            public void run() {
                map.getCenter().getWorld().getNearbyEntitiesByType(Player.class, map.getCenter(), 6)
                        .forEach(player -> playerMove(player));
                counter++;
                if(counter == 4) {
                    counter = 0;
                    if(map.getCenter().getWorld().getNearbyEntitiesByType(Player.class, map.getCenter(),
                                                                map.getActivationRadius()).isEmpty()) {
                        map.close();
                    }
                }
            }
        }.runTaskTimer(MapsPlugin.getPlugin(), 0, 5);
    }

    public void clear() {
        coordEntities.forEach((player, entity) -> entity.remove());
        coordEntities.clear();
        markers.forEach((player, marker) -> player.hideEntity(MapsPlugin.getInstance(), marker.getEntity()));
        markers.clear();
        task.cancel();
    }

    public void playerMove(Player player) {
        if(player.getLocation().distance(map.getCenter()) < 8) {
            Map.RayTraceTarget rayTraceTarget = map.getRayTraceTarget(player);
            Position position = rayTraceTarget.position();
            Marker marker = rayTraceTarget.marker();
            if (marker != null) {
                Marker lastMarker = markers.get(player);
                if (lastMarker != marker) {
                    if (lastMarker != null) {
                        player.hideEntity(MapsPlugin.getInstance(), lastMarker.getEntity());
                    }
                    player.showEntity(MapsPlugin.getInstance(), marker.getEntity());
                    markers.put(player, marker);
                }
            } else {
                hideMarker(player);
            }
            if (MapsPlugin.getPlugin().isShowCoordinates(player)) {
                TextDisplay entity = coordEntities.get(player);
                if (position != null) {
                    Location entityPosition = new Location(player.getWorld(), position.getWorldX(),
                            map.getCenter().getY() + 1,
                            position.getWorldZ());
                    if (entity == null) {
                        entity = (TextDisplay) player.getWorld()
                                .spawnEntity(entityPosition,
                                        EntityType.TEXT_DISPLAY);
                        coordEntities.put(player, entity);
                        entity.setShadowed(true);
                        entity.setBillboard(Display.Billboard.CENTER);
                        entity.setTransformation(new Transformation(new Vector3f(0, 0, 0),
                                new Quaternionf(0, 0, 0, 1),
                                new Vector3f(0.5f, 0.5f, 0.5f),
                                new Quaternionf(0, 0, 0, 1)));
                    } else {
                        entity.teleport(entityPosition);
                    }
                    entity.text(Component.text(String.format("x: %1$.0f  z: %2$.0f", position.getMapX(), position.getMapZ())));
                } else {
                    if (entity != null) {
                        coordEntities.remove(player);
                        entity.remove();
                    }
                }
            } else {
                Entity entity = coordEntities.get(player);
                if(entity != null) {
                    entity.remove();
                    coordEntities.remove(player);
                }
            }
        } else {
            hideMarker(player);
        }
    }

    private void hideMarker(Player player) {
        Marker marker = markers.get(player);
        if(marker != null) {
            player.hideEntity(MapsPlugin.getInstance(), marker.getEntity());
            markers.remove(player);
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = coordEntities.get(player);
        if(entity != null) {
            entity.remove();
        }
        Marker lastMarker = markers.get(player);
        if(lastMarker != null) {
            player.hideEntity(MapsPlugin.getInstance(), lastMarker.getEntity());
            markers.remove(player);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(EquipmentSlot.HAND.equals(event.getHand())) {
            handleInteract(event.getPlayer());
        }
    }

    @EventHandler
    public void playerInteract(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer());
    }

    private void handleInteract(Player player) {
        Map.RayTraceTarget target = map.getRayTraceTarget(player);
        if(target.marker()!=null) {
            target.marker().handleInteract(player);
        }
    }

}
