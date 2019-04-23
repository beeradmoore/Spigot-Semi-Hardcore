package com.ftlz.spigot.semihardcore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;

import net.md_5.bungee.api.ChatColor;
 
public class PlayerMoveListener implements Listener
{
    //private JSONArray _deathData;
    private HashMap<String, DeathData> _deathLocations;
    private HashMap<String, Timer> _deathTimers;

    private App _app;
    private final String DeathLocationsFilename = "SH-DeathLocations.json";
    private static PlayerMoveListener _instance;

    public static PlayerMoveListener Instance()
    {
        return _instance;
    }

    public PlayerMoveListener(App app)
    {
        _app = app;
        _deathLocations = new HashMap<String, DeathData>();
        _deathTimers = new HashMap<String, Timer>();
        loadData();
        _instance = this;
    }

    @EventHandler
    public void onEntityDeath(PlayerDeathEvent event)
    {
        _app.getLogger().info("onEntityDeath");

        Player player = event.getEntity();

        // Player was killed by the timer to respawn.
        DeathData deathData = _deathLocations.get(player.getName());
        if (deathData != null)
        {
            event.setDeathMessage(null);
            deathData.setIsRespawning(true);
            saveData();
            return;
        }

        int deathDuration = _app.getConfig().getInt("death-duration", 21600);

        if (event.getEntity().getKiller() instanceof Player) {
            deathDuration = _app.getConfig().getInt("pvp-death-duration", 60);
        }


        _deathLocations.put(player.getName(), new DeathData(player, deathDuration));

        startTimer(player.getName(), deathDuration);

        saveData();
    }

    private int getCurrentUnixTimestamp()
    {
        return (int)(System.currentTimeMillis() / 1000L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event)
    {
        _app.getLogger().info("onPlayerRespawn");

        Player player = event.getPlayer();
       
        _app.getServer().getScheduler().scheduleSyncDelayedTask(_app, new Runnable() {
            public void run() {
                if (player.getGameMode() == GameMode.SPECTATOR)
                {
                    DeathData deathData = _deathLocations.get(player.getName());
                    if (deathData == null)
                    {
                        _app.getLogger().info("DeathData was not found for " + player.getName());
                        respawnPlayer(player);
                        return;
                    }

                    // Lock the user down.
                    if (deathData.getRespawnTime() > getCurrentUnixTimestamp())
                    {
                        player.teleport(deathData.getDeathLocation());
                        player.setFlySpeed(0);
                        player.setWalkSpeed(0);
                        displayRespawnCountdown(player, deathData);
                    }
                    else
                    {
                        respawnPlayer(player);
                    }
                }
            }
        }, 2l);

       
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        _app.getLogger().info("onPlayerQuitEvent");

        String playerName = event.getPlayer().getName();
        cancelTimer(playerName);
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event)
    {
        _app.getLogger().info("onPlayerJoinEvent");

        Player player = event.getPlayer();

      
        int deathDuration = _app.getConfig().getInt("death-duration", 21600);
        String displayTime = secondsToDisplay(deathDuration);
        player.sendMessage("" + ChatColor.RED + "" + ChatColor.BOLD + "NOTE:" + ChatColor.RESET + "" + ChatColor.RED + " A death will result in you being a ghost for " + displayTime + ".");
 
        
        if (player.getGameMode() == GameMode.SPECTATOR)
        {
            DeathData deathData = _deathLocations.get(player.getName());

            if (deathData == null)
            {
                respawnPlayer(player);
                return;
            }
            
            if (deathData.shouldStartRespawnTimer())
            {
                displayRespawnCountdown(player, deathData);
                startTimer(player.getName(), deathData.respawnTimerDuration());
            }
            else
            {
                respawnPlayer(player);
            }
        }
    }
    
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event)
    {
        if (event.getCause() == TeleportCause.SPECTATE)
        {
            event.setCancelled(true);
            event.getPlayer().setSpectatorTarget(null);
        }
    }

    /*
    private void displayRespawnCountdown(Player player)
    {
        displayRespawnCountdown(player, null);
    }
    */

