package com.mcmiddleearth.mapInteraction.map;

import com.google.gson.JsonParseException;
import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.marker.Marker;
import com.mcmiddleearth.mapInteraction.map.marker.PageMarker;
import com.mcmiddleearth.mapInteraction.map.marker.WarpMarker;
import com.mcmiddleearth.mapInteraction.warp.MyWarpDBConnector;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Map {

    private final ConfigurationSection config;

    private Interaction mapEntity, activationEntity, previousPageEntity, nextPageEntity;
    private ModeledEntity animationEntity;
    private ActiveModel animationModel;
    //private BoundingBox nextButton, previousButton;

    private final double activationRadius;

    private final Set<Marker> mapMarkerSet = new HashSet<>();

    private List<String> pages;
    private int currentPage = -1;

    private double xWorldMin, xWorldMax, zWorldMin, zWorldMax;
    private Transformation transformation;

    private MapDisplay listener;

    public Map(ConfigurationSection mapConfig, ConfigurationSection worldConfig) {
        this.config = mapConfig;
        // add saved location
        // add loadMap and UnloadMap (= remove) methods.
        activationRadius = worldConfig.getDouble("activationRange",10);
        ConfigurationSection pagesConfig = mapConfig.getConfigurationSection("pages");
        ConfigurationSection worldSection = worldConfig.getConfigurationSection("world_coordinates");
        ConfigurationSection activationSection = worldConfig.getConfigurationSection("activation_coordinates");
        if (pagesConfig != null && worldSection != null && activationSection != null) {

            ConfigurationSection worldPos1 = worldSection.getConfigurationSection("pos1");
            ConfigurationSection worldPos2 = worldSection.getConfigurationSection("pos2");

            assert worldPos1 != null;
            assert worldPos2 != null;

            xWorldMin = worldPos1.getDouble("x");
            xWorldMax = worldPos2.getDouble("x");
            zWorldMin = worldPos1.getDouble("z");
            zWorldMax = worldPos2.getDouble("z");

            World world = Bukkit.getWorld(Objects.requireNonNull(worldConfig.getString("world")));
            Location center = new Location(world, (xWorldMax + xWorldMin) / 2,
                    worldSection.getDouble("y"),
                    (zWorldMax + zWorldMin) / 2);

            assert world != null;

            double xSize = xWorldMax - xWorldMin;
            double zSize = zWorldMax - zWorldMin;

            mapEntity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
            mapEntity.setInteractionHeight((float) 0.1);
            mapEntity.setInteractionWidth((float) Math.max(xSize, zSize));
            mapEntity.setPersistent(true);

            ArrayList<PageId> ids = new ArrayList<>();
            for(String pageName: pagesConfig.getKeys(false)) {
                ConfigurationSection pageConfig = pagesConfig.getConfigurationSection(pageName);
                if(pageConfig!=null) {
                    ids.add(new PageId(pageName, pageConfig.getInt("no",0)));
                }
            }
            pages = ids.stream().sorted(Comparator.comparingInt(PageId::no)).map(pageId -> pageId.name).toList();

            //loadPage(0);
            //don't load page on creation
            //instead create One interaction entity
            // Animation depends on clicked position (activation, pageleft pageright)

            activationEntity = loadControlEntity(activationSection, world);
Logger.getGlobal().info("activation Entiry height: "+activationEntity.getInteractionHeight());
Logger.getGlobal().info("activation Entiry width: "+activationEntity.getInteractionWidth());
Logger.getGlobal().info("activation Entiry pos: "+activationEntity.getLocation());

            ConfigurationSection nextSection = worldConfig.getConfigurationSection("next_coordinates");
            ConfigurationSection previousSection = worldConfig.getConfigurationSection("previous_coordinates");

            if(nextSection != null && previousSection!= null) {
Logger.getGlobal().info("Load next and previous");
                nextPageEntity = loadControlEntity(nextSection, world);
                previousPageEntity = loadControlEntity(previousSection, world);
            }

            animationEntity = ModelEngineAPI.createModeledEntity(activationEntity);
            animationModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint("book_and_map"));
            animationEntity.addModel(animationModel, true);

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
            clearPage();
            String pageName = pages.get(page);
            currentPage = page;

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

            ConfigurationSection paddingSection = pageConfig.getConfigurationSection("padding");
            if (paddingSection != null) {

                xWorldMin = xWorldMin + paddingSection.getDouble("left",0);
                xWorldMax = xWorldMax - paddingSection.getDouble("right",0);
                zWorldMin = zWorldMin + paddingSection.getDouble("top",0);
                zWorldMax = zWorldMax - paddingSection.getDouble("bottom",0);
            }

            transformation = new Transformation(xMapMin, zMapMin, xMapMax, zMapMax,
                    xWorldMin, zWorldMin, xWorldMax, zWorldMax);

            ConfigurationSection linkConfig = pageConfig.getConfigurationSection("links");
            if(linkConfig != null) {
                for(String linkTarget : linkConfig.getKeys(false)) {
                    ConfigurationSection section = linkConfig.getConfigurationSection(linkTarget);
                    if(section != null) {
                        int x = section.getInt("x");
                        int z = section.getInt("z");
                        double radius = getMarkerRadius(section);
                        Position  position = new Position(this).setMapPosition(x, z);
                        Marker marker = new PageMarker(this, position, linkTarget);
                        marker.setRadius(radius);
                        mapMarkerSet.add(marker);
                    }
                }
            }
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
//Logger.getGlobal().info("Warp loaded: " + warpName + " " + warp.getPosition().getX() + " " + warp.getPosition().getZ());
                            if (section != null) {
                                marker.setPriority(section.getInt("priority", 0));
                                marker.setRadius(getMarkerRadius(section));
                                try {
                                    marker.setMessage(GsonComponentSerializer.gson().deserialize(section.getString("message", "{\"text\":\"\"}")));
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

    private double getMarkerRadius(ConfigurationSection section) {
        return section.getDouble("radius", config.getDouble("marker_radius", 0.1));
    }

    public void clearPage() {
        mapMarkerSet.forEach(marker -> {
            if(marker.getEntity() != null) {
                marker.getEntity().remove();
            }
        });
        currentPage = -1;
    }

    public void remove() {
        clearPage();
        if(mapEntity != null) {
            mapEntity.remove();
        }
        mapMarkerSet.clear();
        if(listener != null) {
            listener.clear();
            HandlerList.unregisterAll(listener);
        }
        if(animationEntity != null) {
            animationEntity.markRemoved();
        }
        if(activationEntity != null) {
            activationEntity.remove();
        }
        if(nextPageEntity != null) {
            nextPageEntity.remove();
        }
        if(previousPageEntity != null) {
            previousPageEntity.remove();
        }
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public @NotNull Entity getMapEntity() {
        return mapEntity;
    }

    public @NotNull Entity getActivationEntity() {
        return activationEntity;
    }

    public Entity getPreviousPageEntity() {
        return previousPageEntity;
    }

    public Entity getNextPageEntity() {
        return nextPageEntity;
    }

    public @NotNull ActiveModel getAnimationModel() {
        return animationModel;
    }

    public Location getCenter() {
        return mapEntity.getLocation();
    }

    public Position getTargetPosition(Player player) {
        RayTraceResult result = player.getWorld()
                .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),5,
                                  entity -> entity == this.mapEntity);
        if(result != null) {
            return new Position(this).setWorldPosition(result.getHitPosition().getX(),
                                                                        result.getHitPosition().getZ());
        }
        return null;
    }

    public Marker getMarker(Position position) {
        return mapMarkerSet.stream().filter(marker -> {
            Location markerLocation = new Location(getCenter().getWorld(), marker.getPosition().getWorldX(),
                                                         0, marker.getPosition().getWorldZ());
            return markerLocation.distance(new Location(markerLocation.getWorld(),
                                                    position.getWorldX(),0,
                                                    position.getWorldZ())) < marker.getRadius();
        }).min(Comparator.comparingInt(marker -> -((Marker) marker).getPriority())
                .thenComparing(marker -> ((Marker) marker).getRadius())).orElse(null);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public double getActivationRadius() {
        return activationRadius;
    }

    public List<String> getPages() {
        return pages;
    }

    private BoundingBox readBoundingBox(@Nullable ConfigurationSection section) {
        assert section != null;
        ConfigurationSection pos1 = section.getConfigurationSection("pos1");
        ConfigurationSection pos2 = section.getConfigurationSection("pos2");
        assert pos1 != null;
        assert pos2 != null;
        return new BoundingBox(pos1.getDouble("x",0),
                pos1.getDouble("y",0),
                pos1.getDouble("z",0),
                pos2.getDouble("x",0),
                pos2.getDouble("y",0),
                pos2.getDouble("z",0));
    }

    public Interaction loadControlEntity(ConfigurationSection section, World world) {
        assert section != null;
        ConfigurationSection pos1 = section.getConfigurationSection("pos1");
        ConfigurationSection pos2 = section.getConfigurationSection("pos2");

        assert pos1 != null;
        assert pos2 != null;

        double xMin = pos1.getDouble("x");
        double xMax = pos2.getDouble("x");
        double yMin= pos1.getDouble("y");
        double yMax = pos2.getDouble("y");
        double zMin= pos1.getDouble("z");
        double zMax = pos2.getDouble("z");

        Location center = new Location(world, (xMin + xMax) / 2,
                (yMin + yMax) / 2,
                (zMin + zMax) / 2);
        float width = (float) Math.max(zMax-zMin,xMax-xMin);
        float height = (float) (yMax - yMin);
        //nextButton = readBoundingBox(worldConfig.getConfigurationSection("next_coordinates"));
        //previousButton = readBoundingBox(worldConfig.getConfigurationSection("previous_coordinates"));

        world.getNearbyEntitiesByType(Interaction.class, center,Math.max(width/2, height/2)).forEach(Entity::remove);

        Interaction entity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
        entity.setInteractionHeight(height);
        entity.setInteractionWidth(width);
        entity.setPersistent(true);
        return entity;
    }

/*    public boolean isPreviousPageButton(@NotNull Vector clickedPosition) {
Logger.getGlobal().info("activation Entiry height: "+activationEntity.getInteractionHeight());
Logger.getGlobal().info("activation Entiry width: "+activationEntity.getInteractionWidth());
Logger.getGlobal().info("activation Entiry pos: "+activationEntity.getLocation());
        Vector scaled = new Vector(clickedPosition.getX()*activationEntity.getInteractionWidth(),
                                   clickedPosition.getY()*activationEntity.getInteractionHeight(),
                                   clickedPosition.getZ()*activationEntity.getInteractionWidth());
        Vector absolutePosition = activationEntity.getLocation().add(scaled).toVector();
        Logger.getGlobal().info("clicked absolute: "+absolutePosition);
        Logger.getGlobal().info("Bounding box: "+previousButton);
        Logger.getGlobal().info("inside: "+ previousButton.contains(absolutePosition));
        return previousButton.contains(absolutePosition);
    }

    public boolean isNextPageButton(@NotNull Vector clickedPosition) {
Logger.getGlobal().info("activation Entiry height: "+activationEntity.getInteractionHeight());
Logger.getGlobal().info("activation Entiry width: "+activationEntity.getInteractionWidth());
Logger.getGlobal().info("activation Entiry pos: "+activationEntity.getLocation());
        Vector scaled = new Vector(clickedPosition.getX()*activationEntity.getInteractionWidth(),
                clickedPosition.getY()*activationEntity.getInteractionHeight(),
                clickedPosition.getZ()*activationEntity.getInteractionWidth());
        Vector absolutePosition = activationEntity.getLocation().add(scaled).toVector();
        Logger.getGlobal().info("clicked absolute: "+absolutePosition);
        Logger.getGlobal().info("Bounding box: "+nextButton);
        Logger.getGlobal().info("inside: "+ nextButton.contains(absolutePosition));
        return nextButton.contains(absolutePosition);
    }*/

    private record PageId(String name, int no){}
}
