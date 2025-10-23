package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class WarpMarker extends TextMarker {

    private final WarpData warpData;

    public WarpMarker(Map map, WarpData warpData) {
        super(createPosition(map, warpData));
        this.warpData = warpData;
        createMarkerEntity(map);
    }

    private static Position createPosition(Map map, WarpData warpData) {
        return new Position(map).setMapPosition(warpData.getPosition().getX(), warpData.getPosition().getZ());
    }

    @Override
    public Component getText() {
        return Component.text(warpData.getName());
    }

    @Override
    public void handleInteract(Player player) {
        player.sendMessage(Component.text("Click to warp to "+warpData.getName())
                                    .clickEvent((ClickEvent.runCommand("/warp "+warpData.getName()))));
    }
}
