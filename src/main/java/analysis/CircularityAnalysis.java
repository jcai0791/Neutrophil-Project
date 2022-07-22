package analysis;

import ij.process.ImageProcessor;
import ij.plugin.filter.ParticleAnalyzer;
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
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
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
/**
 * Works with single image .nd2 files in timepont subfolders
 * @author MMB
 *
 */
public class CircularityAnalysis extends Component implements ActionListener {
	public static String example = "D:\\Neutrophil Kinetics Ex Vivo\\Experimental Data\\2021-09-24 Under-oil Neutrophils R21 (EasySep-Prostate383+Prostate579-RPMI vs Plasma 10000g)\\02 383-RPMI\\Hoechst+Sytox (10000x)+Annexin V (2.5ul per 100ul)-End Point";
	public static String resultColumn = "Circ.";
	public boolean automatic = false;
	public int channel = 0;
	double minSize = 0;
	double maxSize = Double.POSITIVE_INFINITY;
	double minCirc = 0;
	double maxCirc = 1;
	
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
	public static void main(String[] args) throws FormatException, IOException {
		
		CircularityAnalysis a = new CircularityAnalysis();
		a.run(example, example);
		
		/**
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
		**/
	}
	public void run(String srcFile, String destFile) throws IOException {


		//Each image gets time, series, measurement
		TreeMap<Integer,ArrayList<String>[]> data = new TreeMap<Integer,ArrayList<String>[]>();
		for(File f : FileUtils.listFilesAndDirs(new File(srcFile), new NotFileFilter(TrueFileFilter.INSTANCE), DirectoryFileFilter.DIRECTORY)) {
			if(!f.getName().endsWith("h")||!f.getParent().equals(srcFile)) continue;
			System.out.println(f.getName());
			try {
				String timeString = f.getName();
				int time = Integer.parseInt(timeString.substring(0,timeString.length()-1).trim());
				if(timeString.toLowerCase().endsWith("d")) time*=24;
				data.put(time, measurements(f.getAbsolutePath(), destFile, channel));
			} catch(Exception e) {e.printStackTrace();}
		}
		FileWriter writer = new FileWriter(destFile+File.separator+"Circularity Analysis.csv", false);
		BufferedWriter out = new BufferedWriter(writer);
		int maxLength = 0;
		for(Integer time : data.keySet()) {
			for(int series = 0; series<data.get(time).length; series++) {
				out.write(new File(srcFile).getName()+" Replicate "+(series+1)+" "+time+"h,");
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
		writer.close();

	}

	public ArrayList<String>[] measurements(String fileName, String destFile, int channel) throws IOException, FormatException{
		//Create folder to store images
		String name = new File(fileName).getName();
		File imageFolder = new File(destFile+File.separator+"Thresholded Images"+File.separator+name);
		if(!imageFolder.exists()) imageFolder.mkdirs();
		
		
		
		
		//Prepare
		int numSeries = 0;
		for(File f : FileUtils.listFiles(new File(fileName), new String[] {"nd2"}, true)) {
			String match = patternMatcher("[0-9]{4}",f.getName());
			if(!(match.endsWith("3")||match.endsWith("5")||match.endsWith("7")||match.endsWith("9")||match.endsWith("11"))) continue;
			numSeries++;
		}
		
		ArrayList<String>[] data = new ArrayList[numSeries];
		for(int i = 0; i<data.length; i++) data[i] = new ArrayList<String>();
		
		imps = new ImagePlus[numSeries];
		//Import nd2 files
		int i = 0;
		for(File f : FileUtils.listFiles(new File(fileName), new String[] {"nd2"}, true)) {
			String match = patternMatcher("[0-9]{4}",f.getName());
			if(!(match.endsWith("3")||match.endsWith("5")||match.endsWith("7")||match.endsWith("9")||match.endsWith("11"))) continue;
			System.out.println(match);
			DebugTools.setRootLevel("OFF");
			ImporterOptions options = new ImporterOptions();
			options.setSplitChannels(false);
			options.setId(f.getAbsolutePath());
			options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
			options.setOpenAllSeries(true);
			imps[i] = BF.openImagePlus(options)[0];
			i++;
		}
				
		
		count = 0;
		chosenMethod = null;
		yesToAll = false;
		
		for(ImagePlus imp : imps) {
			count++;
			System.out.println(count+" of "+imps.length);
			
			int series = count-1;
			ImagePlus[] channels = ChannelSplitter.split(imp);
			if(!automatic) {
				if(!yesToAll) chosenMethod = display(channels[channel]);
				if(chosenMethod.equals("Default")) {
					double[] calculateResults = calculate(channels[channel].getProcessor(),0, imageFolder, series);
					//data[series].add(0,"Default");
					for(double d : calculateResults) data[series].add(""+d);
					save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				}
				else if (chosenMethod.equals("Triangle")) {
					double[] calculateResults = calculate(channels[channel].getProcessor(),1, imageFolder, series);
					//data[series].add(0,"Triangle");
					for(double d : calculateResults) data[series].add(""+d);
					save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded Triangle");
				}
			}
			else {
				double[] calculateResults = calculate(channels[channel].getProcessor(),0, imageFolder, series); //Change the 0 to change the default threshold method
				//data[series].add(0,"Default");
				for(double d : calculateResults) data[series].add(""+d);
				save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				//save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded Triangle");
			}

		}
		return data;
	}
	private String display(ImagePlus imp) { 
		//ContrastEnhancer contrast = new ContrastEnhancer();
		ImagePlus clone = imp.duplicate();
		//contrast.stretchHistogram(clone, 0.3);
		ImagePlus originalDisplay = new ImagePlus("Original", clone.getProcessor());
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
		JLabel label2 = new JLabel("Triangle");
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
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("Default dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	protected static ImageProcessor process2(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("Triangle dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	private void save(ImageProcessor ip, String dest, String fileName) {
		FileSaver fs = new FileSaver(new ImagePlus("Whatever", ip));
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
		//Black and white instead of green
	}
	private double[] calculate(ImageProcessor ip, int method, File imageFolder, int series) {
		ImageProcessor thresholded;
		if(method ==0) thresholded = process(ip);
		else thresholded = process2(ip);
		ResultsTable rt = new ResultsTable();
		int options = ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.SHOW_OUTLINES;
		int measurements = Measurements.SHAPE_DESCRIPTORS|Measurements.AREA|Measurements.PERIMETER|Measurements.CIRCULARITY;
		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize, minCirc, maxCirc);
		pa.analyze(new ImagePlus("", thresholded));
		ImagePlus outputImage = pa.getOutputImage();
		save(outputImage.getProcessor(), imageFolder.getAbsolutePath(), "Series "+series+" Outline");
		outputImage.close();
		return rt.getColumnAsDoubles(rt.getColumnIndex(resultColumn));
	}
	private double sum(ImageProcessor ip) {
		int[][] arr = ip.getIntArray();
		double total = 0;
		for(int i = 0; i<arr.length; i++) {
			for(int j = 0; j<arr[0].length; j++) {
				if(arr[i][j]!=0) total++;
			}
		}
		return total;
	}
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==button) {
			method = "Default";
		}
		else if(e.getSource()==button2) {
			method = "Triangle";
		}
		else if(e.getSource()==yesButton) {
			method = "Default";
			yesToAll = true;
		}
		else if(e.getSource()==yesButton2) {
			method = "Triangle";
			yesToAll = true;
		}

	}

}
