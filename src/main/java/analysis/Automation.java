package analysis;

import ij.process.ImageProcessor;

import java.awt.BorderLayout;
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
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.BackgroundSubtracter;
import loci.common.DebugTools;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class Automation implements ActionListener{
	public static String example = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\TestData";
	public boolean automatic = false;
	public JFrame frame;
	public ImageCanvas window;
	public ImageCanvas window2;
	public JButton button;
	public JButton button2;
	public ImagePlus[] imps;
	public int count;
	public static void main(String[] args) throws FormatException, IOException {
		Automation a = new Automation();
		a.run(example, example);
	}
	public void run(String srcFile, String destFile) throws IOException {


		//Each image gets time, series, measurement
		TreeMap<Integer,String[]> data = new TreeMap<Integer,String[]>();
		String fileName = "";
		for(File f : FileUtils.listFiles(new File(srcFile), new String[] {"nd2"}, true)) {
			try {
				String name = f.getName();
				String timeString = patternMatcher("([0-9]){1,3}.?(h|d).?$", name.toLowerCase().replace(".nd2",""));
				fileName = name.substring(0,name.indexOf("-"));
				int time = Integer.parseInt(timeString.substring(0,timeString.length()-1).trim());
				if(timeString.toLowerCase().endsWith("d")) time*=24;
				data.put(time, measurements(f.getAbsolutePath(), destFile, 1));

			} catch(Exception e) {e.printStackTrace();}
		}
		FileWriter writer = new FileWriter(destFile+File.separator+fileName+".csv");
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Time (hours),");
		for(int i = 1; i<=data.firstEntry().getValue().length; i++) {
			out.write("Series "+i+",");
		}
		out.write("\n");
		for(Integer t : data.keySet()) {
			out.write(t+",");
			for(String s : data.get(t)) out.write(s+",");
			out.write("\n");
		}
		out.close();

	}
	public String[] measurements(String fileName, String destFile, int channel) throws IOException, FormatException{
		String name = new File(fileName).getName().replace(".nd2","");
		File imageFolder = new File(destFile+File.separator+"Images"+File.separator+name);
		if(!imageFolder.exists()) imageFolder.mkdirs();
		DebugTools.setRootLevel("OFF");
		ImporterOptions options = new ImporterOptions();
		options.setSplitChannels(false);
		options.setId(fileName);
		options.setStackFormat(ImporterOptions.VIEW_HYPERSTACK);
		options.setOpenAllSeries(true);
		imps = BF.openImagePlus(options);
		int numSeries = imps.length;
		String[] data = new String[numSeries];
		count = 1;
		for(ImagePlus imp : imps) {
			System.out.println(count+" of "+imps.length);
			count++;
			int series = Integer.parseInt(imp.getProperty("Series").toString());

			ImagePlus[] channels = ChannelSplitter.split(imp);
			data[series] = ""+calculate(channels[channel].getProcessor());
			save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
			save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded MaxEntropy");

		}
		return data;
	}
	private void display(ImagePlus[] imps, int count) {
		ImagePlus imp = imps[count-1];
		int series = Integer.parseInt(imp.getProperty("Series").toString());
		ImagePlus[] channels = ChannelSplitter.split(imp);


		ImagePlus display = new ImagePlus("Thresholded",process(channels[1].getProcessor()));
		window = new ImageCanvas(display);
		window.setMagnification(700.0/display.getWidth());
		window.setSize(700, 700);
		ImagePlus display2 = new ImagePlus("Thresholded",process2(channels[1].getProcessor()));
		window2 = new ImageCanvas(display2);
		window2.setMagnification(700.0/display.getWidth());
		window2.setSize(700, 700);
		frame = new JFrame("Test");
		JPanel contentPane = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(5,5,5,5);
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
		frame.setContentPane(contentPane);
		gbc.gridx = 0;
		gbc.gridy = 0;
		frame.getContentPane().add(label,gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		frame.getContentPane().add(label2,gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		frame.getContentPane().add(window,gbc);
		gbc.gridx = 1;
		gbc.gridy = 1;
		frame.getContentPane().add(window2,gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		frame.getContentPane().add(button,gbc);
		gbc.gridx = 1;
		gbc.gridy = 2;
		frame.getContentPane().add(button2,gbc);
		frame.pack();
		frame.setVisible(true);
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
	private ImageProcessor process(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("Default dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	private ImageProcessor process2(ImageProcessor ip) {
		ImageProcessor ret = (ImageProcessor) ip.convertToByteProcessor().clone();
		BackgroundSubtracter bs = new BackgroundSubtracter();
		bs.rollingBallBackground(ret, 50, false, false, false, true, false);
		ret.setAutoThreshold("MaxEntropy dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	private void save(ImageProcessor ip, String dest, String fileName) {
		FileSaver fs = new FileSaver(new ImagePlus("Whatever", ip));
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
	}
	private double calculate(ImageProcessor ip) {
		ImageProcessor thresholded = process(ip);
		double total = thresholded.getWidth()*thresholded.getHeight();
		return sum(thresholded)/total;
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

		}
		else if(e.getSource()==button2) {

		}
		frame.dispose();
		System.out.println(count+" Of "+imps.length);
		display(imps, count++);

	}

}
