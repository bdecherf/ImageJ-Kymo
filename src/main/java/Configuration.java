/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the Unlicense for details:
 *     https://unlicense.org/
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.imagej.ChannelCollection;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayService;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imagej.options.OptionsChannels;
import net.imagej.overlay.EllipseOverlay;
import net.imagej.overlay.LineOverlay;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.PointOverlay;
import net.imagej.overlay.RectangleOverlay;
import net.imagej.overlay.TextOverlay;
import net.imagej.roi.ROIService;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.geom.real.DefaultWritablePointMask;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.scijava.command.Command;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.Colors;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.MessageDialog;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.plugin.RoiRotator;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import io.scif.jj2000.j2k.roi.encoder.ROIMaskGenerator;

/**
 * Adds ROIs to a display.
 * <p>
 * <b>Please note that this API is <em>not</em> stable and will almost certainly
 * change prior to the final <code>2.0.0</code> release!</b>
 * </p>
 */
@Plugin(type = Op.class, menuPath = "Plugins>Kymo>Configuration")
public class Configuration extends AbstractOp {
	
    
    public void run() {
    	Prefs.get("kymo.pixel_duration", 60);
    	
    	
    	GenericDialog gd = new GenericDialog("Add Legent");
    	int width=5;
    	String tpf="50 ms";
        int height=5;
        String scale="200 um";
        int fontSize = 8;
        int lineWidth = Line.getWidth();
        
        gd.addNumericField("Font size: ", fontSize, 0);
        gd.addNumericField("Legend width: ", lineWidth, 0);
        
        gd.addStringField("X legend: ", scale, 0);
        gd.addNumericField("Legend width: ", width, 0);
        
        gd.addStringField("Y legend: ", tpf, 0);
        gd.addNumericField("Legend height: ", height, 0);
        
        gd.showDialog();
        if (gd.wasCanceled()) return;
        
        fontSize = (int)gd.getNextNumber();
        lineWidth = (int)gd.getNextNumber();
        
        scale = gd.getNextString();
        width = (int)gd.getNextNumber();
        
        tpf = gd.getNextString();
        height = (int)gd.getNextNumber();
        
    	//gd = new GenericDialog("Add Legent");
    	//gd.addMessage("Not implemented");
    	//gd.showDialog();
    	

    	ij.gui.Overlay overlay = new ij.gui.Overlay();
        ImagePlus imp = IJ.getImage();
        
        int imageWidth = imp.getWidth();
    	int imageHeight = imp.getHeight();

    	int bottomLeftX = 5+fontSize;
    	int bottomLeftY = imageHeight - 25;
    	
    	TextRoi.setFont(TextRoi.getDefaultFontName(), fontSize, TextRoi.getDefaultFontStyle());
    	Line.setWidth(lineWidth);
    	
    	TextRoi xLegend = new TextRoi(bottomLeftX, bottomLeftY, scale);
		overlay.add(xLegend);
    	overlay.add(new Line(bottomLeftX, bottomLeftY, bottomLeftX+width, bottomLeftY));
    	
    	TextRoi yLegend = new TextRoi(0, bottomLeftY, tpf);
    	yLegend.setRotationCenter(0, bottomLeftY);
    	yLegend.setAngle(90);
        //overlay.add(RoiRotator.rotate(yLegend, -90));
    	overlay.add(yLegend);
    	overlay.add(new Line(bottomLeftX, bottomLeftY, bottomLeftX, bottomLeftY - height));
    	
        imp.setOverlay(overlay);

    	//display.update();
    }
    
	
}
