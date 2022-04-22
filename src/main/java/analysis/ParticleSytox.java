package analysis;

import ij.process.ImageProcessor;
import ij.plugin.filter.ParticleAnalyzer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.FileUtils;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.BackgroundSubtracter;
import loci.common.DebugTools;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class ParticleSytox extends Component implements ActionListener {
	public static String example = "D:\\Experiments\\2021-08-10 Under-oil Neutrophils R12 (Prostate vs Healthy-EasySep&Polarization)\\Morphology Analysis";
	public static String resultColumn = "Area";
	public boolean automatic = false;
	public int[] series = {3,3,3,3};
	public String[] seriesTitles = {"Condition 0", "Condition 1", "Condition 2", "Condition 3"};
	int channel = 1;
	public final double MICRONS_PER_PIXEL = 1.63;
	
	private boolean yesToAll = false;
	private String chosenMethod;
	private JFrame frame;
	private ImageCanvas window;
	private ImageCanvas window2;
	private ImageCanvas original;
	private JButton button;
	private JButton button2;
	private JButton yesButton;
	private JButton yesButton2;
	private ImagePlus[] imps;
	private int count;

	public String method = null;
	/**
	 * Main method calls run
	 * @param args
	 */
	public static void main(String[] args) throws FormatException, IOException {
		ParticleSytox a = new ParticleSytox();
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setCurrentDirectory(new File(System.getProperty("user.home")));
		int result = fc.showOpenDialog(a);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = fc.getSelectedFile();
		    System.out.println("Selected file: " + selectedFile.getAbsolutePath());
		    a.run(selectedFile.getAbsolutePath(), selectedFile.getAbsolutePath());
		}
		else System.out.println("Invalid Folder");
		
	}
	public void run(String srcFile, String destFile) throws IOException {


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
				data.put(time, measurements(f.getAbsolutePath(), destFile, channel));
			} catch(Exception e) {e.printStackTrace();}
		}
		FileWriter writer = new FileWriter(destFile+File.separator+fileName+" Particle Channel "+channel+" "+resultColumn+".csv");
		BufferedWriter out = new BufferedWriter(writer);
		int maxLength = 0;
		for(Integer time : data.keySet()) {
			for(int series = 0; series<data.get(time).length; series++) {
				out.write("Condition "+series+" "+time+"h,");
				if(data.get(time)[series].size()>maxLength) maxLength = data.get(time)[series].size();
			}
		}
		out.write("\n");
		
		for(int i = 0; i<maxLength; i++) {
			for(Integer time : data.keySet()) {
				for(int series = 0; series<data.get(time).length; series++) {
					if(i<data.get(time)[series].size()) {
						out.write(data.get(time)[series].get(i)+",");
					}
					else out.write(",");
				}
			}
			out.write("\n");
		}
		
		out.close();

	}
	
	public ArrayList<String>[] measurements(String fileName, String destFile, int channel) throws IOException, FormatException{
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
		chosenMethod = null;
		yesToAll = false;
		
		for(ImagePlus imp : imps) {

			System.out.println(count+" of "+imps.length);
			count++;
			int series = Integer.parseInt(imp.getProperty("Series").toString());

			ImagePlus[] channels = ChannelSplitter.split(imp);
			if(!automatic) {
				if(!yesToAll) chosenMethod = display(channels[channel]);
				if(chosenMethod.equals("Default")) {
					double[] calculateResults = calculate(channels[channel].getProcessor(),0, imageFolder, series);
					data[conditionMap.get(series)].add(0,"Default");
					for(double d : calculateResults) data[conditionMap.get(series)].add(""+(d*(MICRONS_PER_PIXEL)*(MICRONS_PER_PIXEL))); //Convert to microns 4x (1.63 microns per pixel)
					save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				}
				else if (chosenMethod.equals("MaxEntropy")) {
					double[] calculateResults = calculate(channels[channel].getProcessor(),1, imageFolder, series);
					data[conditionMap.get(series)].add(0,"MaxEntropy");
					for(double d : calculateResults) data[conditionMap.get(series)].add(""+(d*(MICRONS_PER_PIXEL)*(MICRONS_PER_PIXEL)));
					save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded MaxEntropy");
				}
			}
			else {
				double[] calculateResults = calculate(channels[channel].getProcessor(),0, imageFolder, series); //Change the 0 to change the default threshold method
				data[conditionMap.get(series)].add(0,"Default");
				for(double d : calculateResults) data[conditionMap.get(series)].add(""+(d*(MICRONS_PER_PIXEL)*(MICRONS_PER_PIXEL)));
				save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				//save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded MaxEntropy");
			}

		}
		return data;
	}
	/**
	 * Displays the GUI
	 * @param imp to be displayed
	 * @return chosen method
	 */
	private String display(ImagePlus imp) { 
		ImagePlus originalDisplay = new ImagePlus("Original", imp.getProcessor());
		original = new ImageCanvas(originalDisplay);
		original.setMagnification(600.0/originalDisplay.getWidth());
		original.setSize(600,600);
		ImagePlus display = new ImagePlus("Thresholded",process(imp.getProcessor()));
		window = new ImageCanvas(display);
		window.setMagnification(600.0/display.getWidth());
		window.setSize(600, 600);
		ImagePlus display2 = new ImagePlus("Thresholded",process2(imp.getProcessor()));
		window2 = new ImageCanvas(display2);
		window2.setMagnification(600.0/display.getWidth());
		window2.setSize(600, 600);
		frame = new JFrame("Test");
		JPanel contentPane = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5,5,5,5);
		JLabel olabel = new JLabel("Original");
		olabel.setSize(200,40);
		JLabel label = new JLabel("Default");
		label.setSize(200,40);
		JLabel label2 = new JLabel("MaxEntropy");
		label2.setSize(200,40);
		button = new JButton("Choose This");
		button.setSize(200,40);
		button.addActionListener(this);
		button2 = new JButton("Choose This");
		button2.setSize(200,40);
		button2.addActionListener(this);
		yesButton = new JButton("Yes To All");
		yesButton.setSize(200,40);
		yesButton.addActionListener(this);
		yesButton2 = new JButton("Yes To All");
		yesButton2.setSize(200,40);
		yesButton2.addActionListener(this);
		frame.setContentPane(contentPane);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.5;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		frame.getContentPane().add(olabel,gbc);
		gbc.gridx = 2;
		gbc.gridy = 0;
		frame.getContentPane().add(label,gbc);
		gbc.gridx = 4;
		gbc.gridy = 0;
		frame.getContentPane().add(label2,gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		frame.getContentPane().add(original, gbc);
		gbc.gridx = 2;
		gbc.gridy = 1;
		frame.getContentPane().add(window,gbc);
		gbc.gridx = 4;
		gbc.gridy = 1;
		frame.getContentPane().add(window2,gbc);
		
		gbc.gridx = 2;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		frame.getContentPane().add(button,gbc);
		gbc.gridx = 3;
		gbc.gridy = 2;
		frame.getContentPane().add(yesButton,gbc);
		gbc.gridx = 4;
		gbc.gridy = 2;
		frame.getContentPane().add(button2,gbc);
		gbc.gridx = 5;
		gbc.gridy = 2;
		frame.getContentPane().add(yesButton2,gbc);
		
		frame.pack();
		frame.setVisible(true);
		while(method == null) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		String temp = method;
		method = null;
		frame.dispose();
		return temp;
	}
	/**
	 * Returns substring that matches regular expression
	 * @param regex to match
	 * @param s to search
	 * @return substring
	 */
	private String patternMatcher(String regex, String s) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(s);
		String match = "";
		while (matcher.find()) {
			match = matcher.group();
		}
		return match;
	}
	/**
	 * Performs background subtraction and thresholding
	 * @param ip image to process
	 * @return processed image
	 */
	protected static ImageProcessor process(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("Default dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	/**
     * Performs background subtraction and thresholding
     * @param ip image to process
     * @return processed image
     */
	protected static ImageProcessor process2(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("MaxEntropy dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	/**
	 * Saves image at a location
	 * @param ip image to save
	 * @param dest location of folder
	 * @param fileName name of saved file
	 */
	private void save(ImageProcessor ip, String dest, String fileName) {
		FileSaver fs = new FileSaver(new ImagePlus("Whatever", ip));
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
		//Black and white instead of green
	}
	/**
	 * Does Particle Analysis measurements on an image
	 * @param ip image to measure
	 * @param method either default (0) or maxEntropy (1)
	 * @param imageFolder folder to save images in
	 * @param series series number of image (affects output image name)
	 * @return array of particle measurements
	 */
	private double[] calculate(ImageProcessor ip, int method, File imageFolder, int series) {
		ImageProcessor thresholded;
		if(method ==0) thresholded = process(ip);
		else thresholded = process2(ip);
		ResultsTable rt = new ResultsTable();
		int options = ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.SHOW_OUTLINES;
		int measurements = Measurements.SHAPE_DESCRIPTORS|Measurements.AREA|Measurements.PERIMETER;
		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, 0.0, Double.POSITIVE_INFINITY, 0.0, 1.0);
		pa.analyze(new ImagePlus("", thresholded));
		ImagePlus outputImage = pa.getOutputImage();
		save(outputImage.getProcessor(), imageFolder.getAbsolutePath(), "Series "+series+" Outline");
		outputImage.close();
		return rt.getColumnAsDoubles(rt.getColumnIndex(resultColumn));
	}
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==button) {
			method = "Default";
		}
		else if(e.getSource()==button2) {
			method = "MaxEntropy";
		}
		else if(e.getSource()==yesButton) {
			method = "Default";
			yesToAll = true;
		}
		else if(e.getSource()==yesButton2) {
			method = "MaxEntropy";
			yesToAll = true;
		}

	}

}
