package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WarpItemMarker extends ItemMarker {

    private static final float size = 0.5f;

    private final WarpData warpData;

    public WarpItemMarker(Map map, WarpData warpData) {
        super(map, createPosition(map, warpData), getItem(),
                new Transformation(new Vector3f(0,0,0),
                new Quaternionf().rotateX((float)Math.toRadians(90)),
                new Vector3f(size, size, size),
                new Quaternionf(0,0,0,1)));
        //createMarkerEntity(map, item);
        this.warpData = warpData;
    }

    private static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(0);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleInteract(Player player) {
        player.sendMessage(Component.text("Click to warp to "+warpData.getName())
                .clickEvent((ClickEvent.runCommand("/warp "+warpData.getName()))));
    }

}
