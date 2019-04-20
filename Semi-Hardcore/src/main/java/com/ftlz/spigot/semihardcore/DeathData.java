package com.ftlz.spigot.semihardcore;

import com.google.gson.annotations.SerializedName;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DeathData
{
    @SerializedName("name")
    private String _playerName;

    @SerializedName("death_location")
    private Location _deathLocation;

    @SerializedName("spawn_location")
    private Location _spawnLocation;

    @SerializedName("death_time")
    private int _deathTime;

    @SerializedName("respawn_time")
    private int _respawnTime;
    
    public DeathData(Player player, int deathDuration)
    {
        _playerName = player.getName();
        _deathLocation = player.getLocation();
        _spawnLocation = player.getBedSpawnLocation();
        _deathTime = (int)(System.currentTimeMillis() / 1000L);
        _respawnTime = _deathTime + deathDuration;
    }

    public String getPlayerName()
    {
        return _playerName;
    }

    public Location getDeathLocation()
    {
        return _deathLocation;
    }

    public Location getSpawnLocation()
    {
        return _spawnLocation;
    }

    public int getDeathTime()
    {
        return _deathTime;
    }

    public int getRespawnTime()
    {
        return _respawnTime;
    }
}