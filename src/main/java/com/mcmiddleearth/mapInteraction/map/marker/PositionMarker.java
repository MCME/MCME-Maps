package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import com.mcmiddleearth.mapInteraction.warp.WarpData;

public abstract class PositionMarker implements Marker {

    private final Map map;
    private final Position position;
    private double radius;
    private int priority;

    public PositionMarker(Map map, Position position) {
        this.map = map;
        this.position = position;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Position getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Map getMap() {
        return map;
    }

    protected static Position createPosition(Map map, WarpData warpData) {
        return new Position(map).setMapPosition(warpData.getPosition().getX(), warpData.getPosition().getZ());
    }

}
