package com.cjcrafter.vivecraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ConstructTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        List<String> list = new ArrayList<String>();
        if (arg0 instanceof Player) {
            if (arg3.length >= 1) {
                for (Cmd cmd : ViveCommand.getCommands()) {
                    if (cmd.getCommand().startsWith(arg3[0]))
                        list.add(cmd.getCommand());
                }
                return list;
            }
        }
        return null;
    }
}
