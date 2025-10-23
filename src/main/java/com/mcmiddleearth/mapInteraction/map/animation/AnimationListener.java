package com.mcmiddleearth.mapInteraction.map.animation;

import com.mcmiddleearth.mapInteraction.MapsPlugin;
import com.mcmiddleearth.mapInteraction.map.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.logging.Logger;


public class AnimationListener implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(EquipmentSlot.HAND.equals(event.getHand())) {
            //RayTraceResult result = event.getPlayer().rayTraceEntities(5, true);
            Player player  = event.getPlayer();
            for(Map map: MapsPlugin.getPlugin().getMapManager().getMaps().values()) {
                RayTraceResult result = player.getWorld()
todo: always use raytracing even for entityInteractEvent -> better selection of entities.
                        .rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 5,
                                entity -> (map.getCurrentPage()<0 && (entity.equals(map.getActivationEntity())))
                                        || (map.getCurrentPage()>=0 && (entity.equals(map.getMapEntity())
                                                                     || entity.equals(map.getPreviousPageEntity())
                                                                     || entity.equals(map.getNextPageEntity()))));

                Logger.getGlobal().info("Interact ray trace: " + result);
                if (result != null && result.getHitEntity() != null) {
                    Logger.getGlobal().info("Interact ray trace hit: " + result.getHitEntity());
                    handleInteract(result.getHitEntity());
                }
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
         handleInteract(event.getRightClicked());
    }

    private void handleInteract(Entity clicked) {
        //check if map entity
        //then activate map (load Page)
        //also start Timer to periodically check player in range for deactivation.
//Logger.getGlobal().info("clicked pos: "+event.getClickedPosition());
Logger.getGlobal().info("with Clicked: "+clicked.hashCode() + "at: "+clicked.getLocation());
        for(Map map: MapsPlugin.getPlugin().getMapManager().getMaps().values()) {
//Logger.getGlobal().info("Check map at: "+map.getCenter());
Logger.getGlobal().info("with Activation: "+ map.getActivationEntity().hashCode() + "at: "+map.getActivationEntity().getLocation());
//Logger.getGlobal().info("with next: "+map.getNextPageEntity().hashCode() + "at: "+map.getNextPageEntity().getLocation());
//Logger.getGlobal().info("with previous: "+map.getPreviousPageEntity().hashCode() + "at: "+map.getPreviousPageEntity().getLocation());
            if(clicked.equals(map.getActivationEntity())
                    || clicked.equals(map.getNextPageEntity())
                    || clicked.equals(map.getPreviousPageEntity())) {
//Logger.getGlobal().info("match");
                int currentPage = map.getCurrentPage();
//Logger.getGlobal().info("page: "+map.getCurrentPage());
                if(currentPage == -1
                        && (clicked.equals(map.getActivationEntity())
                            || clicked.equals(map.getNextPageEntity())
                            || clicked.equals(map.getPreviousPageEntity()))) {
//Logger.getGlobal().info("open");
                    map.open();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (clicked.getWorld()
                                    .getNearbyEntitiesByType(Player.class, map.getCenter(), map.getActivationRadius()).isEmpty()) {
                                /*map.getAnimationModel().getAnimationHandler().forceStopAnimation("open");
                                for(int i = 0; i < map.getPages().size(); i++) {
                                    map.getAnimationModel().getAnimationHandler().forceStopAnimation("page"+i);
                                }*/
                                map.close();
//map.getMapAnimationModel().getAnimationHandler().getAnimations().forEach(((name, anim) -> Logger.getGlobal().info(name)));
                                cancel();
//Logger.getGlobal().info("close");
                            }
                        }
                    }.runTaskTimer(MapsPlugin.getPlugin(), 40, 40);
                } else if(clicked.equals(map.getPreviousPageEntity())) {
                    map.getMapAnimationModel().getAnimationHandler().forceStopAnimation("page"+(currentPage));
                    currentPage--;
                    if(currentPage < 0) {
                        currentPage = map.getPages().size()-1;
                    }
                    map.loadPage(currentPage);
                    map.getMapAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0, 2, 1, true);
//Logger.getGlobal().info("previous ");
                } else if(clicked.equals(map.getNextPageEntity())) {
                    map.getMapAnimationModel().getAnimationHandler().forceStopAnimation("page"+(currentPage));
                    currentPage++;
                    if(currentPage > map.getPages().size()-1) {
                        currentPage = 0;
                    }
                    map.loadPage(currentPage);
                    map.getMapAnimationModel().getAnimationHandler().playAnimation("page"+(currentPage), 0, 2, 1, true);
//Logger.getGlobal().info("next ");
                }
                return;

            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        //???
    }
}
