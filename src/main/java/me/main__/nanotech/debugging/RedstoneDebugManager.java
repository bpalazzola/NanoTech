package me.main__.nanotech.debugging;

import me.main__.nanotech.NanoTech;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class RedstoneDebugManager {
    private final NanoTech plugin;
    private final RedstoneDebugTools tools;

    public RedstoneDebugManager(final NanoTech plugin, final RedstoneDebugTools tools) {
        this.plugin = plugin;
        this.tools = tools;

        plugin.getServer().getPluginManager().registerEvents(tools, plugin);
        plugin.getCommand("redstonedebug").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (args.length == 2) {
                        if (args[0].equalsIgnoreCase("setDebugPoint")) {
                            Block target = player.getTargetBlock(null, 5);
                            tools.addDebugger(args[1], player, target);
                            player.sendMessage("Debug point added.");
                            return true;
                        } else if (args[0].equalsIgnoreCase("removeDebugPoint")) {
                            if (tools.removeDebugger(args[1]))
                                player.sendMessage("Debugger successfully removed.");
                            else player.sendMessage("Failed to remove the specified debugger.");
                            return true;
                        }
                    }
                    return false;
                } else {
                    sender.sendMessage("You have to be a Player to use this command!");
                    return true;
                }
            }
        });
        plugin.getCommand("redstonedebug").setTabCompleter(new TabCompleter() {
            @Override
            public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
                if (sender instanceof Player) {
                    if (args.length == 1) {
                        if (args[0].startsWith("s"))
                            return Collections.singletonList("setDebugPoint");
                        if (args[0].startsWith("r"))
                            return Collections.singletonList("removeDebugPoint");
                    } else if (args.length == 2) {
                        if (args[0].startsWith("r"))
                            return tools.matchStart(args[1], (Player)sender);
                    }
                }
                return null;
            }
        });
    }
}
