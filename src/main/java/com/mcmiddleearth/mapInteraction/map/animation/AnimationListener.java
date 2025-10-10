package com.mcmiddleearth.mapInteraction.map.animation;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.Map;
import com.ticxo.modelengine.api.events.AnimationEndEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.logging.Logger;


public class AnimationListener implements Listener {

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        //check if map entity
        //then activate map (load Page)
        //also start Timer to periodically check player in range for deactivation.
Logger.getGlobal().info("clicked pos: "+event.getClickedPosition());
Logger.getGlobal().info("with Clicked: "+event.getRightClicked().hashCode() + "at: "+event.getRightClicked().getLocation());
        for(Map map: MapsPlugin.getPlugin().getMapManager().getMaps().values()) {
//Logger.getGlobal().info("Check map at: "+map.getCenter());
Logger.getGlobal().info("with Activation: "+ map.getActivationEntity().hashCode() + "at: "+map.getActivationEntity().getLocation());
Logger.getGlobal().info("with next: "+map.getNextPageEntity().hashCode() + "at: "+map.getNextPageEntity().getLocation());
Logger.getGlobal().info("with previous: "+map.getPreviousPageEntity().hashCode() + "at: "+map.getPreviousPageEntity().getLocation());
            if(clicked.equals(map.getActivationEntity())
                    || clicked.equals(map.getNextPageEntity())
                    || clicked.equals(map.getPreviousPageEntity())) {
Logger.getGlobal().info("match");
                int currentPage = map.getCurrentPage();
Logger.getGlobal().info("page: "+map.getCurrentPage());
                if(currentPage == -1 && clicked.equals(map.getActivationEntity())) {
Logger.getGlobal().info("open");
                    map.loadPage(0);
                    map.getAnimationModel().getAnimationHandler().playAnimation("open", 0.1, 2, 1, true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (clicked.getWorld()
                                    .getNearbyEntitiesByType(Player.class, map.getCenter(), map.getActivationRadius()).isEmpty()) {
                                map.getAnimationModel().getAnimationHandler().forceStopAnimation("open");
                                //map.getAnimationModel().getAnimationHandler().playAnimation("idle", 0.1, 2, 1, true);
                                map.clearPage();
map.getAnimationModel().getAnimationHandler().getAnimations().forEach(((name, anim) -> Logger.getGlobal().info(name)));
                                cancel();
Logger.getGlobal().info("close");
                            }
                        }
                    }.runTaskTimer(MapsPlugin.getPlugin(), 40, 40);
                } else if(currentPage > 0 && clicked.equals(map.getPreviousPageEntity())) {
                    currentPage--;
                    map.loadPage(currentPage);
                    map.getAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0.1, 2, 1, true);
Logger.getGlobal().info("previous ");
                } else if(currentPage < map.getPages().size()-1  && clicked.equals(map.getNextPageEntity())) {
                    currentPage++;
                    map.loadPage(currentPage);
                    map.getAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0.1, 2, 1, true);
Logger.getGlobal().info("next ");
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
