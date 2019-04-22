package com.ftlz.spigot.semihardcore;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandShc implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.isOp()) {
                if (command.getName().equalsIgnoreCase("shc")) {
                    if(args.length == 0)
                    {
                        player.sendMessage( "Command Missing");
                        return true;
                    }
                    String shcCommand = args[0].toLowerCase();
                    if(shcCommand.compareTo("respawn") == 0)
                    {
                        if(args.length == 2)
                        {
                            if(forceRespawn(args[1]))
                            {
                                player.sendMessage("Player respawned.");
                            }
                            else
                            {
                                player.sendMessage("This player is not dead...");
                            }
                        }
                    }
                    else if(shcCommand.compareTo("update") == 0)
                    {
                        if(args.length == 4)
                        {
                            String playerName = args[1];
                            String updateType = args[2];
                            int value = -1;
                            try
                            {
                                value = Integer.parseInt(args[3]);
                            }
                            catch(NumberFormatException e)
                            {
                                return false;
                            }
                            if(updatePlayerRespawnTimer(playerName, updateType, value))
                            {
                                player.sendMessage("Players respawn duration has been updated.");
                            }
                            else
                            {
                                player.sendMessage("Unable to update this players respawn time, it could be they're not dead or some part of the command was invalid.");
                            }
                        }
                    }
                    else if(shcCommand.compareTo("list") == 0)
                    {
                        ArrayList<String> messagesToPrint = new ArrayList<String>();
                        if(args.length == 1)
                        {
                            messagesToPrint = getCurrentlyDead();
                        }
                        else if(args.length == 2)
                        {
                            int value = 0;
                            try
                            {
                                value = Integer.parseInt(args[1]);
                            }
                            catch(NumberFormatException e)
                            {
                                return false;
                            }
                            messagesToPrint = getCurrentlyDead(value);
                        }
                        Iterator<String> i = messagesToPrint.iterator();
                        while (i.hasNext()) {
                            player.sendMessage(i.next());
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean forceRespawn(String playerName)
    {
        return PlayerMoveListener.Instance().respawnPlayer(playerName);
    }

    private boolean updatePlayerRespawnTimer(String playerName, String updateType, int value)
    {
        int seconds = value;
        if(updateType.compareTo("minutes") == 0)
        {
            seconds = value * 60;
        }
        else if(updateType.compareTo("hours") == 0)
        {
            seconds = value * 3600;
        }
        return PlayerMoveListener.Instance().adjustPlayersRespawnSchedule(playerName, seconds);
    }

    private ArrayList<String> getCurrentlyDead()
    {
        return getCurrentlyDead(0);
    }
    
    private ArrayList<String> getCurrentlyDead(int page)
    {
        int pageSize = 5;
        int fromIndex = page * pageSize;
        int toIndex = fromIndex + pageSize;
        ArrayList<String> deathMessages = new ArrayList<String>();
        ArrayList<String> messagesToPrint = new ArrayList<String>();
        ArrayList<DeathData> theWarDead = PlayerMoveListener.Instance().getCurrentlyDead();
        
        Iterator<DeathData> i = theWarDead.iterator();
        while (i.hasNext()) {
            DeathData d = i.next();
            deathMessages.add(String.format("%s will revive in: %s", d.getPlayerName(), PlayerMoveListener.Instance().secondsToDisplayFullTimestamp(d.getRespawnTime())));
        }

        if(fromIndex > deathMessages.size())
        {
            fromIndex = 0;
            toIndex = fromIndex + pageSize;
        }
        else if(fromIndex < 0)
        {
            fromIndex = 0;
            toIndex = fromIndex + pageSize;
        }

        if(toIndex > deathMessages.size())
        {
            toIndex = deathMessages.size();
        }
        
        if(deathMessages.size() > 0)
        {
            messagesToPrint.add(String.format("Page: [%s/%s]", (page+1), (int)(Math.ceil((float)theWarDead.size() / (float)pageSize))));
            messagesToPrint.addAll(deathMessages.subList(fromIndex, toIndex));
        }
        else 
        {
            messagesToPrint.add("Nothing here boss.");
        }
        return messagesToPrint;
    }
}