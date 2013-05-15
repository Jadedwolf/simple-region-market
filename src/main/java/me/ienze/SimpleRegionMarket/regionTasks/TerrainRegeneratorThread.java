package me.ienze.SimpleRegionMarket.regionTasks;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.server.v1_5_R3.BiomeBase;
import net.minecraft.server.v1_5_R3.ChunkProviderGenerate;
import net.minecraft.server.v1_5_R3.WorldType;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;

public class TerrainRegeneratorThread extends Thread {

    private World world;
    private ProtectedRegion region;

    public TerrainRegeneratorThread(World world, ProtectedRegion region) {
        this.world = world;
        this.region = region;
    }

    public void run() {

        HashMap<String, byte[][]> chunks = new HashMap<String, byte[][]>();

        ChunkGenerator generator = world.getGenerator();

        for (int x = (int) (Math.floor(region.getMinimumPoint().getBlockX() / 16) - 1); x < (Math.floor(region.getMaximumPoint().getBlockX() / 16)); x++) {
            for (int z = (int) (Math.floor(region.getMinimumPoint().getBlockZ() / 16)); z < (Math.floor(region.getMaximumPoint().getBlockZ() / 16) + 1); z++) {

                if (generator != null) {
                    //plugin
                    byte[][] chunk = new byte[world.getMaxHeight() / 16][4096];
                    BiomeGrid biomes = null;

                    short[][] extChunk = generator.generateExtBlockSections(world, new Random(world.getSeed()), x, z, biomes);
                    if (extChunk != (short[][]) null) {
                        for (int i = 0; i < extChunk.length; i++) {
                            ByteBuffer.wrap(chunk[i]).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(extChunk[i]);
                        }
                    } else {
                        chunk = generator.generateBlockSections(world, new Random(world.getSeed()), x, z, biomes);
                        if (chunk == (byte[][]) null) {
                            System.out.print("[SimpleRegionMarket] Your terrain generator isn't supported.");
                        }
                    }
                    chunks.put(x + "," + z, chunk);
                } else {
                    //default generator
                    net.minecraft.server.v1_5_R3.World mcworld = ((CraftWorld) world).getHandle();

                    byte[] bytes = new byte[65536];
                    BiomeBase[] biomes = new BiomeBase[256];
                    for (int i = 0; i < biomes.length; i++) {
                        biomes[i] = BiomeBase.PLAINS;
                    }

                    if (mcworld.worldProvider.type == WorldType.NORMAL) {
                        new ChunkProviderGenerate(mcworld, mcworld.getSeed(), true).a(x, z, bytes, biomes);
                    }

                    byte[][] sections = new byte[world.getMaxHeight() / 16][4096];
                    for (int i = 0; i < world.getMaxHeight() / 16; i++) {
                        for (int cx = 0; cx < 16; cx++) {
                            for (int cy = 0; cy < 16; cy++) {
                                for (int cz = 0; cz < 16; cz++) {
                                    sections[cy >> 4][((cy & 0xF) << 8) | (cz << 4) | cx] = bytes[(cz * 16 + cx) * world.getMaxHeight() + cy];
                                }
                            }
                        }
                    }

                    chunks.put(x + "," + z, sections);
                }
            }
        }

        for (int x = region.getMinimumPoint().getBlockX(); x < region.getMaximumPoint().getBlockX(); x++) {
            for (int y = region.getMinimumPoint().getBlockY(); y < region.getMaximumPoint().getBlockY(); y++) {
                for (int z = region.getMinimumPoint().getBlockZ(); z < region.getMaximumPoint().getBlockZ(); z++) {
                    byte[][] chunk = chunks.get(((int) Math.floor(x / 16) - 1) + "," + ((int) Math.floor(z / 16)));
                    if (chunk == null) {
                        System.out.print("Chunk is null.");
                    } else {

                        int cx = (int) (x - ((int) Math.floor(x / 16) - 1) * 16) - 1;
                        int cz = (int) (z - ((int) Math.floor(z / 16) - 1) * 16) - 1;

                        if (chunk[y >> 4] == (byte[]) null) {
                            world.getBlockAt(x, y, z).setTypeId(0);
                        } else {
                            int id = chunk[y >> 4][((y & 0xF) << 8) | (cz << 4) | cx];
                            world.getBlockAt(x, y, z).setTypeId(id);
                        }
                    }
                }
            }
        }
    }
}
