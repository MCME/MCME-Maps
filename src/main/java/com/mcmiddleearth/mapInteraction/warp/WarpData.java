/*
 * Copyright (C) 2019 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.mapInteraction.warp;

import com.mcmiddleearth.base.core.player.McmeProxyPlayer;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 *
 * @author Eriol_Eandur
 */
public class WarpData {
    
    private String server;
    private String world;
    private String name;
    private String location;
    private Vector warpPosition;
    private String welcomeMessage;
    private boolean visibleToEveryone;
    private UUID owner;
    private Set<UUID> invited = new HashSet<>();

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
        String[] locData = location.split(";");
        warpPosition = new Vector(Double.parseDouble(locData[0]),
                Double.parseDouble(locData[1]),
                Double.parseDouble(locData[2]));
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public boolean isVisible(McmeProxyPlayer player) {
        return visibleToEveryone || player.getUniqueId().equals(owner) || invited.contains(player.getUniqueId());
    }

    public void setInvited(Set<UUID> invitedPlayers) {
        invited = Objects.requireNonNullElseGet(invitedPlayers, HashSet::new);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setPublic(boolean isPublic) {
        visibleToEveryone = isPublic;
    }

    public Vector getPosition() {
        return warpPosition;
    }

}
