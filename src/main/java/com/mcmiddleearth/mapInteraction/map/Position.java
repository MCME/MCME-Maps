package com.mcmiddleearth.mapInteraction.map;

public class Position {

    private final Map map;

    private double mapX, mapZ, worldX, worldZ;

    public Position(Map map) {
        this.map = map;
    }

    public Position setMapPosition(double mapX, double mapZ) {
        this.mapX = mapX;
        this.mapZ = mapZ;
        Transformation transformation = map.getTransformation();
        this.worldX = transformation.getWorldX(mapX);
        this.worldZ = transformation.getWorldZ(mapZ);
        return this;
    }

    public Position setWorldPosition(double worldX, double worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        Transformation transformation = map.getTransformation();
        this.mapX = transformation.getMapX(worldX);
        this.mapZ = transformation.getMapZ(worldZ);
        return this;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldZ() {
        return worldZ;
    }

    public double getMapX() {
        return mapX;
    }

    public double getMapZ() {
        return mapZ;
    }
}
