package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public class ItemMarker extends PositionMarker {

    private final ItemDisplay entity;

    public ItemMarker(Map map, Position position, ItemStack item, Transformation transformation) {
        super(map, position);
        entity = (ItemDisplay) map.getCenter().getWorld()
                .spawnEntity(new Location(map.getCenter().getWorld(),getPosition().getWorldX(),
                        map.getCenter().getY()+0.01,
                        getPosition().getWorldZ()), EntityType.ITEM_DISPLAY);
        entity.setItemStack(item);
        entity.setTransformation(transformation);
        entity.setVisibleByDefault(false);
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void handleInteract(Player player) { }

}
