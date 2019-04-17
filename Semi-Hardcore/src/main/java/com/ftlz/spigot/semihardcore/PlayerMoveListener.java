package com.ftlz.spigot.semihardcore;

import java.io.FileOutputStream;
import java.util.logging.Level;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.json.simple.JSONObject;
 
public class PlayerMoveListener implements Listener
{
    App _app;
    public PlayerMoveListener(App app)
    {
        _app = app;        
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event)
    {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR)
        {
            Location fromLocation = event.getFrom();
            Location toLocation = event.getTo();
            toLocation.setX(fromLocation.getX());
            toLocation.setY(fromLocation.getY());
            toLocation.setZ(fromLocation.getZ());
            //event.setFrom(fromLocation);
            event.setTo(toLocation);
            //event.setCancelled(true);
        }
    }
}