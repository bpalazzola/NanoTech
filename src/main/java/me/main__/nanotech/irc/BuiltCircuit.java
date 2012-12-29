package me.main__.nanotech.irc;

import me.main__.nanotech.BlockChangeAPI;
import me.main__.nanotech.LightweightLocation;
import me.main__.nanotech.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.material.Diode;
import org.bukkit.material.RedstoneTorch;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.*;

@SerializableAs("NanoTech_IRC")
public class BuiltCircuit implements ConfigurationSerializable, Runnable, Circuit.Input, Circuit.Output {
    private List<? extends Location> repeaterLocations;
    private List<? extends Location> torchLocations;
    private List<? extends Location> blockLocations;
    private Block[] repeaters;
    private Block[] torches;
    private Block[] blocks;

    // these four are redundant from the blocks but we're saving them for performance reasons
    protected final int highX;
    protected final int lowX;
    protected final int highZ;
    protected final int lowZ;

    protected Circuit circuit;
    protected final CircuitFactory circuitFactory;
    protected boolean built;

    private BuiltCircuit(final List<? extends Location> blocks, final int highX, final int lowX, final int highZ,
                         final int lowZ, final CircuitFactory circuitFactory,
                         final List<? extends Location> repeaters, final List<? extends Location> torches) {
        this.blocks = null;
        this.blockLocations = blocks;
        this.torches = null;
        this.torchLocations = torches;
        this.repeaters = null;
        this.repeaterLocations = repeaters;

        this.highX = highX;
        this.lowX = lowX;
        this.highZ = highZ;
        this.lowZ = lowZ;
        this.circuitFactory = circuitFactory;

        this.built = true;
    }

    private static Block[] toBlockArray(List<? extends Location> locs) {
        Block[] blocks = new Block[locs.size()];
        for (int i = 0; i < blocks.length; i++)
            blocks[i] = locs.get(i).getBlock();
        return blocks;
    }

    private static List<LightweightLocation> toLightweightLocationList(Block[] blocks) {
        List<LightweightLocation> locs = new LinkedList<LightweightLocation>();
        for (Block b : blocks)
            locs.add(new LightweightLocation(b.getLocation()));
        return locs;
    }

    private BuiltCircuit(final Block[] blocks, final int highX, final int lowX, final int highZ,
                         final int lowZ, final CircuitFactory circuitFactory,
                         final Block[] repeaters, final Block[] torches) {
        this.blocks = blocks;
        this.highX = highX;
        this.lowX = lowX;
        this.highZ = highZ;
        this.lowZ = lowZ;
        this.circuitFactory = circuitFactory;

        //if (!circuit.isDigital())
        //    throw new IllegalArgumentException("This is only for digital circuits!");
        this.repeaters = repeaters;
        this.torches = torches;
        this.built = true;
    }

    public BuiltCircuit(final Block[] blocks, final int highX, final int lowX, final int highZ,
                        final int lowZ, final CircuitFactory circuitFactory) {
        this(blocks, highX, lowX, highZ, lowZ, circuitFactory, new Block[circuitFactory.getInputs()],
                new Block[circuitFactory.getOutputs()]);
        this.built = false;
    }

    public Block[] getBlocks() {
        activate();
        return blocks;
    }

    public Circuit getCircuit() {
        if (circuit == null) {
            circuit = circuitFactory.build();
            circuit.init(this, this, circuitFactory);
        }
        return circuit;
    }

    public boolean isBuilt() {
        return built;
    }

    public void build(BlockFace orientation, BlockChangeAPI api) {
        activate();
        // what is this orientation shit?
        // ---
        // Example: North north east
        // ==> the data will flow through this IRC from south to north and the inputs and outputs will be
        // aligned to the east. Like this:
        //    ^^^
        //    |||             N
        // XXXXXX           W   E
        //  |||||             S
        //  ^^^^^

        // yay for ascii art

        final int lenz = abs(highZ - lowZ);
        final int lenx = abs(highX - lowX);
        final World w = blocks[0].getWorld();
        final int y = blocks[0].getY();
        // make the arrays big enough
        Block[] possibleRepeaterBlocks = new Block[max(lenz, lenx) + 1];
        Block[] possibleTorchBlocks = new Block[max(lenz, lenx) + 1];

        // note to self: errors here might be caused by inconsistency between lenz/lenx and the orientation

        switch (orientation) {
            case NORTH_NORTH_EAST:
                for (int offset = 0; offset <= lenx; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(highX - offset, y, highZ + 1);
                    possibleTorchBlocks[offset] = w.getBlockAt(highX - offset, y, lowZ - 1);
                }
                break;
            case NORTH_NORTH_WEST:
                for (int offset = 0; offset <= lenx; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(lowX + offset, y, highZ + 1);
                    possibleTorchBlocks[offset] = w.getBlockAt(lowX + offset, y, lowZ - 1);
                }
                break;
            case SOUTH_SOUTH_EAST:
                for (int offset = 0; offset <= lenx; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(highX - offset, y, lowZ - 1);
                    possibleTorchBlocks[offset] = w.getBlockAt(highX - offset, y, highZ + 1);
                }
                break;
            case SOUTH_SOUTH_WEST:
                for (int offset = 0; offset <= lenx; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(lowX + offset, y, lowZ - 1);
                    possibleTorchBlocks[offset] = w.getBlockAt(lowX + offset, y, highZ + 1);
                }
                break;
            case EAST_NORTH_EAST:
                for (int offset = 0; offset <= lenz; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(lowX - 1, y, lowZ + offset);
                    possibleTorchBlocks[offset] = w.getBlockAt(highX + 1, y, lowZ + offset);
                }
                break;
            case EAST_SOUTH_EAST:
                for (int offset = 0; offset <= lenz; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(lowX - 1, y, highZ - offset);
                    possibleTorchBlocks[offset] = w.getBlockAt(highX + 1, y, highZ - offset);
                }
                break;
            case WEST_NORTH_WEST:
                for (int offset = 0; offset <= lenz; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(highX + 1, y, lowZ + offset);
                    possibleTorchBlocks[offset] = w.getBlockAt(lowX - 1, y, lowZ + offset);
                }
                break;
            case WEST_SOUTH_WEST:
                for (int offset = 0; offset <= lenz; offset++) {
                    possibleRepeaterBlocks[offset] = w.getBlockAt(highX + 1, y, highZ - offset);
                    possibleTorchBlocks[offset] = w.getBlockAt(lowX - 1, y, highZ - offset);
                }
                break;
            default:
                throw new IllegalStateException("Illegal orientation.");
        }

        BlockFace repeaterAndTorchOrientation = Util.cutDownToOneOfTheFourBasicDirections(orientation);

        Diode diodeData = new Diode(Material.DIODE_BLOCK_OFF);
        RedstoneTorch torchData = new RedstoneTorch(Material.REDSTONE_TORCH_OFF);
        diodeData.setFacingDirection(repeaterAndTorchOrientation);
        torchData.setFacingDirection(repeaterAndTorchOrientation);
        for (int i = 0; i < getCircuit().getFactory().getInputs(); i++)
            repeaters[i] = api.changeBlock(possibleRepeaterBlocks[i], diodeData.getItemType(), diodeData.getData());
        for (int i = 0; i < getCircuit().getFactory().getOutputs(); i++)
            torches[i] = api.changeBlock(possibleTorchBlocks[i], torchData.getItemType(), torchData.getData());

        built = true;
    }

