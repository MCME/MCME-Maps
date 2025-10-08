package com.mcmiddleearth.mapInteraction.map.animation;

import com.ticxo.modelengine.api.events.AnimationEndEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class AnimationListener implements Listener {

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        //check if map entity
        //then activate map (load Page)
        //also start Timer to periodically check player in range for deactivation.
    }

    @EventHandler
    public void onAnimationEnd(AnimationEndEvent event) {
        //???
    }
}
