package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PageMarker extends TextMarker {

    private final String targetPage;
    private final Map map;

    public PageMarker(Map map, Position position, String targetPage) {
        super(position);
        this.map = map;
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

    @Override
    public void handleInteract(Player player) {
        map.loadPage(targetPage);
    }


}
