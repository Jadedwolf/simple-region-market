package me.ienze.SimpleRegionMarket.regions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import me.ienze.SimpleRegionMarket.SimpleRegionMarket;

import org.bukkit.World;

public class RegionsSaver {

	public RegionsSaver(List<World> list) {
		String path = SimpleRegionMarket.getPluginDir() + "regions";

		File directory = new File(path);
		if(!directory.exists()) {
			directory.mkdirs();
		}

		for(World world : list) {
			File wFile = new File(path, world.getName() + ".rdb");
			if(!wFile.exists()) {
				try {
					wFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void load(SimpleRegionData data) {
		new RegionLoadThread(data).start();
	}

	public void save(RegionSaverData data) {
		new RegionSaveThread(data).start();
	}

	public void remove(SimpleRegionData data) {
		new RegionRemoveThread(data).start();
	}

	private static Integer readLength(InputStream bis) {
		byte[] b = new byte[4];
		read(bis, b);
		return b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
	}

	private static void writeLength(OutputStream bos, int value) {
		byte[] b = new byte[] {
		        (byte) (value >> 24),
		        (byte) (value >> 16),
		        (byte) (value >> 8),
		        (byte) value};
		write(bos, b);
	}

	public static byte[] readData(InputStream bis) {
		int length = readLength(bis);
		if(length > 0) {
			byte[] bytes = new byte[length];
			try {
				read(bis, bytes);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}

			return bytes;
		}
		return null;
	}

	public static void writeData(OutputStream bos, byte[] bytes) {
		writeLength(bos, bytes.length);
		write(bos, bytes);
	}

	private static void write(OutputStream bos, byte[] b) {
		synchronized (bos) {
			try {
				for(int i=0; i<b.length; i++) {
					bos.write(b[i]);
				}
				bos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void read(InputStream bis, byte[] bytes) {
		synchronized (bis) {
			try {
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = (byte) bis.read();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
