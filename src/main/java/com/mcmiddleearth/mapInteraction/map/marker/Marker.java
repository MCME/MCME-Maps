package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Map;
import com.mcmiddleearth.mapInteraction.map.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public abstract class Marker {

    private final Position position;
    private double radius;
    private Component message;
    private int priority;

    private TextDisplay entity;

    public Marker(Position position) {
        this.position = position;
    }

    public abstract Component getText();

    public abstract void handleInteract(Player player);

    protected void createMarkerEntity(Map map) {
        TextDisplay markerEntity = (TextDisplay) map.getCenter().getWorld()
                .spawnEntity(new Location(map.getCenter().getWorld(),getPosition().getWorldX(),
                        map.getCenter().getY()+1,
                        getPosition().getWorldZ()), EntityType.TEXT_DISPLAY);
        markerEntity.setBillboard(Display.Billboard.CENTER);
        markerEntity.text(getText());
        float size = 0.5f;
        markerEntity.setTransformation(new org.bukkit.util.Transformation(new Vector3f(0,0,0),
                new Quaternionf(0,0,0,1),
                new Vector3f(size, size, size),
                new Quaternionf(0,0,0,1)));
        markerEntity.setVisibleByDefault(false);
        entity =  markerEntity;
    }


    public Position getPosition() {
        return position;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public TextDisplay getEntity() {
        return entity;
    }

    public String getPlainText() {
        return PlainTextComponentSerializer.plainText().serialize(getText());
    }
}
