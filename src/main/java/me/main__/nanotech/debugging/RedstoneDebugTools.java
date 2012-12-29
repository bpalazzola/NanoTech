package me.main__.nanotech.debugging;

import me.main__.nanotech.TickGetter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedstoneDebugTools implements Listener {
    private static final class DebugEntry {
        private final WeakReference<Player> player;
        private final String name;

        private DebugEntry(final Player player, final String name) {
            this.player = new WeakReference<Player>(player);
            this.name = name;
        }

        public static Player getPlayer(DebugEntry e) {
            return e != null ? e.player.get() : null;
        }
    }
    private final TickGetter tickGetter;
    private final Map<Location, DebugEntry> debuggers;
    private final Set<String> names;

    public RedstoneDebugTools(final TickGetter tickGetter) {
        this.tickGetter = tickGetter;
        this.debuggers = new HashMap<Location, DebugEntry>();
        this.names = new HashSet<String>();
    }

    public void addDebugger(String name, Player player, Block block) {
        debuggers.put(block.getLocation(), new DebugEntry(player, name));
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        DebugEntry entry = debuggers.get(event.getBlock().getLocation());
        Player p = DebugEntry.getPlayer(entry);
        if (p != null)
            p.sendMessage(String.format("[%d] %s: %d => %d", tickGetter.getCurrentTick(), entry.name,
                    event.getOldCurrent(), event.getNewCurrent()));
        else if (entry != null) {
            // expired
            debuggers.remove(event.getBlock().getLocation());
            names.remove(entry.name);
        }
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        List<Location> unloading = new LinkedList<Location>();
        for (Location l : debuggers.keySet())
            if (l.getWorld() == event.getWorld())
                unloading.add(l);

        for (Location l : unloading)
            names.remove(debuggers.remove(l).name);
    }

    public boolean removeDebugger(final String name) {
        Location loc = null;
        for (Map.Entry<Location, DebugEntry> entry : debuggers.entrySet())
            if (entry.getValue().name.equals(name)) {
                loc = entry.getKey();
                break;
            }

        names.remove(name);

        return debuggers.remove(loc) != null;
    }

    public List<String> matchStart(final String name, final Player player) {
        List<String> matches = new LinkedList<String>();
        for (Map.Entry<Location, DebugEntry> entry : debuggers.entrySet())
            if (entry.getValue().name.toLowerCase().startsWith(name.toLowerCase())
                    && entry.getValue().player.get() == player)
                matches.add(entry.getValue().name);
        return matches;
    }
}
