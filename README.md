### Description
Code for analyzing the motility of neutrophils. Details are included in PAPER.

### How to use
Create a LocalThresholding object:

    LocalThresholding thresholder = new LocalThresholding();

To change the parameters, use the other constructor:

    LocalThresholding thresholder = new LocalThresholding(3, 5, 15, 0.1, 0.4);

Create a RunAnalysis object with desired parameters:

    RunAnalysis ra = new RunAnalysis(thresholder, srcFolder, destFile, 5, 10);

Run the analysis using:

    ra.run();
    
The resulting csv will be saved to the `destFile` path.