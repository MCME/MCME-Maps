package com.mcmiddleearth.mapInteraction.map;

import com.google.gson.JsonParseException;
import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.warp.MyWarpDBConnector;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Map {

    private final Interaction entity;

    private final MyWarpDBConnector dbConnector;

    private final Set<WarpData> warpDataSet = new HashSet<>();

    private final Transformation transformation;

    public Map(ConfigurationSection config) {
        ConfigurationSection worldSection = config.getConfigurationSection("world_coordinates");
        ConfigurationSection mapSection = config.getConfigurationSection("map_coordinates");
        assert mapSection != null;
        ConfigurationSection  mapPos1 = mapSection.getConfigurationSection("pos1");
        ConfigurationSection  mapPos2 = mapSection.getConfigurationSection("pos2");
        assert worldSection != null;
        ConfigurationSection  worldPos1 = worldSection.getConfigurationSection("pos1");
        ConfigurationSection  worldPos2 = worldSection.getConfigurationSection("pos2");
        assert mapPos1 != null;
        assert mapPos2 != null;
        assert worldPos1 != null;
        assert worldPos2 != null;

        double xWorldMin = worldPos1.getDouble("x");
        double xWorldMax = worldPos2.getDouble("x");
        double zWorldMin = worldPos1.getDouble("z");
        double zWorldMax = worldPos2.getDouble("z");

        double xMapMin = mapPos1.getDouble("x");
        double xMapMax = mapPos2.getDouble("x");
        double zMapMin = mapPos1.getDouble("z");
        double zMapMax = mapPos2.getDouble("z");

        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString("world")));
        Location center = new Location(world, (xWorldMax + xWorldMin)/2,
                                                 worldSection.getDouble("y"),
                                              (zWorldMax + zWorldMin)/2);
        double xSize = xWorldMax - xWorldMin;
        double zSize = zWorldMax - zWorldMin;

        assert world != null;
        entity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
        entity.setInteractionHeight((float) 0.1);
        entity.setInteractionWidth((float) Math.max(xSize, zSize));
        transformation = new Transformation(xMapMin, zMapMin, xMapMax, zMapMax,
                                                    xWorldMin, zWorldMin, xWorldMax, zWorldMax);

        dbConnector = new MyWarpDBConnector(config.getConfigurationSection("myWarp_Database"));
        MapsPlugin.getInstance().getTask(()-> {
            try {
                ConfigurationSection warpSection = config.getConfigurationSection("warps");
                assert warpSection != null;
Logger.getGlobal().info("warps: "+warpSection.getKeys(false).size());
                for(String warpName: warpSection.getKeys(false)) {
Logger.getGlobal().info("Warp name: "+warpName);
                    ConfigurationSection section = warpSection.getConfigurationSection(warpName);
                    WarpData warp = dbConnector.getWarp(warpName);
                    if(warp != null) {
                        createWarpEntity(warp);
                        warpDataSet.add(warp);
Logger.getGlobal().info("Warp loaded: " + warpName + " " + warp.getPosition().getX() + " " + warp.getPosition().getZ());
                        if (section != null) {
                            warp.setPriority(section.getInt("priority", 0));
                            warp.setRadius(section.getInt("radius", config.getInt("warp_radius", 100)));
                            try {
                                warp.setMessage(GsonComponentSerializer.gson().deserialize(section.getString("message", "{}")));
                            } catch (JsonParseException ex) {
                                MapsPlugin.getInstance().getMcmeLogger().warn("Error while reading warp message");
                            }
                        }
                    }
                }
            } catch(Exception ex) {
                MapsPlugin.getInstance().getMcmeLogger().error("Error while loading warps. ", ex);
                dbConnector.disconnect();
            }
        }).schedule(7, TimeUnit.SECONDS);
    }

    public void remove() {
        if(entity != null) {
            entity.remove();
        }
        warpDataSet.forEach(warp -> {
            if(warp.getEntity() != null) {
                warp.getEntity().remove();
            }
        });
        warpDataSet.clear();
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

    public WarpData getWarp(Position position) {
        return warpDataSet.stream().filter(warp -> {
            Location warpLocation = warp.getPosition().toLocation(getCenter().getWorld());
            warpLocation.setY(0);
            return warpLocation.distance(new Location(warpLocation.getWorld(),
                                                    position.getMapX(),0,
                                                    position.getMapZ())) < warp.getRadius();
        }).min(Comparator.comparingInt(warp -> -((WarpData) warp).getPriority())
                .thenComparing(warp -> ((WarpData) warp).getRadius())).orElse(null);
    }

    private void createWarpEntity(WarpData warp) {
        TextDisplay warpEntity = (TextDisplay) getCenter().getWorld()
                .spawnEntity(new Location(getCenter().getWorld(),
                             getTransformation().getWorldX(warp.getPosition().getX()),
                             getCenter().getY()+1,
                             getTransformation().getWorldZ(warp.getPosition().getZ())), EntityType.TEXT_DISPLAY);
Logger.getGlobal().info("Entity: "+warpEntity.getLocation());
        warpEntity.setBillboard(Display.Billboard.CENTER);
        warpEntity.text(Component.text(warp.getName()));
        float size = 0.5f;
        warpEntity.setTransformation(new org.bukkit.util.Transformation(new Vector3f(0,0,0),
                new Quaternionf(0,0,0,1),
                new Vector3f(size, size, size),
                new Quaternionf(0,0,0,1)));
        warpEntity.setVisibleByDefault(false);
        warp.setEntity(warpEntity);
    }
}
