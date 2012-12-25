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
    protected final Block[] blocks;
    // these four are redundant from the blocks but we're saving them for performance reasons
    protected final int highX;
    protected final int lowX;
    protected final int highZ;
    protected final int lowZ;
    protected Circuit circuit;
    protected final CircuitFactory circuitFactory;
    protected boolean built;
    private final Block[] repeaters;
    private final Block[] torches;

    private BuiltCircuit(final List<? extends Location> blocks, final int highX, final int lowX, final int highZ,
                         final int lowZ, final CircuitFactory circuitFactory,
                         final List<? extends Location> repeaters, final List<? extends Location> torches) {
        this(toBlockArray(blocks), highX, lowX, highZ, lowZ, circuitFactory, toBlockArray(repeaters), toBlockArray(torches));
    }

    private static Block[] toBlockArray(List<? extends Location> locs) {
        Block[] blocks = new Block[locs.size()];
        for (int i = 0; i < blocks.length; i++)
            blocks[i] = locs.get(i).getBlock();
        return blocks;
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

        this.built = false;
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
        return repeaters;
    }

    public Block[] getOutputs() {
        return torches;
    }

    @Override
    public void run() {
        getCircuit().onInputChanged();
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        List<LightweightLocation> blo = new LinkedList<LightweightLocation>();
        for (Block b : blocks)
            blo.add(new LightweightLocation(b.getLocation()));
        map.put("blocks", blo);
        map.put("circuit", getCircuit().getFactory().getId());
        map.put("highX", highX);
        map.put("lowX", lowX);
        map.put("highZ", highZ);
        map.put("lowZ", lowZ);

        List<LightweightLocation> rep = new LinkedList<LightweightLocation>();
        for (Block b : repeaters)
            rep.add(new LightweightLocation(b.getLocation()));
        map.put("repeaters", rep);
        List<LightweightLocation> tor = new LinkedList<LightweightLocation>();
        for (Block b : torches)
            tor.add(new LightweightLocation(b.getLocation()));
        map.put("torches", tor);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static BuiltCircuit deserialize(Map<String, Object> args) {
        return new BuiltCircuit(((List<LightweightLocation>)args.get("blocks")), (Integer)args.get("highX"),
                (Integer)args.get("lowX"), (Integer)args.get("highZ"), (Integer)args.get("lowZ"),
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
        if (!isBuilt())
            return;

        for (int i = 0; i < min(data.length, torches.length); i++)
            torches[i].setType(data[i] ? Material.REDSTONE_TORCH_ON : Material.REDSTONE_TORCH_OFF);
    }
}
