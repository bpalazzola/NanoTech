package me.main__.nanotech;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.LinkedHashMap;
import java.util.Map;

@SerializableAs("LightweightLocation")
public class LightweightLocation extends Location implements ConfigurationSerializable {
    private String world;

    public LightweightLocation(final World world, final double x, final double y, final double z) {
        super(null, x, y, z);
        this.world = world.getName();
    }

    public LightweightLocation(final World world, final double x, final double y, final double z, final float yaw, final float pitch) {
        super(null, x, y, z, yaw, pitch);
        this.world = world.getName();
    }

    public LightweightLocation(final String world, final double x, final double y, final double z, final float yaw, final float pitch) {
        super(null, x, y, z, yaw, pitch);
        this.world = world;
    }

    public LightweightLocation(final Location loc) {
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    @Override
    public void setWorld(final World world) {
        this.world = world.getName();
    }

    @Override
    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public Location toLocation() {
        return new Location(getWorld(), getX(), getY(), getZ(), getYaw(), getPitch());
    }

    @Override
    public Block getBlock() {
        return getWorld().getBlockAt(getBlockX(), getBlockY(), getBlockZ());
    }

    @Override
    public Chunk getChunk() {
        return getWorld().getChunkAt(this);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("world", world);
        map.put("x", getX());
        map.put("y", getY());
        map.put("z", getZ());
        map.put("yaw", getYaw());
        map.put("pitch", getPitch());
        return map;
    }

    public static LightweightLocation deserialize(Map<String, Object> map) {
        return new LightweightLocation((String)map.get("world"), ((Number)map.get("x")).doubleValue(),
                ((Number)map.get("y")).doubleValue(), ((Number)map.get("z")).doubleValue(),
                ((Number)map.get("yaw")).floatValue(), ((Number)map.get("pitch")).floatValue());
    }
}
