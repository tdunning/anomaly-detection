package com.tdunning.plot;

import java.awt.Color;
import java.io.IOException;

/**
 * The first plot example
 * 
 * @author Yuriy Guskov
 * 
 */
public class HelloWorld {

	public static void main(String[] args) throws IOException {
		// configuring plot options
		Plot plot = Plot.plot(Plot.plotOpts().
				title("Hello World").
				legend(Plot.LegendFormat.BOTTOM)).
			xAxis("x", Plot.axisOpts().
				range(0, 5)).
			yAxis("y", Plot.axisOpts().
				range(0, 5)).
			series("Data", Plot.data().
				xy(1, 2).
				xy(3, 4),
				Plot.seriesOpts().
					marker(Plot.Marker.DIAMOND).
					markerColor(Color.GREEN).
					color(Color.BLACK));
		plot.save("sample_hello", "png");
	}
}
