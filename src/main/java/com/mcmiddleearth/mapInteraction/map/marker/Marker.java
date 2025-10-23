package com.mcmiddleearth.mapInteraction.map.marker;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface Marker {

    Entity getEntity();

    void handleInteract(Player player);
}
