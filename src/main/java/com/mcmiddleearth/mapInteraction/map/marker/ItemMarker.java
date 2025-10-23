package com.mcmiddleearth.mapInteraction.map.marker;

import com.mcmiddleearth.mapInteraction.map.Transformation;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

public class ItemMarker implements Marker {

    Interaction entity;
    ModeledEntity animationEntity;
    ActiveModel animationModel;

    public ItemMarker(Interaction entity, String model, Transformation.Rotation rotation) {
        this.entity = (Interaction) entity.getWorld()
                .spawnEntity(entity.getLocation(), EntityType.INTERACTION);
        this.entity.setInteractionWidth(entity.getInteractionWidth());
        this.entity.setInteractionHeight(entity.getInteractionHeight());
        switch(rotation) {
            case LEFT_90 -> this.entity.setRotation(-90,0);
            case RIGHT_90 -> this.entity.setRotation(90,0);
            case TURN_180 -> this.entity.setRotation(180,0);
        }
        //this.entity.setBillboard(entity.getBillboard());
        //this.entity.setTransformation(entity.getTransformation());

        animationEntity = ModelEngineAPI.createModeledEntity(this.entity);
        animationModel = ModelEngineAPI.createActiveModel(ModelEngineAPI.getBlueprint(model));
        animationEntity.addModel(animationModel, true);

        this.entity.setGlowing(true);
        this.entity.setVisibleByDefault(false);

    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void handleInteract(Player player) { }

    public void open() {
        animationModel.getAnimationHandler().forceStopAllAnimations();
        animationModel.getAnimationHandler().playAnimation("open", 0, 2, 1, true);
    }

    public void close() {
        animationModel.getAnimationHandler().forceStopAllAnimations();
        animationModel.getAnimationHandler().playAnimation("idle", 0, 2, 1, true);
    }

    public void remove() {
        animationEntity.markRemoved();
        entity.remove();
    }
}
