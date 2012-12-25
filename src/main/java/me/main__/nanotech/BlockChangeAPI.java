package me.main__.nanotech;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public interface BlockChangeAPI {
    Block changeBlock(Block b, Material m, byte data);

    public static final class Real implements BlockChangeAPI {
        @Override
        public Block changeBlock(final Block b, final Material m, final byte data) {
            // special case for air: if we are EXPLICITLY removing, we don't want drops
            if (m != Material.AIR) b.breakNaturally();
            b.setTypeIdAndData(m.getId(), data, false);
            return b;
        }
    }

    public static final class Virtual implements BlockChangeAPI {
        private final Player player;

        public Virtual(final Player player) {
            this.player = player;
        }

        @Override
        public Block changeBlock(final Block b, final Material m, final byte data) {
            // special case for air: it will simply undo the virtual changes and send real data
            if (m == Material.AIR) player.sendBlockChange(b.getLocation(), b.getType(), b.getData());
            else player.sendBlockChange(b.getLocation(), m, data);
            return b;
        }
    }
}
