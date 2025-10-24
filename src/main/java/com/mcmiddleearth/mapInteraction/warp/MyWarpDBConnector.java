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

import com.mcmiddleearth.base.core.taskScheduling.Task;
import com.mcmiddleearth.mapInteraction.MapsPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Eriol_Eandur
 */
public class MyWarpDBConnector {

    private final String dbUser;
    private final String dbPassword;
    private final String dbName;
    private final String dbIp;
    private final int port;
    
    private Connection dbConnection;

    private PreparedStatement getWarp;

    private final File worldFile = new File(MapsPlugin.getInstance().getDataFolder(),"world.uuid");
        
    private final Map<String, String> worldUUID = new HashMap<>();
    
    private boolean connected = false;
    
    private final Task keepAliveTask;
    
    public MyWarpDBConnector() {
        ConfigurationSection config = MapsPlugin.getInstance().getConfig().getConfigurationSection("myWarp_Database");
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            MapsPlugin.getInstance().getMcmeLogger().error("MySQL driver not found!",e);
        }
        assert config != null;
        dbUser = (String) config.get("user");
        dbPassword = (String) config.get("password");
        dbName = (String) config.get("dbName");
        dbIp = (String) config.get("ip");
        port = (Integer) config.get("port",3306);

        loadWorldUUIDs();
        connect();
        keepAliveTask = MapsPlugin.getInstance().getTask(this::checkConnection);
        keepAliveTask.scheduleRepeating(60,60,TimeUnit.SECONDS);
    }
    
    public void disconnect() {
        connected = false;
        if(keepAliveTask!=null) {
            keepAliveTask.cancel();
        }
        try {
            if(dbConnection!=null) {
                dbConnection.close();
            }
        } catch (SQLException ex) {
            MapsPlugin.getInstance().getMcmeLogger().error("SQLException", ex);
        }
    }
    
    private void checkConnection() {
        try {
            if(connected && dbConnection.isValid(5)) {
                MapsPlugin.getInstance().getMcmeLogger().info("Successfully checked connection to myWarp database.");
                connected = true;
            } else {
                //throw new SQLException();
                if(dbConnection!=null) {
                    dbConnection.close();
                }
                MapsPlugin.getInstance().getMcmeLogger().warn("Reconnecting to myWarp database.");
                connect();
            }
        } catch (SQLException ex) {
            MapsPlugin.getInstance().getMcmeLogger().error("No DB connection!!",ex);
            connected = false;
        }
    }
    
    private void connect() {
        try {
            dbConnection = DriverManager.getConnection(
                    "jdbc:mysql://"+dbIp+":"+port+"/"+dbName,
                    dbUser, dbPassword);

            getWarp = dbConnection.prepareStatement("SELECT warp.name, warp.x, warp.y, warp.z, "
                    + "warp.pitch, warp.yaw, warp.welcome_message, warp.visits, "
                    + "warp.type, world.uuid, owner.uuid, invited.uuid "
                    + "FROM warp JOIN player AS owner ON warp.player_id = owner.player_id "
                    +           "JOIN world ON warp.world_id = world.world_id "
                    +           "LEFT JOIN warp_player_map ON warp.warp_id = warp_player_map.warp_id "
                    +           "LEFT JOIN player AS invited ON warp_player_map.player_id = invited.player_id "
                    + "WHERE warp.name REGEXP ? "
                    + "AND (warp.type = 1) "
                    + "ORDER BY warp.visits DESC");
            getWarp.setQueryTimeout(1);
            getWarp.setFetchSize(1);
            PreparedStatement getWarpList = dbConnection.prepareStatement("SELECT warp.warp_id, warp.name, warp.player_id, warp.type "
                    + "FROM warp");
            getWarpList.setQueryTimeout(1);
            PreparedStatement getPlayerList = dbConnection.prepareStatement("SELECT player.player_id, player.uuid FROM player");
            getPlayerList.setQueryTimeout(1);
            connected = true;
        } catch (SQLException ex) {
            MapsPlugin.getInstance().getMcmeLogger().error("SQLException", ex);
            connected = false;
        }
    }

    public WarpData getWarp(String name) {
        if(connected) {
            try {
                getWarp.setString(1, addWildcards(name));
                ResultSet result = getWarp.executeQuery();
                if(result.next()) {
                    String world = worldUUID.get(result.getString("world.uuid"));
                    if(world==null) {
                        //world unknown
                        world = "_unknown";
                    }
                    WarpData warp = new WarpData();
                    warp.setName(result.getString("warp.name"));
                    warp.setWorld(world);
                    warp.setServer(world);
                    warp.setWelcomeMessage(result.getString("warp.welcome_message"));
                    warp.setLocation(result.getDouble("warp.x")+";"
                                   + result.getDouble("warp.y")+";"
                                   + result.getDouble("warp.z")+";"
                                   + result.getFloat("warp.yaw")+";"
                                   + result.getFloat("warp.pitch"));

                    return warp;
                }
                result.close();
            } catch (SQLException ex) {
                MapsPlugin.getInstance().getMcmeLogger().error("SQLException", ex);
                connected = false;
            }
        }
        return null;
    }
    
    private String addWildcards(String name) {
        StringBuilder result = new StringBuilder();
        for(int i = 0; i<name.length();i++) {
            String sub = name.substring(i,i+1);
            if(sub.matches("[a-z]|[A-Z]")) {
                result.append("[").append(sub.toLowerCase()).append(sub.toUpperCase()).append("]");
            } else if(sub.matches("[ \\-]")) {
                result.append("[ |-]");
            }
        }
        return result.toString();
    }
    
    private void loadWorldUUIDs() {
        if(!worldFile.exists()) {
            return;
        }
        try(Scanner scanner = new Scanner(worldFile)) {
            worldUUID.clear();
            while(scanner.hasNext()) {
                String[] line = scanner.nextLine().split(";");
                worldUUID.put(line[0], line[1]);
            }
        } catch (FileNotFoundException ex) {
            MapsPlugin.getInstance().getMcmeLogger().error("FileNotFoundException", ex);

        }
    }

}


