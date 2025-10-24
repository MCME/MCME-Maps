package com.mcmiddleearth.mapInteraction.map;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.marker.*;
import com.mcmiddleearth.mapInteraction.warp.MyWarpDBConnector;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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

public class Map {

    private final ConfigurationSection mapConfig, worldConfig;

    private Interaction mapEntity;
    private Interaction activationEntity, activationModelEntity, previousPageEntity, nextPageEntity,
                        previousPageModelEntity, nextPageModelEntity;
    private ModelMarker activationMarker, previousMarker, nextMarker;
    private ModeledEntity mapAnimationEntity, nextAnimationEntity, previousAnimationEntity;
    private ActiveModel mapAnimationModel;

    private final double activationRadius;
    private final Location center;

    private boolean loaded;

    private final Set<PositionMarker> mapMarkerSet = new HashSet<>();

    private List<String> pages;
    private int currentPage = -1;

    private final double xWorldMin;
    private final double xWorldMax;
    private final double zWorldMin;
    private final double zWorldMax;
    private Transformation transformation;
    private final Transformation.Rotation rotation;

    private MapDisplay listener;

    public Map(ConfigurationSection mapConfig, ConfigurationSection worldConfig) {
        loaded = false;
        this.mapConfig = mapConfig;
        this.worldConfig = worldConfig;
        activationRadius = worldConfig.getDouble("activationRange",10);

        ConfigurationSection worldSection = worldConfig.getConfigurationSection("world_coordinates");

        assert worldSection!=null;

        rotation = Transformation.Rotation.valueOf(worldConfig.getString("rotation","NONE"));

        ConfigurationSection worldPos1 = worldSection.getConfigurationSection("pos1");
        ConfigurationSection worldPos2 = worldSection.getConfigurationSection("pos2");

        assert worldPos1 != null;
        assert worldPos2 != null;

        xWorldMin = worldPos1.getDouble("x");
        xWorldMax = worldPos2.getDouble("x");
        zWorldMin = worldPos1.getDouble("z");
        zWorldMax = worldPos2.getDouble("z");

        World world = Bukkit.getWorld(Objects.requireNonNull(worldConfig.getString("world")));
        center = new Location(world, (xWorldMax + xWorldMin) / 2,
                worldSection.getDouble("y"),
                (zWorldMax + zWorldMin) / 2);

        assert world != null;

        ConfigurationSection pagesConfig = mapConfig.getConfigurationSection("pages");
        if(pagesConfig!=null) {
            ArrayList<PageId> ids = new ArrayList<>();
            for (String pageName : pagesConfig.getKeys(false)) {
                ConfigurationSection pageConfig = pagesConfig.getConfigurationSection(pageName);
                if (pageConfig != null) {
                    ids.add(new PageId(pageName, pageConfig.getInt("no", 0)));
                }
            }
            pages = ids.stream().sorted(Comparator.comparingInt(PageId::no)).map(pageId -> pageId.name).toList();
        }
        // add saved location
        // add loadMap and UnloadMap (= remove) methods.
    }

