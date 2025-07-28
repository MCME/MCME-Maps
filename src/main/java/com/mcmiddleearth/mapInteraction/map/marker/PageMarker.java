package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import net.kyori.adventure.text.Component;

public class PageMarker extends Marker {

    private final String targetPage;

    public PageMarker(Map map, Position position, String targetPage) {
        super(position);
        this.targetPage = targetPage;
        createMarkerEntity(map);
    }

    public String getTargetPage() {
        return targetPage;
    }

    @Override
    public Component getText() {
        return Component.text(targetPage);
    }
}
