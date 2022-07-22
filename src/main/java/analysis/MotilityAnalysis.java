package analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.AVI_Reader;
import thresholding.LocalThresholding;
/**
 * Works with AVI movie
 * @author MMB
 */
public class MotilityAnalysis{
	private ImageStack stack;
	public LocalThresholding thresholder;
	public int gap1;
	public int gap2;
	public String srcFile;
	public String destFile;

	/**
     * Example of analysis
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        LocalThresholding thresholder = new LocalThresholding();
        //LocalThresholding thresholder = new LocalThresholding(3, 15, 0.1, 0.4, 5);
        String srcFolder = "C:\\Users\\Example\\Documents\\Neutrophil Motility Analysis";
        String destFile = "C:\\Users\\Example\\Documents\\Neutrophil Motility Analysis\\Example.csv";
        MotilityAnalysis ra = new MotilityAnalysis(thresholder, srcFolder, destFile, 5, 10);
        ra.run();
    }
    
	/**
	 * Run analysis on folder
	 * @param thresholder custom thresholder; leave null if you want default
	 * @param srcFile folder that contains all the AVIs to be processed
	 * @param destFile csv file where you want the results to be written. WARNING: Overwrites anything in this file
	 * @param gap1 gap between reference frame and first point. 
	 * @param gap2 gap between reference frame and second point.
	 */
	public MotilityAnalysis(LocalThresholding thresholder, String srcFile, String destFile, int gap1, int gap2) {
		if(thresholder==null) this.thresholder = new LocalThresholding();
		else this.thresholder = thresholder;
		this.srcFile = srcFile;
		this.destFile = destFile;
		this.gap1 = gap1;
		this.gap2 = gap2;
	}


	/**
	 * Goes through all files and calculates motility index and writes it to a csv
	 * @throws IOException
	 */
	public void run() throws IOException {
		FileWriter writer = new FileWriter(new File(destFile));
		int total = FileUtils.listFiles(new File(srcFile), new String[] {"avi"}, true).size();
		int count = 0;
		System.out.println(total);
		for(File f : FileUtils.listFiles(new File(srcFile), new String[] {"avi"}, true)) {
			count++;
			System.out.println(count+" of "+total);
			System.out.println(f.getAbsolutePath());
			//thresholder.save(f.getAbsolutePath(),f.getParentFile().getParentFile().getName()+", "+f.getParentFile().getName()+", "+f.getName(), "C:\\Users\\MMB\\Desktop\\Joseph Cai\\Neutrophil Motility Analysis", 1);
			double motility = motilityIndex(f.getAbsolutePath());
			System.out.println(motility);
			writer.write(f.getAbsolutePath().substring(srcFile.length()+1).replace(File.separatorChar,',')+","+motility+"\n");
		}
		writer.close();
	}
	/**
	 * Calculates motility index
	 * @param fileName movie to be processed
	 * @return motilityIndex in pixels/sec
	 */
	public double motilityIndex(String fileName) {
		stack = getImageStack(fileName);
		double total = 0;
		int count = 0;
		double frameRate = frameRate(fileName);
		for(int i =gap2+1; i<=stack.size(); i++) {
			double difference = combined(i-gap2,i)-combined(i-gap1,i);
			total+=(difference/(gap2-gap1)*frameRate);
			count++;
			System.out.println(count);
		}
		return total/count;

	}

	/**
	 * Calculates difference between two slices in current movie
	 * @param first frame index
	 * @param second frame index
	 * @return difference
	 */
	private double combined(int first, int second) {
		return thresholder.combined(new ImagePlus("",stack.getProcessor(first).convertToByteProcessor()),new ImagePlus("",stack.getProcessor(second).convertToByteProcessor()));
	}

	/**
	 * Gets image stack from movie file location
	 * @param fileName of an AVI file
	 * @return ImageStack of frames of movie
	 */
	public static ImageStack getImageStack(String fileName) {
		AVI_Reader movieReader = new AVI_Reader();
		ImageStack stack = movieReader.makeStack(fileName, 1, 0, true, false, false);
		return stack;
	}
	/**
	 * Calculates and returns the frame rate of an AVI based on the number of frames and movie length
	 * @param filePath source of AVI
	 * @return frame rate
	 */
	public static double frameRate(String filePath) {
		AVI_Reader movieReader = new AVI_Reader();
		ImageStack stack = movieReader.makeStack(filePath, 1, 0, true, false, false);
		String s = movieReader.getSliceLabel(stack.getSize());
		double time = Double.parseDouble(s.substring(0,s.length()-2));
		return stack.size()/time;
	}

	
}
