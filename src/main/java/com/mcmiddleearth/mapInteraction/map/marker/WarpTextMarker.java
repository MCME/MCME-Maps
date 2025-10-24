package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class WarpTextMarker extends TextMarker {

    private final WarpData warpData;

    public WarpTextMarker(Map map, WarpData warpData) {
        super(map, createPosition(map, warpData));
        this.warpData = warpData;
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
