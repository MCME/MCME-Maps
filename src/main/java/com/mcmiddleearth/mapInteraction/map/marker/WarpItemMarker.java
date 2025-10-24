package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.connect.util.ConnectUtil;
import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.warp.WarpData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WarpItemMarker extends ItemMarker {

    private final WarpData warpData;

    public WarpItemMarker(Map map, WarpData warpData, Float visualizerRadius) {
        super(map, createPosition(map, warpData), getItem(),
                new Transformation(new Vector3f(),
                new Quaternionf(),//.rotateX((float)Math.toRadians(90)),
                new Vector3f(visualizerRadius),
                new Quaternionf()));
        this.warpData = warpData;
    }

    private static ItemStack getItem() {
        ItemStack item = new ItemStack(Material.PINK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(1);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void handleInteract(Player player) {
        ConnectUtil.teleportPlayer(player, "world", "world", warpData.getLocation());
        player.sendMessage(Component.text("Welcome to '").color(NamedTextColor.AQUA)
                   . append(Component.text(warpData.getName()).color(NamedTextColor.GREEN))
                    .append(Component.text("', "+player.getName()+".").color(NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("Click "))
                    .append(Component.text("here ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                                                         .clickEvent(ClickEvent.runCommand("/hub")))
                    .append(Component.text("to teleport back to the MCME hub.").color(NamedTextColor.AQUA)));
    }

    @Override
    public void setRadius(double radius) {
        super.setRadius(radius);
        Transformation transf = ((ItemDisplay)getEntity()).getTransformation();
        ((ItemDisplay)getEntity()).setTransformation(new Transformation(transf.getTranslation(),transf.getLeftRotation(),
                                                                   new Vector3f((float)radius/2),transf.getRightRotation()));
    }
}
