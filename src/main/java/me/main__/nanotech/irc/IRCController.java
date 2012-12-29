package me.main__.nanotech.irc;

import me.main__.nanotech.BlockChangeAPI;
import me.main__.nanotech.NanoTech;
import me.main__.nanotech.irc.loaders.JavaCircuitLoader;
import me.main__.nanotech.irc.loaders.SimpleMappingLoader;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class IRCController implements Listener {
    public static enum BuildStatus {
        READY, NOT_FOUND, NOT_RECTANGLE, TOO_SMALL, WRONG_START
    }

    public static final class IRCBuildData {
        private final BuildStatus status;
        private final BuiltCircuit circuit;

        public IRCBuildData(final BuildStatus status) {
            this(status, null);
        }

        public IRCBuildData(final BuiltCircuit circuit) {
            this(BuildStatus.READY, circuit);
        }

        private IRCBuildData(final BuildStatus status, final BuiltCircuit circuit) {
            this.status = status;
            this.circuit = circuit;
        }

        public BuildStatus result() {
            return status;
        }

        public BuiltCircuit circuit() {
            return circuit;
        }
    }

    private final NanoTech plugin;
    private final File storageFolder;
    private final Storage storage;
    private final ConversationFactory conversationFactory;
    private final Map<String, Set<BuiltCircuit>> circuits;
    private final Map<Location, WeakReference<BuiltCircuit>> blocks;
    private final Map<Location, WeakReference<BuiltCircuit>> outputs;
    private final Map<Location, WeakReference<BuiltCircuit>> inputs;

    private final ConfigurationSection config;
    private final WorldConfigStorage configStorage;

    public static void registerClasses() {
        ConfigurationSerialization.registerClass(BuiltCircuit.class);
    }

    public IRCController(final NanoTech plugin, final ConfigurationSection config, final File worldConfigFolder) {
        this.plugin = plugin;
        this.config = config;
        this.configStorage = new WorldConfigStorage(worldConfigFolder);
        this.storageFolder = new File(plugin.getDataFolder(), "IRCStorage");
        this.storage = new Storage(plugin, Arrays.asList(new SimpleMappingLoader(), new JavaCircuitLoader()));
        this.storage.load(storageFolder);
        this.circuits = new HashMap<String, Set<BuiltCircuit>>();
        this.blocks = new HashMap<Location, WeakReference<BuiltCircuit>>();
        this.outputs = new HashMap<Location, WeakReference<BuiltCircuit>>();
        this.inputs = new HashMap<Location, WeakReference<BuiltCircuit>>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("buildirc").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
                if (sender instanceof Player) {
                    Player player = (Player)sender;
                    if (args.length == 1) {
                        IRCController.IRCBuildData data;
                        switch ((data = build(player.getTargetBlock(null, 5), args[0])).result()) {
                            case NOT_FOUND:
                                player.sendMessage("The IRC '" + args[0] + "' was not found!");
                                break;
                            case NOT_RECTANGLE:
                            case WRONG_START:
                                player.sendMessage("A IRC needs a RECTANGLE out of iron blocks.");
                                player.sendMessage("Look at one and then try again.");
                                break;
                            case TOO_SMALL:
                                player.sendMessage("That's too small for this IRC!");
                                break;

                            case READY:
                                Conversation c = conversationFactory.buildConversation(player);
                                c.getContext().setSessionData("bc", data.circuit()); // stringly typed programming, I know...
                                c.begin();
                                break;
                        }
                        return true;
                    }
                    return false;
                } else {
                    sender.sendMessage("You have to be a player to use this command!");
                    return true;
                }
            }
        });
        plugin.getCommand("buildirc").setTabCompleter(new TabCompleter() {
            @Override
            public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
                if (args.length == 1) {
                    return storage.matchStart(args[0]);
                }
                return null;
            }
        });
        plugin.getCommand("loadircs").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
                if (args.length != 0)
                    return false;
                storage.load(storageFolder);
                return true;
            }
        });

        Map<Object, Object> initialData = new HashMap<Object, Object>();
        initialData.put("init", false);
        initialData.put("controller", this);
        conversationFactory = new ConversationFactory(plugin).withLocalEcho(false).withModality(true)
                .withFirstPrompt(OrientationSelectionPrompt.INSTANCE).withInitialSessionData(initialData);
        loadIRCs();
    }

    public File getIRCStorageFolder() {
        return storageFolder;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        List<?> list = configStorage.getConfig(event.getWorld().getName()).getList("circuits");
        if (list != null)
            for (Object obj : list)
                if (obj instanceof BuiltCircuit)
                    add((BuiltCircuit)obj);
                else plugin.getLogger().warning("Unknown object in the circuits list: " + obj);
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        Set<BuiltCircuit> set = circuits.remove(event.getWorld().getName());
        if (set != null) {
            for (BuiltCircuit bc : set)
                bc.sleep();
            configStorage.getConfig(event.getWorld().getName()).set("circuits", new ArrayList<BuiltCircuit>(set));
            configStorage.unload(event.getWorld().getName());
        }
    }

    private void loadIRCs() {
        for (World w : plugin.getServer().getWorlds())
            onWorldLoad(new WorldLoadEvent(w));
    }

    public void saveIRCs() {
        for (Map.Entry<String, Set<BuiltCircuit>> entry : circuits.entrySet())
            configStorage.getConfig(entry.getKey()).set("circuits", new ArrayList<BuiltCircuit>(entry.getValue()));
        configStorage.saveAll();
    }

    public void add(BuiltCircuit bc) {
        bc.activate();
        for (Block b : bc.getInputs())
            inputs.put(b.getLocation(), new WeakReference<BuiltCircuit>(bc));
        for (Block b : bc.getOutputs())
            outputs.put(b.getLocation(), new WeakReference<BuiltCircuit>(bc));
        for (Block b : bc.getBlocks())
            blocks.put(b.getLocation(), new WeakReference<BuiltCircuit>(bc));

        String world = bc.getBlocks()[0].getWorld().getName();
        if (!circuits.containsKey(world))
            circuits.put(world, new HashSet<BuiltCircuit>());
        circuits.get(world).add(bc);
        bc.run();
    }

    public IRCBuildData build(Block searchStartBlock, String name) {
        CircuitFactory irc = storage.getIRC(name);
        if (irc == null)
            return new IRCBuildData(BuildStatus.NOT_FOUND);

        if ((searchStartBlock == null) || (searchStartBlock.getType() != Circuit.MATERIAL))
            return new IRCBuildData(BuildStatus.WRONG_START);

        Set<Block> plain = getICPlain(searchStartBlock);

        int hx = Integer.MIN_VALUE;
        int lx = Integer.MAX_VALUE;
        int hz = Integer.MIN_VALUE;
        int lz = Integer.MAX_VALUE;

        for (Block b : plain) {
            if (b.getX() > hx)
                hx = b.getX();
            if (b.getX() < lx)
                lx = b.getX();

            if (b.getZ() > hz)
                hz = b.getZ();
            if (b.getZ() < lz)
                lz = b.getZ();
        }

        World w = searchStartBlock.getWorld();
        final int y = searchStartBlock.getY();
        final int lenz = abs(hz - lz);
        final int lenx = abs(hx - lx);
        Set<Location> requiredBlocks = new HashSet<Location>();
        for (int zOffset = 0; zOffset <= lenz; zOffset++)
            for (int xOffset = 0; xOffset <= lenx; xOffset++)
                requiredBlocks.add(new Location(w, lx + xOffset, y, lz + zOffset));

        for (Block b : plain) {
            if (!requiredBlocks.remove(b.getLocation()))
                throw new IllegalStateException(String.format("The plain contained %s but it's not in the stuff", b.getLocation()));
        }

        if (!requiredBlocks.isEmpty())
            return new IRCBuildData(BuildStatus.NOT_RECTANGLE);

        if (((lenz + 1) < max(irc.getInputs(), irc.getOutputs())) && ((lenx + 1) < max(irc.getInputs(), irc.getOutputs())))
            return new IRCBuildData(BuildStatus.TOO_SMALL);


        final BuiltCircuit bc = new BuiltCircuit(plain.toArray(new Block[plain.size()]), hx, lx, hz, lz, irc);
        return new IRCBuildData(bc);
    }

    private static Set<Block> getICPlain(Block start) {
        Set<Block> blocks = new HashSet<Block>();
        Set<Block> visited = new HashSet<Block>();
        Stack<Block> searching = new Stack<Block>();
        searching.push(start);
        blocks.add(start);
        while (!searching.empty()) {
            Block b = searching.pop();
            if (!visited.add(b))
                continue;
            getICPlain_helper(b.getRelative(BlockFace.NORTH), searching, blocks);
            getICPlain_helper(b.getRelative(BlockFace.SOUTH), searching, blocks);
            getICPlain_helper(b.getRelative(BlockFace.EAST),  searching, blocks);
            getICPlain_helper(b.getRelative(BlockFace.WEST),  searching, blocks);
        }
        return blocks;
    }

    private static void getICPlain_helper(final Block b, final Stack<Block> searching, final Set<Block> foundBlocks) {
        if (b.getType() == Circuit.MATERIAL) {
            foundBlocks.add(b);
            searching.push(b);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handleRedstone(BlockRedstoneEvent event) {
        Location loc = event.getBlock().getLocation();

        if (outputs.containsKey(loc) && outputs.get(loc).get() != null && outputs.get(loc).get().isBuilt()) {
            // block redstone changes for the outputs
            event.setNewCurrent(event.getOldCurrent());
            return;
        }

        WeakReference<BuiltCircuit> weakCircuit = inputs.get(loc);
        BuiltCircuit circuit = weakCircuit != null ? weakCircuit.get() : null;
        if (circuit != null && circuit.isBuilt())
            // trigger the update 1 tick later
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, circuit);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleBlockBreak(BlockBreakEvent event) {
        WeakReference<BuiltCircuit> weakCircuit = null;
        if (event.getBlock().getType() == Circuit.MATERIAL)
            weakCircuit = blocks.get(event.getBlock().getLocation());
        else if (event.getBlock().getType() == Material.REDSTONE_TORCH_OFF
                || event.getBlock().getType() == Material.REDSTONE_TORCH_ON)
            weakCircuit = outputs.get(event.getBlock().getLocation());
        else if (event.getBlock().getType() == Material.DIODE_BLOCK_OFF
                || event.getBlock().getType() == Material.DIODE_BLOCK_ON)
            weakCircuit = inputs.get(event.getBlock().getLocation());

        final BuiltCircuit circuit = weakCircuit != null ? weakCircuit.get() : null;

        if (circuit != null && circuit.isBuilt()) {
            final Player p = event.getPlayer();
            if (p.hasPermission("nanotech.irc.destroy")) {
                p.sendMessage("[NanoTech] IRC destroyed.");
                circuit.destroy(new BlockChangeAPI.Real());
                circuits.get(event.getPlayer().getWorld().getName()).remove(circuit);
            } else {
                p.sendMessage("Ouch! That hurts!");
                p.playEffect(event.getBlock().getLocation(), Effect.EXTINGUISH, null);
                p.setHealth(max(p.getHealth() - 2, 1));
                event.setCancelled(true);
            }
        }
    }

    public boolean handleEnvironmentalBlockBreak(final Location location) {
        Block block = location.getBlock();
        Material type = block.getType();
        WeakReference<BuiltCircuit> weakCircuit = null;
        if (type == Circuit.MATERIAL)
            weakCircuit = blocks.get(location);
        else if (type == Material.REDSTONE_TORCH_OFF || type == Material.REDSTONE_TORCH_ON)
            weakCircuit = outputs.get(location);
        else if (type == Material.DIODE_BLOCK_OFF || type == Material.DIODE_BLOCK_ON)
            weakCircuit = inputs.get(location);

        final BuiltCircuit circuit = weakCircuit != null ? weakCircuit.get() : null;
        if (circuit != null) {
            if (!config.getBoolean("protect")) {
                circuit.destroy(new BlockChangeAPI.Real());
                circuits.get(location.getWorld().getName()).remove(circuit);
                plugin.getLogger().info("IRC at " + location + " was destroyed by the environment.");
            }
            return true; // return true: we are either protecting it or removed it already
        }
        return false;
    }
}
