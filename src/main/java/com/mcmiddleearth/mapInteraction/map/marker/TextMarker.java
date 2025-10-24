package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class TextMarker extends PositionMarker {

    //private Component message;

    private final TextDisplay entity;

    public TextMarker(Map map, Position position) {
        super(map, position);
        entity = (TextDisplay) map.getCenter().getWorld()
                .spawnEntity(new Location(map.getCenter().getWorld(),getPosition().getWorldX(),
                        map.getCenter().getY()+1,
                        getPosition().getWorldZ()), EntityType.TEXT_DISPLAY);
        entity.setBillboard(Display.Billboard.CENTER);
        entity.text(getText());
        float size = 0.5f;
        entity.setTransformation(new org.bukkit.util.Transformation(new Vector3f(0,0,0),
                new Quaternionf(0,0,0,1),
                new Vector3f(size, size, size),
                new Quaternionf(0,0,0,1)));
        entity.setVisibleByDefault(false);
    }

    public abstract Component getText();

    @Override
    public TextDisplay getEntity() {
        return entity;
    }

    //public String getPlainText() { return PlainTextComponentSerializer.plainText().serialize(getText()); }
}