    public void loadMap() {
        ConfigurationSection activationSection = worldConfig.getConfigurationSection("activation_coordinates");
        if (activationSection != null) {

            double xSize = xWorldMax - xWorldMin;
            double zSize = zWorldMax - zWorldMin;

            World world = center.getWorld();

            mapEntity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
            mapEntity.setInteractionHeight((float) 0.1);
            mapEntity.setInteractionWidth((float) Math.max(xSize, zSize));
            mapEntity.setPersistent(false);

            activationEntity = loadControlEntity(activationSection, world, rotation);
            activationModelEntity = loadControlEntity(activationSection, world, rotation);
            nextPageModelEntity = loadControlEntity(activationSection, world, rotation);
            previousPageModelEntity = loadControlEntity(activationSection, world, rotation);

            ConfigurationSection nextSection = worldConfig.getConfigurationSection("next_coordinates");
            ConfigurationSection previousSection = worldConfig.getConfigurationSection("previous_coordinates");

            if(nextSection != null && previousSection!= null) {
                nextPageEntity = loadControlEntity(nextSection, world, rotation);
                previousPageEntity = loadControlEntity(previousSection, world, rotation);
            }

            mapAnimationEntity = ModelEngineAPI.createModeledEntity(activationModelEntity);
            mapAnimationModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint("book_and_map"));
            mapAnimationEntity.addModel(mapAnimationModel, true);
            nextAnimationEntity = ModelEngineAPI.createModeledEntity(nextPageModelEntity);
            ActiveModel nextAnimationModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint("book_page_right"));
            nextAnimationEntity.addModel(nextAnimationModel, true);
            previousAnimationEntity = ModelEngineAPI.createModeledEntity(previousPageModelEntity);
            ActiveModel previousAnimationModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint("book_page_left"));
            previousAnimationEntity.addModel(previousAnimationModel, true);

            activationMarker = new ModelMarker(activationEntity, "book_transparent", rotation, this::open);
            nextMarker = new ModelMarker(nextPageModelEntity, "book_page_right", rotation, this::nextPage);
            previousMarker = new ModelMarker(previousPageModelEntity, "book_page_left", rotation, this::previousPage);

            listener = new MapDisplay(this);
            Bukkit.getPluginManager().registerEvents(listener, MapsPlugin.getInstance());

            loaded = true;
        } else {
            MapsPlugin.getInstance().getMcmeLogger().warn("Invalid map configuration.");
        }
    }

    private void nextPage() {
        getMapAnimationModel().getAnimationHandler().forceStopAnimation("page" + (currentPage));
        currentPage++;
        if (currentPage > getPages().size() - 1) {
            currentPage = 0;
        }
        loadPage(currentPage);
        getMapAnimationModel().getAnimationHandler().playAnimation("page" + (currentPage), 0, 2, 1, true);
    }

    private void previousPage() {
        getMapAnimationModel().getAnimationHandler().forceStopAnimation("page" + (currentPage));
        currentPage--;
        if (currentPage < 0) {
            currentPage = getPages().size() - 1;
        }
        loadPage(currentPage);
        getMapAnimationModel().getAnimationHandler().playAnimation("page" + (currentPage), 0, 2, 1, true);
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

            ConfigurationSection pagesConfig = mapConfig.getConfigurationSection("pages");
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
                    xWorldMin, zWorldMin, xWorldMax, zWorldMax, rotation);

            ConfigurationSection linkConfig = pageConfig.getConfigurationSection("links");
            if(linkConfig != null) {
                for(String linkTarget : linkConfig.getKeys(false)) {
                    ConfigurationSection section = linkConfig.getConfigurationSection(linkTarget);
                    if(section != null) {
                        int x = section.getInt("x");
                        int z = section.getInt("z");
                        double radius = getMarkerRadius(section);
                        Position  position = new Position(this).setMapPosition(x, z);
                        TextMarker marker = new PageMarker(this, position, linkTarget);
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
                    //Logger.getGlobal().info("warps: " + warpSection.getKeys(false).size());
                    for (String warpName : warpSection.getKeys(false)) {
                        //Logger.getGlobal().info("Warp name: " + warpName);
                        ConfigurationSection section = warpSection.getConfigurationSection(warpName);
                        WarpData warp = dbConnector.getWarp(warpName);
                        if (warp != null) {
                            WarpItemMarker marker = new WarpItemMarker(this, warp,
                                                      (float) mapConfig.getDouble("warp_visualizer_radius", 0.1));
                            mapMarkerSet.add(marker);
                            if (section != null) {
                                marker.setPriority(section.getInt("priority", 0));
                                marker.setRadius(getMarkerRadius(section));
                            }
                        }
                    }
                } catch (Exception ex) {
                    MapsPlugin.getInstance().getMcmeLogger().error("Error while loading warps. ", ex);
                } finally {
                    dbConnector.disconnect();
                }
            }).schedule(0, TimeUnit.SECONDS);
        }
    }

    public void open() {
        loadPage(0);
        mapAnimationModel.getAnimationHandler().playAnimation("open", 0, 2, 1, true);
        activationMarker.open();
        nextMarker.open();
        previousMarker.open();
    }

    public void close() {
        mapAnimationModel.getAnimationHandler().forceStopAllAnimations();
        mapAnimationModel.getAnimationHandler().playAnimation("idle", 0, 2, 1, true);
        activationMarker.close();
        nextMarker.close();
        previousMarker.close();
        clearPage();
    }
    private double getMarkerRadius(ConfigurationSection section) {
        return section.getDouble("radius", mapConfig.getDouble("marker_radius", 0.1));
    }

    public void clearPage() {
        mapMarkerSet.forEach(marker -> {
            if(marker.getEntity() != null) {
                marker.getEntity().remove();
            }
        });
        mapMarkerSet.clear();
        currentPage = -1;
    }

    public void unloadMap() {
        loaded = false;
        clearPage();
        if(mapEntity != null) {
            mapEntity.remove();
        }
        mapMarkerSet.clear();
        if(listener != null) {
            listener.clear();
            HandlerList.unregisterAll(listener);
        }
        if(mapAnimationEntity != null) {
            mapAnimationEntity.markRemoved();
        }
        if(nextAnimationEntity != null) {
            nextAnimationEntity.markRemoved();
        }
        if(previousAnimationEntity != null) {
            previousAnimationEntity.markRemoved();
        }
        if(activationEntity != null) {
            activationEntity.remove();
        }
        if(activationModelEntity != null) {
            activationModelEntity.remove();
        }
        if(nextPageEntity != null) {
            nextPageEntity.remove();
        }
        if(previousPageEntity != null) {
            previousPageEntity.remove();
        }
        if(nextPageModelEntity != null) {
            nextPageModelEntity.remove();
        }
        if(previousPageModelEntity != null) {
            previousPageModelEntity.remove();
        }
        if(nextMarker!= null) {
            nextMarker.remove();
        }
        if(previousMarker != null) {
            previousMarker.remove();
        }
        if(nextMarker != null) {
            nextMarker.remove();
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

    public @NotNull ActiveModel getMapAnimationModel() {
        return mapAnimationModel;
    }

    public Location getCenter() {
        return mapEntity.getLocation();
    }

    public @NotNull RayTraceTarget getRayTraceTarget(Player player) {
        RayTraceResult result = player.getWorld()
                .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(),5,
                        entity -> (getCurrentPage()<0 && (entity.equals(getActivationEntity())))
                                || (getCurrentPage()>=0 && (entity.equals(getMapEntity())
                                || entity.equals(getPreviousPageEntity())
                                || entity.equals(getNextPageEntity()))));
        Position position = null;
        Marker marker = null;
        if(result != null && result.getHitEntity() != null) {
            if(result.getHitEntity().equals(mapEntity)) {
                position = new Position(this).setWorldPosition(result.getHitPosition().getX(),
                        result.getHitPosition().getZ());
                marker = getMarker(position);
            } else {
                if(result.getHitEntity().equals(activationEntity)) {
                    marker = activationMarker;
                } else if(result.getHitEntity().equals(nextPageEntity)) {
                    marker = nextMarker;
                } else if(result.getHitEntity().equals(previousPageEntity)) {
                    marker = previousMarker;
                }
            }
        }
        return new RayTraceTarget(marker, position);
    }

    public PositionMarker getMarker(Position position) {
        return mapMarkerSet.stream().filter(marker -> {
            Location markerLocation = new Location(getCenter().getWorld(), marker.getPosition().getWorldX(),
                                                         0, marker.getPosition().getWorldZ());
            return markerLocation.distance(new Location(markerLocation.getWorld(),
                                                    position.getWorldX(),0,
                                                    position.getWorldZ())) < marker.getRadius();
        }).min(Comparator.comparingInt(marker -> -((PositionMarker) marker).getPriority())
                .thenComparing(marker -> ((PositionMarker) marker).getRadius())).orElse(null);
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

    public Interaction loadControlEntity(ConfigurationSection section, World world, Transformation.Rotation rotation) {
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

        Interaction entity = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
        entity.setInteractionHeight(height);
        entity.setInteractionWidth(width);
        entity.setPersistent(false);
        switch(rotation) {
            case LEFT_90 -> entity.setRotation(-90,0);
            case RIGHT_90 -> entity.setRotation(90,0);
            case TURN_180 -> entity.setRotation(180,0);
        }
        return entity;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isInside(Chunk chunk) {
        if(chunk.getWorld().equals(center.getWorld())) {
            for(Chunk search: getChunkList()) {
                if(search.getChunkKey() == chunk.getChunkKey()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean areAllChunksLoaded() {
        Location min = center.clone();
        min.setX(xWorldMin);
        min.setZ(zWorldMin);
        Location max = center.clone();
        max.setX(xWorldMax);
        max.setZ(zWorldMax);
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();
        for(int i = minChunk.getX() - 1; i <= maxChunk.getX() + 1; i++) {
            for(int j = minChunk.getZ() - 1; j <= maxChunk.getZ() + 1; j++) {
                if (!min.getWorld().isChunkLoaded(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    private Set<Chunk> getChunkList() {
        Set<Chunk> result = new HashSet<>();
        if(center != null) result.add(center.getChunk());
        if(activationEntity != null) result.add(activationEntity.getChunk());
        if(nextPageEntity != null) result.add(nextPageEntity.getChunk());
        if(previousPageEntity != null) result.add(previousPageEntity.getChunk());
        return result;
    }

    private record PageId(String name, int no){}

    public record RayTraceTarget(Marker marker, Position position) {}
}
