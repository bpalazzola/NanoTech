package me.main__.nanotech;

import org.bukkit.block.BlockFace;

public final class Util {
    private Util() {}

    public static BlockFace cutDownToOneOfTheFourBasicDirections(BlockFace face) {
        switch (face) {
            case NORTH:
            case NORTH_NORTH_WEST:
            case NORTH_NORTH_EAST:
                return BlockFace.NORTH;
            case WEST:
            case WEST_NORTH_WEST:
            case WEST_SOUTH_WEST:
                return BlockFace.WEST;
            case SOUTH:
            case SOUTH_SOUTH_EAST:
            case SOUTH_SOUTH_WEST:
                return BlockFace.SOUTH;
            case EAST:
            case EAST_NORTH_EAST:
            case EAST_SOUTH_EAST:
                return BlockFace.EAST;
            default:
                throw new UnsupportedOperationException("Can't cut down: " + face);
        }
    }
}
