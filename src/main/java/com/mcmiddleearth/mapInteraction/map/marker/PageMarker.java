package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PageMarker extends TextMarker {

    private final String targetPage;

    public PageMarker(Map map, Position position, String targetPage) {
        super(map, position);
        this.targetPage = targetPage;
    }

    public String getTargetPage() {
        return targetPage;
    }

    @Override
    public Component getText() {
        return Component.text((targetPage!=null?targetPage:""));
    }

    @Override
    public void handleInteract(Player player) {
        getMap().loadPage(targetPage);
    }


}
