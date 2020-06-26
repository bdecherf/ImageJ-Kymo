/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
@Plugin(type = Command.class, name="Join Molecules", menuPath = "Plugins>Kymo>Join molecules")
public class Join implements Command {
	
    @Parameter
    private RoiManager roiManager;

    public void run() {
    	int[] selected = roiManager.getSelectedIndexes();
    	System.out.println("Joining " + selected.length + " molecules.");
    	PolygonRoi first = (PolygonRoi)roiManager.getRoi(selected[0]);
		FloatPolygon firstPolygon = first.getFloatPolygon();
    	for(int i = 1; i<selected.length; i++) {
    		PolygonRoi r = (PolygonRoi)roiManager.getRoi(selected[i]);
    		FloatPolygon polygon = r.getFloatPolygon();
    		for(int ip = 0; ip < polygon.npoints; ip++) {
    			firstPolygon.addPoint(polygon.xpoints[ip], polygon.ypoints[ip]);
    		}    		
    	}
    	
    	Roi r = new PolygonRoi(firstPolygon, Roi.POLYLINE);
    	r.setName(first.getName());
		roiManager.runCommand("delete");
    	roiManager.addRoi(r);
	}
	
}
