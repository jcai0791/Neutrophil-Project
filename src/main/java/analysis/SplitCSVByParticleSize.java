package analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SplitCSVByParticleSize {
	/**
	 * Creates 3 new CSVs based on the particle sizes
	 * Input file has to have:
	 * 1. Header line
	 * 2. Columns of data formatted as doubles
	 * @param inputFile
	 * @throws IOException
	 */
	public static void run(String inputFile) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(inputFile));
		String s = in.readLine();
		String[] headers = s.split(",");
		ArrayList<String>[] data = new ArrayList[headers.length];
		s = in.readLine();
		while(s!=null) {
			
		}
	}
}
