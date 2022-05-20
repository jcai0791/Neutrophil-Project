### Description
This project contains three scripts used to process images for the Microtechnology, Medicine and Biology Lab. Details involving the purpose of this code can be found in PAPER.

### Getting Started

First, download Eclipse IDE or another IDE or your choice. To download this project, click Code → Download ZIP. 
Extract the downloaded zipped file somewhere.
In Eclipse, go to File → Import → Maven → Existing Maven Projects.
Choose the unzipped Code and check the Project. Finish the dialog.
Now, there should be a `Neutrophil-Project` project in the explorer.
There are several dependencies that may not automatically download. To resolve those errors, go to Eclipse preferences, Maven -> User Settings and change the path of the settings to the settings.xml file in the project. This will tell Eclipse to download the dependencies automatically from the Open Microscopy Artifactory.

### Motility Analysis

Motility analysis involves thresholding.LocalThresholding and MotilityAnalysis.java. They contain code for analyzing the cell motility. Bright-field videos (.avi, 30X magnification, ～8 FPS, 10 s) were recorded on a Nikon Ti. The videos were analyzed using a customized code in Java. 

#### How to use
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


Navigate to `src/main/java/analysis/RunAnalysis.java` and start coding in the `main` method.
Full documentation is available in the `doc` folder at `doc/analysis/RunAnalsis.html` and `doc/thresholding/LocalThresholding.html`.

### Particle Analysis
Particle analysis uses the ImageJ particle analyzer to measure the areas of particles. There is a multiplier to convert the result from pixels to square microns. It includes a GUI that allows users to choose between two different thresholding methods. There are many variables that can be changed. 

`resultColumn` is the name of the column which will be calculated and saved into the resulting csv file. Examples include "Area", "Circularity", and "Perimeter", and any measurement that's an option in ImageJ.

`automatic` determines whether the GUI is displayed or not. If it is set to true, the script will be run using Default for all images. If it is set to fault, the GUI will pop up when run.

`series` is an array with the number of series you want in each replicate. This is intended for use with replicates. For example, say you have 3 conditions each with 3 replicates in a single nd2 file. If you set `series` to {3,3,3}, the first three series will be put into the first column, the next three in the second column, and the last three in the third column. This results in each condition taking up a single column instead of three. Make sure that the sum of the numbers in the array equals the total number of images.

`seriesTitles` builds off of `series` so that each column can be named instead of just Condition 0, Condition 1, Condition 2. Make sure that the length of `seriesTitles` matches `series`. 

`channel` determines which channel of the nd2 file the script will use. Note that the first channel is channel 0.

`MICRONS_PER_PIXEL` is a multiplier to the final result to convert it from pixels to microns squared.

#### How to use
Set the variables as desired above, then run. A file chooser window will pop up. Select the folder containing all the nd2 files you want to process. Note that the script does a deep search for nd2 files, so make sure only the nd2 files you want to process are present in the folder and its subfolders.

### Clumping Analysis
Clumping analysis uses a novel algorithm to determine how much clumping there is in an image. 

Many of the options are also part of Particle Analysis and are described above. These are the unique ones:

`gridSizes` is an array of different grid sizes to test. Each grid size will be saved in a separate csv.

`crop` is an ROI. The algorithm firsts crops the image to this ROI. The format is (top left X coordinate, top left Y coordinate, width of ROI, height of ROI). 


#### How to use
Set the options as desired, then run. 
