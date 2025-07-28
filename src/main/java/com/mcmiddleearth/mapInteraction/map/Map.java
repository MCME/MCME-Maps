package com.mcmiddleearth.mapInteraction.map;

import com.google.gson.JsonParseException;
import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.marker.Marker;
import com.mcmiddleearth.mapInteraction.map.marker.WarpMarker;
import com.mcmiddleearth.mapInteraction.warp.MyWarpDBConnector;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Map {

    private final ConfigurationSection config;

    private Interaction entity;

    private final Set<Marker> mapMarkerSet = new HashSet<>();

    private List<String> pages;

    private int currentPage = 0;

    private double xWorldMin, xWorldMax, zWorldMin, zWorldMax;
    private Transformation transformation;

    private MapDisplay listener;

    public Map(ConfigurationSection config) {
        this.config = config;
        ConfigurationSection pagesConfig = config.getConfigurationSection("pages");
        ConfigurationSection worldSection = config.getConfigurationSection("world_coordinates");
        if (pagesConfig != null && worldSection != null) {

            ConfigurationSection worldPos1 = worldSection.getConfigurationSection("pos1");
            ConfigurationSection worldPos2 = worldSection.getConfigurationSection("pos2");

            assert worldPos1 != null;
            assert worldPos2 != null;

            xWorldMin = worldPos1.getDouble("x");
            xWorldMax = worldPos2.getDouble("x");
            zWorldMin = worldPos1.getDouble("z");
            zWorldMax = worldPos2.getDouble("z");

            World world = Bukkit.getWorld(Objects.requireNonNull(config.getString("world")));
            Location center = new Location(world, (xWorldMax + xWorldMin) / 2,
                    worldSection.getDouble("y"),
                    (zWorldMax + zWorldMin) / 2);

            assert world != null;

            double xSize = xWorldMax - xWorldMin;
            double zSize = zWorldMax - zWorldMin;

            entity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
            entity.setInteractionHeight((float) 0.1);
            entity.setInteractionWidth((float) Math.max(xSize, zSize));

            ArrayList<PageId> ids = new ArrayList<>();
            for(String pageName: pagesConfig.getKeys(false)) {
                ConfigurationSection pageConfig = pagesConfig.getConfigurationSection(pageName);
                if(pageConfig!=null) {
                    ids.add(new PageId(pageName, pageConfig.getInt("no",0)));
                }
            }
            pages = ids.stream().sorted(Comparator.comparingInt(PageId::no)).map(pageId -> pageId.name).toList();
            loadPage(0);
        } else {
            MapsPlugin.getInstance().getMcmeLogger().warn("Invalid map configuration.");
        }
    }

    public void loadPage(String name) {
        int pageNo = pages.indexOf(name);
        if(pageNo >= 0) {
            loadPage(pageNo);
        }
    }

    public void loadPage(int page) {
        if(page >= 0 && page < pages.size()) {
            currentPage = page;
            String pageName = pages.get(page);

            ConfigurationSection pagesConfig = config.getConfigurationSection("pages");
            assert pagesConfig != null;
            ConfigurationSection pageConfig =  pagesConfig.getConfigurationSection(pageName);
            assert pageConfig != null;
            ConfigurationSection mapSection = pageConfig.getConfigurationSection("map_coordinates");
            assert mapSection != null;
            ConfigurationSection mapPos1 = mapSection.getConfigurationSection("pos1");
            ConfigurationSection mapPos2 = mapSection.getConfigurationSection("pos2");

            assert mapPos1 != null;
            assert mapPos2 != null;
            double xMapMin = mapPos1.getDouble("x");
            double xMapMax = mapPos2.getDouble("x");
            double zMapMin = mapPos1.getDouble("z");
            double zMapMax = mapPos2.getDouble("z");

            double xWorldMin = this.xWorldMin;
            double xWorldMax = this.xWorldMax;
            double zWorldMin = this.zWorldMin;
            double zWorldMax = this.zWorldMax;

            ConfigurationSection worldSection = config.getConfigurationSection("world_coordinates");
            if (worldSection != null) {

                ConfigurationSection worldPos1 = worldSection.getConfigurationSection("pos1");
                ConfigurationSection worldPos2 = worldSection.getConfigurationSection("pos2");

                assert worldPos1 != null;
                assert worldPos2 != null;

                xWorldMin = worldPos1.getDouble("x");
                xWorldMax = worldPos2.getDouble("x");
                zWorldMin = worldPos1.getDouble("z");
                zWorldMax = worldPos2.getDouble("z");
            }

            transformation = new Transformation(xMapMin, zMapMin, xMapMax, zMapMax,
                    xWorldMin, zWorldMin, xWorldMax, zWorldMax);

            MapsPlugin.getInstance().getTask(() -> {
                MyWarpDBConnector dbConnector = new MyWarpDBConnector();
                try {
                    ConfigurationSection warpSection = pageConfig.getConfigurationSection("warps");
                    assert warpSection != null;
                    Logger.getGlobal().info("warps: " + warpSection.getKeys(false).size());
                    for (String warpName : warpSection.getKeys(false)) {
                        Logger.getGlobal().info("Warp name: " + warpName);
                        ConfigurationSection section = warpSection.getConfigurationSection(warpName);
                        WarpData warp = dbConnector.getWarp(warpName);
                        if (warp != null) {
                            Marker marker = new WarpMarker(this, warp);
                            mapMarkerSet.add(marker);
Logger.getGlobal().info("Warp loaded: " + warpName + " " + warp.getPosition().getX() + " " + warp.getPosition().getZ());
                            if (section != null) {
                                marker.setPriority(section.getInt("priority", 0));
                                marker.setRadius(section.getInt("radius", pageConfig.getInt("warp_radius", 100)));
                                try {
                                    marker.setMessage(GsonComponentSerializer.gson().deserialize(section.getString("message", "{}")));
                                } catch (JsonParseException ex) {
                                    MapsPlugin.getInstance().getMcmeLogger().warn("Error while reading warp message");
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    MapsPlugin.getInstance().getMcmeLogger().error("Error while loading warps. ", ex);
                } finally {
                    dbConnector.disconnect();
                }
                listener = new MapDisplay(this);
                Bukkit.getPluginManager().registerEvents(listener, MapsPlugin.getInstance());
            }).schedule(0, TimeUnit.SECONDS);
        }
    }

    public void remove() {
        if(entity != null) {
            entity.remove();
        }
        mapMarkerSet.forEach(warp -> {
            if(warp.getEntity() != null) {
                warp.getEntity().remove();
            }
        });
        mapMarkerSet.clear();
        if(listener != null) {
            listener.clear();
            HandlerList.unregisterAll(listener);
        }
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public @NotNull Entity getEntity() {
        return entity;
    }

    public Location getCenter() {
        return entity.getLocation();
    }

    public Position getTargetPosition(Player player) {
        RayTraceResult result = player.getWorld()
                .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),5,
                                  entity -> entity == this.entity);
        if(result != null) {
            return new Position(this).setWorldPosition(result.getHitPosition().getX(),
                                                                        result.getHitPosition().getZ());
        }
        return null;
    }

    public Marker getMarker(Position position) {
        return mapMarkerSet.stream().filter(marker -> {
            Location markerLocation = new Location(getCenter().getWorld(), marker.getPosition().getMapX(),
                                                         0, marker.getPosition().getMapZ());
            return markerLocation.distance(new Location(markerLocation.getWorld(),
                                                    position.getMapX(),0,
                                                    position.getMapZ())) < marker.getRadius();
        }).min(Comparator.comparingInt(marker -> -((Marker) marker).getPriority())
                .thenComparing(marker -> ((Marker) marker).getRadius())).orElse(null);
    }

   private record PageId(String name, int no){}
}
