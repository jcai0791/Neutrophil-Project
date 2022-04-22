package analysis;

import ij.process.ImageProcessor;
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
import ij.io.FileSaver;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.BackgroundSubtracter;
import loci.common.DebugTools;
import loci.formats.FormatException;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

public class AreaFraction extends Component implements ActionListener {
	public static String example = "C:\\Users\\MMB\\Desktop\\Joseph Cai\\TestData";
	int channel = 1;
	public boolean automatic = false;
	
	
	
	public boolean yesToAll = false;
	public String chosenMethod;
	public JFrame frame;
	public ImageCanvas window;
	public ImageCanvas window2;
	public ImageCanvas original;
	public JButton button;
	public JButton button2;
	public JButton yesButton;
	public JButton yesButton2;
	public ImagePlus[] imps;
	public int count;

	public String method = null;
	public static void main(String[] args) throws FormatException, IOException {
		AreaFraction a = new AreaFraction();
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
		TreeMap<Integer,String[][]> data = new TreeMap<Integer,String[][]>();
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
		FileWriter writer = new FileWriter(destFile+File.separator+fileName+".csv");
		BufferedWriter out = new BufferedWriter(writer);
		out.write("Time (hours),");
		for(int i = 1; i<=data.firstEntry().getValue().length; i++) {
			out.write("Series "+i+",");
		}
		out.write("\n");
		for(Integer t : data.keySet()) {
			out.write(t+",");
			for(String[] s : data.get(t)) out.write(s[0]+",");
			out.write("\n");
		}
		out.write("\n\n");
		out.write("Time (hours),");
		for(int i = 1; i<=data.firstEntry().getValue().length; i++) {
			out.write("Series "+i+",");
		}
		out.write("\n");
		for(Integer t : data.keySet()) {
			out.write(t+",");
			for(String[] s : data.get(t)) out.write(s[1]+",");
			out.write("\n");
		}
		out.close();

	}

	public String[][] measurements(String fileName, String destFile, int channel) throws IOException, FormatException{
		//Create folder to store images
		String name = new File(fileName).getName().replace(".nd2","");
		File imageFolder = new File(destFile+File.separator+"Images"+File.separator+name);
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
		String[][] data = new String[numSeries][2];
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
					data[series][0] = ""+calculate(channels[channel].getProcessor(),0);
					data[series][1] = "Default";
					save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				}
				else if (chosenMethod.equals("MaxEntropy")) {
					data[series][0] = ""+calculate(channels[channel].getProcessor(),1);
					data[series][1] = "MaxEntropy";
					save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded MaxEntropy");
				}
			}
			else {
				data[series][0] = ""+calculate(channels[channel].getProcessor(),0);
				data[series][1] = "Default";
				save(process(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded");
				save(process2(channels[channel].getProcessor()), imageFolder.getAbsolutePath(), "Series "+series+" Thresholded MaxEntropy");
			}

		}
		return data;
	}
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
		ret.setAutoThreshold("MaxEntropy dark");
		ret.threshold((int) ret.getMinThreshold());
		return ret;
	}
	private void save(ImageProcessor ip, String dest, String fileName) {
		FileSaver fs = new FileSaver(new ImagePlus("Whatever", ip));
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
		//Black and white instead of green
	}
	private double calculate(ImageProcessor ip, int method) {
		ImageProcessor thresholded;
		if(method ==0) thresholded = process(ip);
		else thresholded = process2(ip);
		double total = thresholded.getWidth()*thresholded.getHeight();
		return sum(thresholded)/total/((2.4*2.4)/(3.34*3.33));
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
