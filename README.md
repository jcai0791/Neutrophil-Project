### Description
Code for analyzing the cell motility. Bright-field videos (.avi, 30X magnification, ～8 FPS, 10 s) were recorded on a Nikon Ti. The videos were analyzed using a customized code in Java. Details are included in PAPER.

### How to use
Create a LocalThresholding object:

    LocalThresholding thresholder = new LocalThresholding();

To change the parameters, use the other constructor:

    LocalThresholding thresholder = new LocalThresholding(3, 15, 0.1, 0.4, 5);

Create a RunAnalysis object with desired parameters:

    RunAnalysis ra = new RunAnalysis(thresholder, srcFolder, destFile, 5, 10);

Run the analysis using:

    ra.run();
    
The resulting csv will be saved to the `destFile` path. 

The explanation of these parameters can be found in the methods section of the paper.

### Getting Started

First, download Eclipse IDE or another IDE or your choice. To download this project, click Code → Download ZIP. 
Extract the downloaded zipped file somewhere.
In Eclipse, go to File → Import → Maven → Existing Maven Projects.
Choose the unzipped Code and check the Project. Finish the dialog.
Now, there should be a `motility-analysis` project in the explorer.
Navigate to `src/main/java/analysis/RunAnalysis.java` and start coding in the `main` method.
Full documentation is available in the `doc` folder at `doc/analysis/RunAnalsis.html` and `doc/thresholding/LocalThresholding.html`.