    private void displayRespawnCountdown(Player player, DeathData deathData)
    {
        if (player.isOnline())
        {
            if (deathData == null)
            {
                deathData = _deathLocations.get(player.getName());
            }

            if (deathData != null)
            {
                int secondsUntilRespawn = deathData.getRespawnTime() - getCurrentUnixTimestamp();
                if (secondsUntilRespawn > 0)
                {
                    String respawnTime = secondsToDisplay( secondsUntilRespawn);
                    player.sendMessage("" + ChatColor.RED + "Respawn in " + respawnTime + ".");
                }
            }
        }
    }

    public String secondsToDisplayFullTimestamp(int timestamp)
    {
        return secondsToDisplay(timestamp - getCurrentUnixTimestamp());
    }

    public String secondsToDisplay(int seconds)
    { 
        int remainingSeconds = seconds;

        int minutes = remainingSeconds / 60;
        remainingSeconds -= (minutes * 60);

        int hours = minutes / 60;
        minutes -= (hours * 60);

        String output = "";

        if (hours > 0)
        {
            output += "" + hours + ((hours == 1) ? " hour" : " hours");
        }

        if (minutes > 0)
        {
            if (output != "")
            {
                output += ", "; 
            }
            output += "" + minutes + ((minutes == 1) ? " minute" : " minutes");
        }

        if (remainingSeconds > 0)
        {
            if (output != "")
            {
                output += ", "; 
            }
            output += "" + remainingSeconds + ((remainingSeconds == 1) ? " second" : " seconds");
        }

        return output;
    }

    private void loadData()
    {
        _app.getLogger().info("loadData");
        FileReader fileReader = null;
        try
        {
            File file = new File(DeathLocationsFilename);
            if (file.exists())
            {
                if (file.canRead())
                {
                    final GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.registerTypeAdapter(Location.class, new LocationAdapter());
                    gsonBuilder.setPrettyPrinting();        
                    final Gson gson = gsonBuilder.create();
                   
                    fileReader = new FileReader(file);

                    Type type = new TypeToken<HashMap<String, DeathData>>(){}.getType();
                    _deathLocations = gson.fromJson(fileReader, type);
                }
                else
                {
                    throw new Exception("Config exists but we can't read it.");
                }
            }  
        }
        catch (Exception err)
        {
            _app.getLogger().info("ERROR (loadData): " + err.getMessage());
            _deathLocations = new HashMap<String, DeathData>();
        }
        finally
        {
            if (fileReader != null)
            {
                try
                {
                    fileReader.close();
                }
                catch (Exception err)
                {
                    _app.getLogger().info("ERROR (loadData): " + err.getMessage());
                }
                fileReader = null;
            }
        }
        if(_deathLocations == null || !(_deathLocations instanceof HashMap))
        {
            _deathLocations = new HashMap<String, DeathData>();
        }
    }

    private void saveData()
    {
        _app.getLogger().info("saveData");

        FileOutputStream fileOutputStream = null;
        try
        {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Location.class, new LocationAdapter());
            gsonBuilder.setPrettyPrinting();        
            final Gson gson = gsonBuilder.create();
            String jsonData = gson.toJson(_deathLocations);

            File file = new File(DeathLocationsFilename);
            fileOutputStream = new FileOutputStream(file);
            FileLock fileLock = fileOutputStream.getChannel().lock();
            if (fileLock != null)
            {
                fileOutputStream.write(jsonData.getBytes());
            }
        }
        catch (Exception err)
        {
            _app.getLogger().info("ERROR (saveData): " + err.getMessage());
        }
        finally
        {
            if (fileOutputStream != null)
            {
                try
                {
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
                catch (Exception err)
                {
                    _app.getLogger().info("ERROR (saveData): " + err.getMessage());
                }
            }
        }
    }

    private void cancelTimer(String playerName)
    {
        if (_deathTimers.containsKey(playerName))
        {
            Timer timer = _deathTimers.get(playerName);
            if (timer != null)
            {
                timer.cancel();
                timer = null;
            }
            _deathTimers.remove(playerName);
        }
    }

