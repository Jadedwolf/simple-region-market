package me.ienze.SimpleRegionMarket.regions;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.nio.ByteBuffer;
import me.ienze.SimpleRegionMarket.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.ContainerBlock;

@SuppressWarnings("deprecation")
public class RegionSaverData {

    private String world;
    private String region;
    private int rx, ry, rz;
    private ByteBuffer data;

    public RegionSaverData(String world, String region) {

        this.world = world;
        this.region = region;
    }

    public RegionSaverData(byte[] bytes) {
        data = ByteBuffer.allocate(bytes.length);
        data.put(bytes);
        data.rewind();
    }

    public int getTypeId(int ox, int oy, int oz) {
        return data.getShort((12) + (ox * rz + oz) * ry + oy);
    }

    public byte getData(int ox, int oy, int oz) {
        return data.get((12) + (ox * rz + oz) * ry + oy /* +2 becouse data is second block value:*/ + 2);
    }

    public void fromBlocks(World world, ProtectedRegion region) {

        this.world = world.getName();
        this.region = region.getId();

        int mix = region.getMinimumPoint().getBlockX();
        int miy = region.getMinimumPoint().getBlockY();
        int miz = region.getMinimumPoint().getBlockZ();

        int max = region.getMaximumPoint().getBlockX() + 1;
        int may = region.getMaximumPoint().getBlockY() + 1;
        int maz = region.getMaximumPoint().getBlockZ() + 1;

        this.rx = (max - mix);
        this.ry = (may - miy);
        this.rz = (maz - miz);

        data = ByteBuffer.allocate(rx * ry * rz * 3);

        for (int x = 0; x < rx; x++) {
            for (int y = 0; y < ry; y++) {
                for (int z = 0; z < rz; z++) {
                    int to = ((x * rz + z) * ry + y) * 3;
                    data.putShort(to, (short) world.getBlockTypeIdAt(mix + x, miy + y, miz + z));
                    data.put(to + 2, world.getBlockAt(mix + x, miy + y, miz + z).getData());
                }
            }
        }
    }

    public void placeBlocks() {
        ProtectedRegion region = Utils.getProtectedRegion(this.region, null, this.world);
        World world = Bukkit.getWorld(this.world);

        int mix = region.getMinimumPoint().getBlockX();
        int miy = region.getMinimumPoint().getBlockY();
        int miz = region.getMinimumPoint().getBlockZ();

        int max = region.getMaximumPoint().getBlockX() + 1;
        int may = region.getMaximumPoint().getBlockY() + 1;
        int maz = region.getMaximumPoint().getBlockZ() + 1;

        this.rx = (max - mix);
        this.ry = (may - miy);
        this.rz = (maz - miz);

        for (int x = 0; x < rx; x++) {
            for (int y = 0; y < ry; y++) {
                for (int z = 0; z < rz; z++) {
                    int to = ((x * rz + z) * ry + y) * 3;
                    int blockId = data.getShort(to);
                    byte blockData = data.get(to + 2);

                    //remove old block good
                    Block b = world.getBlockAt(mix + x, miy + y, miz + z);
                    if (b.getState() instanceof ContainerBlock) {
                        ((ContainerBlock) b.getState()).getInventory().clear();
                    }
                    b.setTypeIdAndData(blockId, blockData, false);
                }
            }
        }
    }

    public byte[] getData() {
        return data.array();
    }

    public String getWorld() {
        return world;
    }

    public String getRegion() {
        return region;
    }

    public void setData(SimpleRegionData data) {
        this.region = data.getRegion();
        this.world = data.getWorld();
    }
}
