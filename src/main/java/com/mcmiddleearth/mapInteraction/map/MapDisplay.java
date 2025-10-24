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

    private final static boolean showCoordinates = false;

    public MapDisplay(Map map) {
        this.map = map;
        task = new BukkitRunnable()  {
            int counter=0;
            @Override
            public void run() {
//Logger.getGlobal().info("Move ticker...");
                map.getCenter().getWorld().getNearbyEntitiesByType(Player.class, map.getCenter(), 6).forEach(player -> {
//Logger.getGlobal().info("Player move: "+player.getName());
                    playerMove(player);
                });
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
        markers.forEach((player, marker) -> {
            player.hideEntity(MapsPlugin.getInstance(), marker.getEntity());
        });
        markers.clear();
        task.cancel();
    }

    //@EventHandler
    //public void playerMove(PlayerMoveEvent event) {
    //    Player player = event.getPlayer();
    public void playerMove(Player player) {
        if(player.getLocation().distance(map.getCenter()) < 8) {
            Map.RayTraceTarget rayTraceTarget = map.getRayTraceTarget(player);
            Position position = rayTraceTarget.position();
            Marker marker = rayTraceTarget.marker();
            //Logger.getGlobal().info("Markers: "+map.);
//Logger.getGlobal().info("Position: "+position+" marker: "+marker);
            if (marker != null) {
                //TextMarker marker = map.getMarker(position);
                Marker lastMarker = markers.get(player);
                //if(marker != null) {
                if (lastMarker != marker) {
                    if (lastMarker != null) {
                        player.hideEntity(MapsPlugin.getInstance(), lastMarker.getEntity());
                    }
                    player.showEntity(MapsPlugin.getInstance(), marker.getEntity());
                    markers.put(player, marker);
                }
                /*} else {
                    if(lastMarker != null) {
                        player.hideEntity(MapsPlugin.getInstance(),lastMarker.getEntity());
                        markers.remove(player);
                    }
                }*/
            } else {
                hideMarker(player);
            }
            if (showCoordinates) {
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
                    entity.text(Component.text(String.format("x: %1$.2f  z: %2$.2f", position.getMapX(), position.getMapZ())));
                } else {
                    if (entity != null) {
                        //Logger.getGlobal().info("remove entity");
                        coordEntities.remove(player);
                        entity.remove();
                    }
                }
            //} else {
            //    hideMarker(player);
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
            /*RayTraceResult result = event.getPlayer().rayTraceEntities(5, true);
Logger.getGlobal().info("Interact ray trace: " + result);
            if (result != null && result.getHitEntity() != null) {
Logger.getGlobal().info("Interact ray trace hit: " + result.getHitEntity());
                handleInteract(event.getPlayer(), result.getHitEntity());
            }*/
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
        /*if(entity.equals(map.getMapEntity())) {
            Marker marker = markers.get(player);
            if(marker != null) {
                marker.handleInteract(player);
            }
        }*/
    }

}