    private void startTimer(String playerName, int seconds)
    {
        _app.getLogger().info(playerName);
        _app.getLogger().info(String.format("%s", seconds));
        // If it exists, kill it.
        cancelTimer(playerName);

        TimerTask task = new TimerTask()
        {
            public void run()
            {
                _app.getLogger().info("TimerTaskFired");
                String playerName = Thread.currentThread().getName().replace("-DeathTimer", "");
                _app.getLogger().info(playerName);
                _deathTimers.remove(playerName);
                Player player = _app.getServer().getPlayer(playerName);

                _app.getServer().getScheduler().scheduleSyncDelayedTask(_app, new Runnable()
                {
                    public void run()
                    {
                        DeathData deathData = _deathLocations.get(player.getName());
                        if (deathData != null)
                        {
                            _app.getLogger().info("Death data is not null");
                            // Set is respawning to true and kill the player. 
                            // When the player hits "respawn" it will call the onPlayerRespawn event
                            deathData.setIsRespawning(true);
                            saveData();
                            player.setHealth(0);
                            player.setGameMode(GameMode.SPECTATOR);
                            return;
                        }
                        else
                        {
                            // Death data not found, lets force respawn.
                            respawnPlayer(player);
                        }
                    }
                });
                
            }
        };
        Timer timer = new Timer(playerName+"-DeathTimer");
        timer.schedule(task, seconds * 1000L);
        _deathTimers.put(playerName, timer);
    }

    private void respawnPlayer(Player player)
    {
        _app.getLogger().info("respawnPlayer");

        if (player == null)
        {
            return;
        }

        // Should be cleaned up, but lets be sure we clean it up.
        cancelTimer(player.getName());

        // If player isn't online we don't do anything.
        if (player.isOnline() == false)
        {
            return;
        }
        
        _app.getServer().getScheduler().scheduleSyncDelayedTask(_app, new Runnable()
        {
            public void run()
            {
                _deathLocations.remove(player.getName());
                saveData();

                Location spawnLocation = player.getBedSpawnLocation();

                // Players spawn locaiton was not found, send them back to world spawn.
                if (spawnLocation == null)
                {
                    spawnLocation = _app.getServer().getWorlds().get(0).getSpawnLocation();
                }

                // Fix things that would prevent them moving.
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);  

                // Teleport to correct poition.
                player.teleport(spawnLocation);

                // Puts you out incase you were on fire.
                player.setFireTicks(0);

                // Removes any potion effects you had.
                for (PotionEffect effect : player.getActivePotionEffects())
                {
                    player.removePotionEffect(effect.getType());
                }
                
                // Set the game mode back to survival.
                player.setGameMode(GameMode.SURVIVAL);

                // Reapwn the player.
                player.spigot().respawn();

                //Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "effect " + player.getName() + " clear");

            }
        }, 2l);
    }

    public boolean respawnPlayer(String playerName)
    {
        Player player = _app.getServer().getPlayer(playerName);
        if(player.getGameMode() == GameMode.SPECTATOR)
        {
            player.sendMessage("You're being commanded to rise!");
            cancelTimer(playerName);
            _deathLocations.get(player.getName()).respawningPlayer();
            respawnPlayer(player);
            return true;
        }
        return false;
    }

    public boolean adjustPlayersRespawnSchedule(String playerName, int newDuration)
    {
        // Cancel the timer
        // Start a new timer (if they're online)
        // Update the deathdata in memory
        // Save to file
        Player player = _app.getServer().getPlayer(playerName);
        DeathData playerDeathData = _deathLocations.get(playerName);
        if(player == null || player.getGameMode() != GameMode.SPECTATOR)
        {
            return false;
        }

        cancelTimer(playerName);
        if(player.isOnline())
        {
            startTimer(playerName, newDuration);
        }
        int newRespawnTime = newDuration + getCurrentUnixTimestamp();
        _deathLocations.get(playerName).setRespawnTime(newRespawnTime);
        _app.getLogger().info(secondsToDisplayFullTimestamp(playerDeathData.getRespawnTime()));
        saveData();
        player.sendMessage("Your respawn has been altered by fate, you shall respawn at " + secondsToDisplayFullTimestamp(playerDeathData.getRespawnTime()));
        return true;
    }

    public ArrayList<DeathData> getCurrentlyDead()
    {
        ArrayList<DeathData> theWarDead = new ArrayList<DeathData>();
        for (HashMap.Entry<String, DeathData> entry : _deathLocations.entrySet()) {
            //System.out.println(entry.getKey() + " = " + entry.getValue());
            if(entry.getValue().currentlyDead())
            {
                theWarDead.add(entry.getValue());
            }
        }

        Collections.sort(theWarDead, new Comparator<DeathData>(){
            public int compare(DeathData d1, DeathData d2) {
                return d2.getRespawnTime() - d1.getRespawnTime();
            }
        });

        return theWarDead;
    }
}