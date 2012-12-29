package me.main__.nanotech;

import me.main__.nanotech.debugging.RedstoneDebugManager;
import me.main__.nanotech.debugging.RedstoneDebugTools;
import me.main__.nanotech.dirty.CraftBukkitHelper;
import me.main__.nanotech.irc.IRCController;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class NanoTech extends JavaPlugin implements Listener {
    //private Buscript buscript;
    private IRCController ircController;
    private RedstoneDebugManager redstoneDebugManager;
    private TickGetter tickGetter;

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(LightweightLocation.class);
        IRCController.registerClasses();
    }

    @Override
    public void onEnable() {
        // TODO: javascript IRCs
        //buscript = new Buscript(this);
        saveDefaultConfig();

        // craftbukkit.force is not in the default config!
        if (!CraftBukkitHelper.run(this, getConfig().getBoolean("craftbukkit.force"))) {
            getLogger().warning("Helper methods were NOT registered!");
            getLogger().warning("Removing blocks underneath IRC components (repeaters) WILL result in glitches!");
            getLogger().warning("RedstoneDebug will always show -1 as the current tick!");
        } else {
            tickGetter = CraftBukkitHelper.TICK_GETTER;
        }

        if (getConfig().getBoolean("debug.redstone-events"))
            getServer().getPluginManager().registerEvents(this, this);

        ircController = new IRCController(this, getConfig().getConfigurationSection("irc"),
                new File(getDataFolder(), "persistence"));

        redstoneDebugManager = new RedstoneDebugManager(this,
                new RedstoneDebugTools(tickGetter != null ? tickGetter : new TickGetter() {
            @Override
            public int getCurrentTick() {
                return -1;
            }
        }));
    }

    @Override
    public void onDisable() {
        ircController.saveIRCs();
        saveConfig();
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        sender.sendMessage("Not implemented!");
        return true;
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        getLogger().info(String.format("RedstoneEvent [%d => %d | x=%d y=%d z=%d]", event.getOldCurrent(),
                event.getNewCurrent(), event.getBlock().getX(), event.getBlock().getY(), event.getBlock().getZ()));
    }

    public IRCController getIRCController() {
        return ircController;
    }
}

