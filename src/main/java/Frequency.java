/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import net.imagej.ChannelCollection;
import net.imagej.Dataset;
import net.imagej.DefaultDatasetService;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.convert.normalizeScale.NormalizeScaleRealTypes;
import net.imagej.options.OptionsChannels;
import net.imagej.overlay.EllipseOverlay;
import net.imagej.overlay.LineOverlay;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.PointOverlay;
import net.imagej.overlay.RectangleOverlay;
import net.imagej.roi.ROIService;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.geom.real.DefaultWritablePointMask;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.convert.ConvertService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.Colors;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.ImageConverter;
import io.scif.jj2000.j2k.roi.encoder.ROIMaskGenerator;

/**
 * Join 2 molecules from RoiManager
 * <p>
 * <b>Please note that this API is <em>not</em> stable and will almost certainly
 * change prior to the final <code>2.0.0</code> release!</b>
 * </p>
 */
@Plugin(type = Command.class, name="Frequency graph", menuPath = "Plugins>Kymo>Frequency graph")
public class Frequency implements Command {
	
    @Parameter
    private RoiManager roiManager;

    class Duration {
    	Roi roi;
    	double duration;
    	Duration(Roi r, double duration) {
    		this.roi = r;
    		this.duration = duration;
    	}
    }
    
    public void run() {
    	System.out.println("Calc frequency graph");
    	Roi[] rois = roiManager.getRoisAsArray();
    	List<Duration> durations= new ArrayList<Frequency.Duration>();
    	double minDuration = Double.MAX_VALUE;
    	double maxDuration = 0;
    	
    	for(int i = 0; i<rois.length; i++) {
    		Roi r = rois[i];
    		double duration = r.getBounds().getHeight();
    		durations.add(new Duration(r, duration));
    		if(minDuration > duration) {
    			minDuration = duration;
    		}
    		if(maxDuration < duration) {
    			maxDuration = duration;
    		}
    	}
    	
    	System.out.println("Duration: min: " + minDuration+ ", max: " + maxDuration);

    	GenericDialog gd = new GenericDialog("Frequency configuration");
        int binWidth = 5;
        gd.addNumericField("Bin Width: ", binWidth, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return;        
        binWidth= (int)gd.getNextNumber();
        
    	Map<Integer, Integer> values = new HashMap<Integer, Integer>();
    	int maxNum = 0;
    	int maxSegment = 0;
    	for(Duration d: durations) {
    		int durationSegment = (int)(d.duration / binWidth);
    		int num = values.containsKey(durationSegment) ? values.get(durationSegment) : 0;
    		values.put(durationSegment, num+1);
    		if(num > maxNum)
    			maxNum = num;
    		if(maxSegment < durationSegment)
    			maxSegment = durationSegment;
    	}
    	for(int i = 0; i < maxSegment; i++) {
    		if(!values.containsKey(i)) {
    			values.put(i, 0);
    		}
    	}
    	
    	PlotWindow.noGridLines = false; // draw grid lines
        Plot plot = new Plot("Frequency","Duration","Num molecules");
        plot.setLimits(minDuration, maxDuration, 0, maxNum + 5);
        plot.setLineWidth(2);

        //ArrayList<Float> xValues = new ArrayList<Float>();
        //for(int v: values.keySet()) {
        //	xValues.add((float)v);
        //}
        ArrayList<Integer> xIntValues = new ArrayList<Integer>(values.keySet());
        Collections.sort(xIntValues);
        //ArrayList<Float> yValues = new ArrayList<Float>();
        double[] xValues = new double[xIntValues.size()];
        double[] yValues = new double[xIntValues.size()];
        
        ResultsTable table = new ResultsTable();

        int num = 0;
        for(int x: xIntValues) {
            table.incrementCounter();
			table.addValue("duration", x * binWidth);
			table.addValue("num molecules", values.get(x));

        	
        	xValues[num] = (double)(x * binWidth);
        	yValues[num] = (double)values.get(x);
        	System.out.println(x+": "+values.get(x));
        	num++;
        }
		table.show("Frequency");

        
        plot.setColor(Color.red);
        plot.add("line", xValues,yValues);
        
        plot.show();

	}
	
}
