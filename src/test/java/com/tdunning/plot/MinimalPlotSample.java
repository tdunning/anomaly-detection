package com.tdunning.plot;

import java.io.IOException;

/**
 * Minimal plot sample
 * 
 * @author Yuriy Guskov
 *
 */
public class MinimalPlotSample {

	public static void main(String[] args) throws IOException {
		// configuring everything by default
		Plot plot = Plot.plot(null).
			// setting data
			series(null, Plot.data().
				xy(1, 2).
				xy(3, 4), null);
		// saving sample_minimal.png
		plot.save("sample_minimal", "png");
	}
}
