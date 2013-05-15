package me.ienze.SimpleRegionMarket.regions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import me.ienze.SimpleRegionMarket.SimpleRegionMarket;

public class RegionSaveThread extends Thread {

    private RegionSaverData data;

    public RegionSaveThread(RegionSaverData data) {
        super("RegionSaveThread");
        this.data = data;
    }

    @Override
    public void run() {
        File regionFile = new File(SimpleRegionMarket.getPluginDir() + "regions", data.getWorld() + ".rdb");
        try {
            String tmpPath = regionFile.getPath();
            if (tmpPath.endsWith("\\") || tmpPath.endsWith("/")) {
                tmpPath = tmpPath.substring(0, tmpPath.length() - 1);
            }
            File tempFile = new File(tmpPath + ".temp");
            if (regionFile.exists()) {
                tempFile.delete();
                tempFile.deleteOnExit();

                regionFile.renameTo(tempFile);
            }
            byte[] buf = new byte[1024];

            ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
            ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(regionFile));

            ZipEntry entry = zin.getNextEntry();
            boolean rewrited = false;

            while (entry != null) {
                zout.putNextEntry(new ZipEntry(entry.getName()));
                if (entry.getName().equals(data.getRegion())) {
                    RegionsSaver.writeData(zout, data.getData());
                    rewrited = true;
                } else {
                    int len;
                    while ((len = zin.read(buf)) > 0) {
                        zout.write(buf, 0, len);
                    }
                }
                entry = zin.getNextEntry();
            }

            if (!rewrited) {
                zout.putNextEntry(new ZipEntry(data.getRegion()));
                RegionsSaver.writeData(zout, data.getData());
            }

            zin.close();
            zout.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
