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

    @SerializedName("death_time")
    private int _deathTime;

    @SerializedName("respawn_time")
    private int _respawnTime;

    @SerializedName("is_respawning")
    private boolean _isRespawning;
    
    public DeathData(Player player, int deathDuration)
    {
        _playerName = player.getName();
        _deathLocation = player.getLocation();
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

    public int getDeathTime()
    {
        return _deathTime;
    }

    public int getRespawnTime()
    {
        return _respawnTime;
    }

    public boolean getIsRespawning()
    {
        return _isRespawning;
    }

    public boolean shouldStartRespawnTimer()
    {
        int timestamp = (int)(System.currentTimeMillis() / 1000L);
        int secondsUntilRespawn = _respawnTime - timestamp;
        return secondsUntilRespawn > 0;
    }

    public int respawnTimerDuration()
    {
        int timestamp = (int)(System.currentTimeMillis() / 1000L);
        return _respawnTime - timestamp;
    }

    public void setRespawnTime(int newRespawnTime)
    {
        _respawnTime = newRespawnTime;
    }

    public void setIsRespawning(boolean isRespawning)
    {
        _isRespawning = isRespawning;
    }

    public boolean currentlyDead()
    {
        int timestamp = (int)(System.currentTimeMillis() / 1000L);
        return _respawnTime > timestamp;
    }

    public void respawningPlayer()
    {
        _respawnTime = (int)(System.currentTimeMillis() / 1000L);
    }
}