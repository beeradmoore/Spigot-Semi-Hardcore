package com.ftlz.spigot.semihardcore;

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
                }
            }
        }
        return true;
    }

    private boolean forceRespawn(String playerName)
    {
        return PlayerMoveListener.Instance().respawnPlayer(playerName);
    }
}