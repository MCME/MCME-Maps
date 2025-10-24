package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemMarker extends PositionMarker {

    private final ItemDisplay entity;

    public ItemMarker(Map map, Position position, ItemStack item, Transformation transformation) {
        super(map, position);
        entity = (ItemDisplay) map.getCenter().getWorld()
                .spawnEntity(new Location(map.getCenter().getWorld(),getPosition().getWorldX(),
                        map.getCenter().getY()+0.01,
                        getPosition().getWorldZ()), EntityType.ITEM_DISPLAY);
        //markerEntity.setBillboard(Display.Billboard.CENTER);
        //markerEntity.text(getText());
        entity.setItemStack(item);
        float size = 0.5f;
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
