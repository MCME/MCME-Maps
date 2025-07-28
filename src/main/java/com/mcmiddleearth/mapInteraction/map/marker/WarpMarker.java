package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;

public class WarpMarker extends com.mcmiddleearth.mapInteraction.map.marker.Marker {

    private final WarpData warpData;

    public WarpMarker(Map map, WarpData warpData) {
        super(createPosition(map, warpData));
        this.warpData = warpData;
        createMarkerEntity(map);
    }

    private static Position createPosition(Map map, WarpData warpData) {
        Position position = new Position(map);
        position.setMapPosition(warpData.getPosition().getX(), warpData.getPosition().getZ());
        return position;
    }

    @Override
    public Component getText() {
        return Component.text(warpData.getName());
    }
}
