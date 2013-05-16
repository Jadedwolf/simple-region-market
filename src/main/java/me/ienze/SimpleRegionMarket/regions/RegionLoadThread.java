package me.ienze.SimpleRegionMarket.regions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;

public class RegionLoadThread extends Thread {

    private SimpleRegionData data;

    public RegionLoadThread(SimpleRegionData data) {
        super("RegionLoadThread");
        this.data = data;
    }

    @Override
    public void run() {
        File regionFile = new File(SimpleRegionMarket.getPluginDir() + "regions", data.getWorld() + ".rdb");
        try {
            ZipFile zip = new ZipFile(regionFile);
            ZipEntry ze = zip.getEntry(data.getRegion());
            if (ze != null) {
                InputStream is = zip.getInputStream(ze);
                byte[] readed = RegionsSaver.readData(is);
                is.close();
                RegionSaverData regionSaver = new RegionSaverData(readed);
                regionSaver.setData(data);
                regionSaver.placeBlocks();
            }
            zip.close();
        } catch (FileNotFoundException e) {
            System.out.println("[SRM] You're using old region files, please delete the SimpleRegionMarket/region folder!");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[SRM] Unable to read/write to the SimpleRegionMarket/region location!");
            e.printStackTrace();
        }

        new RegionRemoveThread(data).start();
    }
}