    public void activate() {
        if (this.blocks != null)
            return;
        this.blocks = toBlockArray(this.blockLocations);
        this.torches = toBlockArray(this.torchLocations);
        this.repeaters = toBlockArray(this.repeaterLocations);
        this.blockLocations = this.torchLocations = this.repeaterLocations = null;
    }

    public void sleep() {
        if (this.blockLocations != null)
            return;
        this.blockLocations = toLightweightLocationList(this.blocks);
        this.torchLocations = toLightweightLocationList(this.torches);
        this.repeaterLocations = toLightweightLocationList(this.repeaters);
        this.blocks = this.torches = this.repeaters = null;
    }

    public Collection<BlockFace> getPossibleOrientations() {
        List<BlockFace> list = new LinkedList<BlockFace>();
        final int lenz = abs(highZ - lowZ);
        final int lenx = abs(highX - lowX);

        if ((lenz + 1) >= max(getCircuit().getFactory().getInputs(), getCircuit().getFactory().getOutputs())) {
            list.add(BlockFace.EAST_NORTH_EAST);
            list.add(BlockFace.EAST_SOUTH_EAST);
            list.add(BlockFace.WEST_NORTH_WEST);
            list.add(BlockFace.WEST_SOUTH_WEST);
        }
        if ((lenx + 1) >= max(getCircuit().getFactory().getInputs(), getCircuit().getFactory().getOutputs())) {
            list.add(BlockFace.NORTH_NORTH_EAST);
            list.add(BlockFace.NORTH_NORTH_WEST);
            list.add(BlockFace.SOUTH_SOUTH_EAST);
            list.add(BlockFace.SOUTH_SOUTH_WEST);
        }
        return list;
    }

    public void destroy(BlockChangeAPI api) {
        activate();
        makeAirAndNullOut(repeaters, api);
        makeAirAndNullOut(torches, api);
        built = false;
    }

    private static void makeAirAndNullOut(final Block[] blocks, final BlockChangeAPI api) {
        for (int i = 0; i < blocks.length; i++) {
            api.changeBlock(blocks[i], Material.AIR, (byte)0);
            blocks[i] = null;
        }
    }

    public Block[] getInputs() {
        activate();
        return repeaters;
    }

    public Block[] getOutputs() {
        activate();
        return torches;
    }

    @Override
    public void run() {
        activate();
        getCircuit().onInputChanged();
    }

    @Override
    public Map<String, Object> serialize() {
        sleep();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("blocks", blockLocations);
        map.put("circuit", getCircuit().getFactory().getId());
        // we're not serializing these values since they're only used in build()
        //map.put("highX", highX);
        //map.put("lowX", lowX);
        //map.put("highZ", highZ);
        //map.put("lowZ", lowZ);
        map.put("repeaters", repeaterLocations);
        map.put("torches", torchLocations);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static BuiltCircuit deserialize(Map<String, Object> args) {
        return new BuiltCircuit(((List<LightweightLocation>)args.get("blocks")), /*(Integer)args.get("highX"),
                (Integer)args.get("lowX"), (Integer)args.get("highZ"), (Integer)args.get("lowZ"),*/ -1, -1, -1, -1,
                Storage.getIRC_static((String) args.get("circuit")),
                ((List<LightweightLocation>)args.get("repeaters")),
                ((List<LightweightLocation>)args.get("torches")));
    }

    @Override
    public boolean[] get() {
        if (!isBuilt())
            return null;

        boolean[] b = new boolean[repeaters.length];
        for (int i = 0; i < b.length; i++)
            b[i] = repeaters[i].getType() == Material.DIODE_BLOCK_ON;
        return b;
    }

    @Override
    public void output(final boolean[] data) {
        activate();
        if (!isBuilt())
            return;

        for (int i = 0; i < min(data.length, torches.length); i++)
            torches[i].setType(data[i] ? Material.REDSTONE_TORCH_ON : Material.REDSTONE_TORCH_OFF);
    }
}
