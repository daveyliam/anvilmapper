package anvilmapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import mapwriter.region.RegionManager;
import mapwriter.region.Region;
import mapwriter.region.BlockColours;

/* TODO:
 *  - Make it possible to load BlockColours from a file (make BlockColours Serializable?)
 *  - Load waypoints and output in JSON format? (No built in library for JSON, maybe use XML or CSV instead?)
 */

public class AnvilMapper {
	
	private final static Logger log = Logger.getLogger("anvilmapper");
	
	static {
		RegionManager.logger = log;
	}
	
    private File worldDir;
	private File imageDir;
	private BlockColours blockColours;
	private RegionManager regionManager;
	
    public AnvilMapper(File worldDir, File imageDir, File blockColoursFile) {
        this.worldDir = worldDir;
        this.imageDir = imageDir;
        this.blockColours = BlockColours.loadFromFile(blockColoursFile);
        this.regionManager = new RegionManager(this.worldDir, this.imageDir, this.blockColours);
        
		this.imageDir.mkdirs();
    }
    
	public void processDimension(File dimDir, int dimension) {
		File regionDir = new File(dimDir, "region");
		if (dimDir.isDirectory()) {
			File[] regionFilesList = regionDir.listFiles();
			if (regionFilesList != null) {
				for (File regionFileName : regionFilesList) {
					if (regionFileName.isFile()) {
						// get the region x and z from the region file name
						String[] baseNameSplit = regionFileName.getName().split("\\.");
						if ((baseNameSplit.length == 4) && (baseNameSplit[0].equals("r")) && (baseNameSplit[3].equals("mca"))) {
							try {
								int rX = Integer.parseInt(baseNameSplit[1]);
								int rZ = Integer.parseInt(baseNameSplit[2]);
								
								Region region = this.regionManager.getRegion(rX << Region.SHIFT, rZ << Region.SHIFT, 0, dimension);
								RegionManager.logInfo("loaded file %s as region %s", regionFileName, region);
								region.reload();
								region.updateZoomLevels();
								region.saveToImage();
								this.splitRegionImage(region, 1);
								this.regionManager.unloadRegion(region);
							} catch (NumberFormatException e) {
								RegionManager.logWarning("could not get region x and z for region file %s", regionFileName);
							}
						
						} else {
							RegionManager.logWarning("region file %s did not pass the file name check", regionFileName);
						}
					}
				}
				
				RegionManager.logInfo("closing region manager");
				this.regionManager.close();
				
			} else {
				RegionManager.logInfo("no region files found for dimension %d", dimension);
			}
			
		} else {
			RegionManager.logInfo("no region directory in dimension directory %s", dimDir);
		}
	}
	
	public void processWorld() {
		
		File[] dimDirList = this.worldDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File f, String name) {
				return f.isDirectory() && name.startsWith("DIM");
			}
		});
		
		for (File dimDir : dimDirList) {
			try {
				int dimension = Integer.parseInt(dimDir.getName().substring(3));
				this.processDimension(dimDir, dimension);
			} catch (NumberFormatException e) {
				RegionManager.logWarning("could not dimension number for dimension directory %s", dimDir);
			}
		}
		
		this.processDimension(this.worldDir, 0);
	}
	
	public static void writeImage(BufferedImage img, File imageFile) {
		// write the given image to the image file
		File dir = imageFile.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		try {
			ImageIO.write(img, "png", imageFile);
		} catch (IOException e) {
			RegionManager.logError("could not write image to %s", imageFile);
		}
	}
	
	private void splitRegionImage(Region region, int z) {
		int splitSize = Region.SIZE >> z;
		int[] pixels = region.getPixels();
		if (pixels != null) {
			
			BufferedImage regionImage = new BufferedImage(Region.SIZE, Region.SIZE, BufferedImage.TYPE_INT_RGB);
			regionImage.setRGB(0, 0, Region.SIZE, Region.SIZE, pixels, 0, Region.SIZE);
			
			BufferedImage dstImage = new BufferedImage(Region.SIZE, Region.SIZE, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = dstImage.createGraphics();
			
			for (int srcZ = 0; srcZ < Region.SIZE; srcZ += splitSize) {
				for (int srcX = 0; srcX < Region.SIZE; srcX += splitSize) {
					
					g.setPaint(Color.BLACK);
					g.fillRect(0, 0, Region.SIZE, Region.SIZE);
					g.drawImage(regionImage, 0, 0, Region.SIZE, Region.SIZE, srcX, srcZ, srcX + splitSize, srcZ + splitSize, null);
					
					writeImage(dstImage, Region.getImageFile(this.imageDir, region.x + srcX, region.z + srcZ, -z, region.dimension));
				}
			}
			
			g.dispose();
		}
	}
	
	public static void main(String [] args) {
		
		if (args.length == 1) {
			File worldDir = new File(args[0]);
			File blockColoursFile = new File("MapWriterBlockColours.txt");
			File imageDir = new File("images");
			if (worldDir.isDirectory()) {
				AnvilMapper anvilMapper = new AnvilMapper(worldDir, imageDir, blockColoursFile);
				anvilMapper.processWorld();
			} else {
				RegionManager.logError("world directory '%s' does not exist\n", worldDir);
			}
			
		} else {
			RegionManager.logInfo("usage: java AnvilMapper <worldDirectory>");
		}
	}
}
