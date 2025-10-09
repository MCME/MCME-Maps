package com.mcmiddleearth.mapInteraction.map.animation;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.Map;
import com.ticxo.modelengine.api.events.AnimationEndEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;


public class AnimationListener implements Listener {

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        //check if map entity
        //then activate map (load Page)
        //also start Timer to periodically check player in range for deactivation.
        for(Map map: MapsPlugin.getPlugin().getMapManager().getMaps().values()) {
            if(event.getRightClicked().equals(map.getActivationEntity())) {
                int currentPage = map.getCurrentPage();
                if(currentPage == -1) {
                    map.loadPage(0);
                    map.getAnimationModel().getAnimationHandler().playAnimation("open", 0.1, 2, 1, true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (event.getRightClicked().getWorld()
                                    .getNearbyEntitiesByType(Player.class, map.getCenter(), map.getActivationRadius()).isEmpty()) {
                                map.clearPage();
                                map.getAnimationModel().getAnimationHandler().playAnimation("idle", 0.1, 2, 1, true);
                                cancel();
                            }
                        }
                    }.runTaskTimer(MapsPlugin.getPlugin(), 40, 40);
                } else if(currentPage > 0 && isPreviousPageButton(event.getClickedPosition())) {
                    currentPage--;
                    map.loadPage(currentPage);
                    map.getAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0.1, 2, 1, true);

                } else if(currentPage < map.getPages().size()-1  && isNextPageButton(event.getClickedPosition())) {
                    currentPage++;
                    map.loadPage(currentPage);
                    map.getAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0.1, 2, 1, true);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onAnimationEnd(AnimationEndEvent event) {
        //???
    }
}
