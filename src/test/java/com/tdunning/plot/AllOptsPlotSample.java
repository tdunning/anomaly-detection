package com.tdunning.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

/**
 * The first plot example
 * 
 * @author Yuriy Guskov
 * 
 */
public class AllOptsPlotSample {

	public static void main(String[] args) throws IOException {
		// configuring plot
		Plot plot = Plot.plot(Plot.plotOpts().
				title("Hello World").
				titleFont(new Font("Arial", Font.ITALIC, 16)).
				width(1000).
				height(600).
				bgColor(Color.GRAY).
				fgColor(Color.BLUE).
				padding(50). // padding for the whole image
				plotPadding(30). // padding between plot border and the very plot
				labelPadding(20). // padding between label and other elements
				labelFont(new Font("Arial", Font.BOLD, 12)).
				grids(5, 5).
				gridColor(Color.CYAN).
				gridStroke(new BasicStroke(1)).
				tickSize(10).
				legend(Plot.LegendFormat.RIGHT)).
			// configuring axes
			xAxis("x", Plot.axisOpts().
				format(Plot.AxisFormat.TIME_HMS). // use not numbers but HH:MM:SS
				range(0, 5)).
			yAxis("y", Plot.axisOpts().
				range(0, 5)).
			// configuring data series
			series("Data", Plot.data().
				xy(1, 2).
				xy(3, 4),
				Plot.seriesOpts().
					color(Color.BLACK).
					line(Plot.Line.SOLID).
					lineWidth(8).
					marker(Plot.Marker.DIAMOND).
					markerColor(Color.GREEN).
					markerSize(12).
					// not useful here as we have one x and one y axes
					xAxis("x"). // reference to x axis
					yAxis("y"));
		plot.save("sample_all_opts", "png");
	}
}
