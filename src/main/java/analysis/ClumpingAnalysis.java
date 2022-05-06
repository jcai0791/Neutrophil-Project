package analysis;

import ij.process.ImageProcessor;
import ij.plugin.filter.ParticleAnalyzer;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.BackgroundSubtracter;
import loci.common.DebugTools;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class ClumpingAnalysis{
	public static String example = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\TestData\\Test";
	public static String resultColumn = "Area";
	public boolean automatic = false;
	public int[] series = {3, 3, 3};
	public String[] seriesTitles = {"N0", "N1", "N2"};
	int channel = 1; //Sytox = 1
	int[] gridSizes = {2,4,6,8};
	static Roi crop = new Roi(500,500,1000,1000); //TopleftX, topLeftY, width, height
	
	private ImagePlus[] imps;
	private int count;

	public String method = null;
	public static void main(String[] args) throws FormatException, IOException {

		ClumpingAnalysis a = new ClumpingAnalysis();
		a.run(example, example);
	}
	public void run(String srcFile, String destFile) throws IOException {

		for(int i = 0; i<gridSizes.length; i++) {
		//Each image gets time, series, measurement
		TreeMap<Integer,ArrayList<String>[]> data = new TreeMap<Integer,ArrayList<String>[]>();
		String fileName = "";
		for(File f : FileUtils.listFiles(new File(srcFile), new String[] {"nd2"}, true)) {
			try {
				String name = f.getName();
				String timeString = patternMatcher("([0-9]){1,3}.?(h|d).?$", name.toLowerCase().replace(".nd2",""));
				fileName = name.substring(0,name.indexOf("-"));
				int time = Integer.parseInt(timeString.substring(0,timeString.length()-1).trim());
				if(timeString.toLowerCase().endsWith("d")) time*=24;
				data.put(time, measurements(f.getAbsolutePath(), destFile, channel, gridSizes[i]));
			} catch(Exception e) {e.printStackTrace();}
		}
		FileWriter writer = new FileWriter(destFile+File.separator+fileName+" Particle Channel "+channel+" "+resultColumn+" GridSize="+gridSizes[i]+".csv");
		BufferedWriter out = new BufferedWriter(writer);
		int maxLength = 0;
		for(Integer time : data.keySet()) {
			for(int series = 0; series<data.get(time).length; series++) {
				out.write(seriesTitles[series]+" "+time+"h,");
				if(data.get(time)[series].size()>maxLength) maxLength = data.get(time)[series].size();
			}
		}
		out.write("\n");

		for(int j = 0; j<maxLength; j++) {
			for(Integer time : data.keySet()) {
				for(int series = 0; series<data.get(time).length; series++) {
					if(j<data.get(time)[series].size()) {
						out.write(data.get(time)[series].get(j)+",");
					}
					else out.write(",");
				}
			}
			out.write("\n");
		}

		out.close();
		}
	}

	public ArrayList<String>[] measurements(String fileName, String destFile, int channel, int gridSize) throws IOException, FormatException{
		//Create folder to store images
		String name = new File(fileName).getName().replace(".nd2","");
		File imageFolder = new File(destFile+File.separator+"Images Channel "+channel+File.separator+name);
		if(!imageFolder.exists()) imageFolder.mkdirs();

		//Import nd2 files
		DebugTools.setRootLevel("OFF");
		ImporterOptions options = new ImporterOptions();
		options.setSplitChannels(false);
		options.setId(fileName);
		options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
		options.setOpenAllSeries(true);
		imps = BF.openImagePlus(options);

		//Prepare
		int numSeries = imps.length;
		int lengthSum = 0;
		for(int i = 0; i<series.length; i++) lengthSum +=series[i];
		if(numSeries!=lengthSum) {
			System.out.println("Invalid Series Lengths");
			return null;
		}

		ArrayList<String>[] data = new ArrayList[series.length];
		for(int i = 0; i<data.length; i++) data[i] = new ArrayList<String>();
		HashMap<Integer, Integer> conditionMap = new HashMap<Integer,Integer>();
		int index = 0;
		int[] seriesCopy = Arrays.copyOf(series, series.length);
		for(int i = 0; i<numSeries; i++) {
			conditionMap.put(i,index);
			seriesCopy[index]--;
			if(seriesCopy[index]==0) index++;
		}
		count = 1;

		for(ImagePlus imp : imps) {

			System.out.println(count+" of "+imps.length);
			count++;
			int series = Integer.parseInt(imp.getProperty("Series").toString());

			ImagePlus[] channels = ChannelSplitter.split(imp);

			double[] calculateResults = calculate(channels[channel].getProcessor(), imageFolder, series, gridSize);
			for(double d : calculateResults) data[conditionMap.get(series)].add(""+d);
			save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Cropped");
			//save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Cropped Triangle");


		}
		return data;
	}

	private String patternMatcher(String regex, String s) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(s);
		String match = "";
		while (matcher.find()) {
			match = matcher.group();
		}
		return match;
	}
	protected static ImageProcessor process(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		ret.setRoi(crop);
		ret.setLineWidth(2);
		ret.setColor(Color.yellow);
		ret.draw(crop);
		new ImagePlus("",ret).show();
		return ret.crop();
	}
	private void save(ImageProcessor ip, String dest, String fileName) {
		FileSaver fs = new FileSaver(new ImagePlus("Whatever", ip));
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
		//Black and white instead of green
	}
	private double[] calculate(ImageProcessor ip, File imageFolder, int series, int gridSize) {
		ImageProcessor cropped = process(ip);
		int[][] image = cropped.getIntArray();
		ArrayList<Double> data = new ArrayList<Double>();
		for(int row = 0; row<image.length/gridSize; row++) {
			for(int col = 0; col<image[0].length/gridSize; col++) {
				double total = 0;
				for(int i = 0; i<gridSize; i++) {
					for(int j = 0; j<gridSize; j++) {
						total+=image[row*gridSize+i][col*gridSize+j];
					}
				}
				data.add(total/(gridSize*gridSize));
			}
		}
		double[] ret = new double[data.size()];
		for(int i = 0; i<data.size(); i++) ret[i] = data.get(i);
		return ret;
		
	}

}
