package thresholding;

import java.io.File;

import analysis.MotilityAnalysis;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.plugins.Median2DBox;
import net.haesleinhuepf.clij2.plugins.SumOfAllPixels;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.plugins.LocalThresholdPhansalkar;
/**
 * Used for thresholding and finding the difference between images
 * @author MMB
 *
 */
public class LocalThresholding {
	private CLIJ2 clij;
	private CLIJx clijx;
	public int blurRadius;
	public int despeckleRadius;
	public int thresholdRadius;
	public float thresholdParameter1;
	public float thresholdParameter2;
	//Read about Phansalkar threshold here: https://imagej.net/plugins/auto-local-threshold
	
	/**
	 * Creates a thresholder with default parameters
	 */
	public LocalThresholding() {
		this.clij = CLIJ2.getInstance();
		this.clijx = CLIJx.getInstance();
		this.blurRadius = 3;
		this.despeckleRadius = 5;
		this.thresholdRadius = 15;
		this.thresholdParameter1 = (float)0.1;
		this.thresholdParameter2 = (float)0.4;
	}
	/**
	 * Creates a thresholder with custom parameters
	 * @param blurRadius radius of gaussian blur (default value 3)
	 * @param thresholdRadius radius of Phansalkar threshold (default value 15)
	 * @param thresholdParameter1 Phansalkar threshold k (default value 0.1)
	 * @param thresholdParameter2 Phansalkar threshold r (default value 0.4)
	 * @param despeckleRadius radius of median filter (default value 5)
	 */
	public LocalThresholding(int blurRadius, int thresholdRadius, double thresholdParameter1, double thresholdParameter2, int despeckleRadius) {
		this();
		this.blurRadius = blurRadius;
		this.despeckleRadius = despeckleRadius;
		this.thresholdRadius = thresholdRadius;
		this.thresholdParameter1 = (float)thresholdParameter1;
		this.thresholdParameter2 = (float)thresholdParameter2;
	}
	
	/**
	 * Gets thresholded image as ClearCLBuffer
	 * @param src source image
	 * @return thresholded image
	 */
	private ClearCLBuffer getThresholdedBuffer(ImagePlus src) {
		ClearCLBuffer s1 = clij.push(src);
		ClearCLBuffer s1smooth = clij.create(s1);
		ClearCLBuffer s1thresholded = clij.create(s1);
		ClearCLBuffer s1despeckle = clij.create(s1);
		//LocalThresholdBernsen.localThresholdBernsen(clijx, s1smooth, s1thresholded, 15, 0);
		clij.gaussianBlur2D(s1, s1smooth, blurRadius, blurRadius);
		LocalThresholdPhansalkar.localThresholdPhansalkar(clijx, s1smooth, s1thresholded, thresholdRadius, thresholdParameter1, thresholdParameter2);
		Median2DBox.median2DBox(clij, s1thresholded, s1despeckle, despeckleRadius, despeckleRadius);
		ClearCLBuffer s1output = clij.create(s1);
		clij.multiplyImageAndScalar(s1despeckle,s1output,255);
		s1.close();
		s1smooth.close();
		s1thresholded.close();
		s1despeckle.close();
		return s1output;
	}
	/**
	 * Gets thresholded image as ImagePlus
	 * @param src source image
	 * @return thresholded image
	 */
	public ImagePlus getThresholdedImage(ImagePlus src) {
		ClearCLBuffer s1 = getThresholdedBuffer(src);
		ImagePlus ret = clij.pull(s1);
		s1.close();
		return ret;
	}
	
	/**
	 * Calculates number of pixels difference between the thresholds of two imagepluses
	 * @param src1 first image
	 * @param src2 second image
	 * @return difference
	 */
	public double combined(ImagePlus src1, ImagePlus src2) {
		ClearCLBuffer s1 = getThresholdedBuffer(src1);
		ClearCLBuffer s2 = getThresholdedBuffer(src2);
		ClearCLBuffer difference = clij.create(s1);
		clij.absoluteDifference(s1,s2, difference);
		double ret = SumOfAllPixels.sumOfAllPixels(clij, difference);
		s1.close();
		s2.close();
		difference.close();
		return ret/255;
	}
	
	/**
	 * Saves a slice as a tif
	 * @param source source file path (should end in .avi)
	 * @param dest destination (folder)
	 * @param fileName destination file name (no extension)
	 * @param slice index of slice to be saved
	 */
	public void save(String source, String dest, String fileName, int slice) {
		ImageStack stack = MotilityAnalysis.getImageStack(source);
		ImagePlus image1 = (getThresholdedImage(new ImagePlus(fileName+" Thresholded", stack.getProcessor(slice).convertToByteProcessor())));
		FileSaver fs = new FileSaver(image1);
		fs.saveAsTiff(dest+File.separator+fileName+".tif");
	}
}
