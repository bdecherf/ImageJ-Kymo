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
import ij.gui.Toolbar;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.frame.RoiManager;
import ij.plugin.tool.PlugInTool;
import ij.process.Blitter;
import ij.process.FloatPolygon;
import ij.process.ImageConverter;
import io.scif.jj2000.j2k.roi.encoder.ROIMaskGenerator;

/**
 * Adds ROIs to a display.
 * <p>
 * <b>Please note that this API is <em>not</em> stable and will almost certainly
 * change prior to the final <code>2.0.0</code> release!</b>
 * </p>
 */
@Plugin(type = Op.class, menuPath = "Plugins>Kymo>Find Molecules")
public class FindSegments extends AbstractOp {
	static final int MAX_Y_BLANK = 4;
	static final int MAX_X_BLANK = 3;
	
	
    @Parameter
    private Dataset currentData;
    
    @Parameter
    private ImagePlus source;
    
	ImagePlus imp;

    
    @Parameter
    private ImageJ ij;
    

    @Parameter
    private ImageDisplay display;
    
    @Parameter
    private UIService uiService;

    @Parameter
    private ROIService roiService;

    @Parameter
    private OptionsService optionsService;

    @Parameter
    private OverlayService overlayService;
    
	@Parameter
	private OpService ops;

    @Parameter
    private ConvertService convertService;
    
	class Point {
		public int x,y;
		Point(int x, int y) {this.x = x; this.y = y;}
	}
	
	class Molecule {
		List<Point> points = new ArrayList<Point>();
		List<Molecule> conflcted = new ArrayList<Molecule>();
		
		Molecule(int x, int y) {points.add(new Point(x, y));}
		
		boolean addPoint(int x, int y) {
			for(Point point: points) {
				if(point.y >= y - MAX_Y_BLANK) {
					int lastx = point.x; 
				
					if(x >= lastx - MAX_X_BLANK && x <= lastx+MAX_X_BLANK) {
						points.add(new Point(x, y));
						return true;
					}
				}
			}
			return false;
		}
		
		void addConflict(Molecule m) {if(!conflcted.contains(m)) {conflcted.add(m);}}
		
		boolean isBiggest() {
			for(Molecule m: conflcted) {
				if(m.points.size() > points.size())
					return false;
			}
			return true;
		}
		
		List<Point> getLines() {
			int lineNum = 0;
			//RandomAccess access = currentData.randomAccess();

			int currentY = 0;
			Point currentPoint = null;
			Point lastPoint = null;
			long currentMaxIntensity = 0;
			List<Point> lines = new ArrayList<FindSegments.Point>();
			for(Point p: points) {
				if(p.y != currentY) {
					//access.setPosition(p.y, 1);
					currentY = p.y;
					if(currentPoint != null) {
						if(lastPoint == null || Math.abs(currentPoint.x - lastPoint.x)<6) {
							lines.add(currentPoint);
							lastPoint = currentPoint;
						}
						
					}
					currentMaxIntensity = 0;
					currentPoint = null;
				}
				//access.setPosition(p.x, 0);
				long intensity = (long)imp.getPixel((int)p.x, (int)currentY)[0];
				//long intensity = ((UnsignedByteType)access.get()).getIntegerLong();
				if(intensity > currentMaxIntensity) { 
					currentMaxIntensity = intensity;
					currentPoint = p;
				}
			}
			if(currentPoint != null && (lastPoint == null || Math.abs(currentPoint.x - lastPoint.x)<3)) {
				lines.add(currentPoint);
			}
			return lines;
		}
	}


	
	List<Molecule> molecules = new ArrayList<FindSegments.Molecule>();
	
	void addPoint(int x, int y) {
		Molecule found = null;
		
		for(Molecule m: molecules) {
			if(m.addPoint(x, y)) {
				if(found != null) {
					found.addConflict(m);
					m.addConflict(found);
				}
				found = m;				
			} 
		}
		if(found == null) { 
			molecules.add(new Molecule(x, y));
		}
		
	}
	
    public void run() {
    	try {
    		parseImage();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private void preprocess() {
		if(imp.getBitDepth() != 8) {
			new ImageConverter(imp).convertToGray8();
		}
		ImagePlus blur = imp.duplicate();
		blur.getProcessor().blurGaussian(10);
		imp.getProcessor().copyBits(blur.getProcessor(), 0, 0, Blitter.SUBTRACT);		
		
		float kernel[] = {-1, 1, 2, 1, -1, -1, 1, 2, 1, -1,-1, 1, 2, 1, -1,-1, 1, 2, 1, -1,-1, 1, 2, 1, -1,-1, 1, 2, 1, -1};
		imp.getProcessor().convolve(kernel, 5, 5);

    }
    
	private void parseImage() throws IOException {
    	imp = source.duplicate();
    	imp.show();

		
		Dataset image = currentData;
		System.out.println("color table count: " + image.getTypeLabelShort());
		System.out.println("Image bit depth: " + imp.getBitDepth());
		System.out.println("Image path: "+imp.getFileInfo().fileName);

		preprocess();
		
		//RandomAccess access = image.randomAccess();
		long width = image.max(0);
		long height = image.max(1);
		System.out.println(width+"x"+height);
		for(long row = 0; row<=height; row++) {
			//long mean = 0;
			List<Long> values = new ArrayList<Long>();
			
			//access.setPosition((int)row, 1);
			for(long col =0; col <= width; col++ ) {
				values.add((long)imp.getPixel((int)col, (int)row)[0]);
			}
			values.sort(new Comparator<Long>() {
				@Override
				public int compare(Long o1, Long o2) {
					return o1.compareTo(o2);
				}
			});
			//Not used
			/*
			long mean = 0L;
			int numValues = (int)(values.size() * 0.9);
			for(int i = 0; i < numValues; i++) {
				mean += values.get(i);
			}
			mean = mean / numValues;
			System.out.println(row + ": "+mean);
			*/
			for(long col =0; col <= width; col++ ) {
				long intensity = (long)imp.getPixel((int)col, (int)row)[0];
				//if(intensity < mean*1.5) {
				if(intensity < 40 /* How to choose this value? */) {
					//ignore pixel
				} else {
					addPoint((int)col, (int)row);
				}
			}
		}
		
		final int[] dimensions = new int[] { (int)width+1, (int)height+1, 3};
		final Img< UnsignedByteType > out = new ArrayImgFactory< UnsignedByteType >(new UnsignedByteType()).create( dimensions);
		final RandomAccess< UnsignedByteType > r = out.randomAccess();

		int numMols = 0;
		final List<Overlay> overlays = new ArrayList<Overlay>();
		
		ChannelCollection channels = null;		
		OptionsChannels opts = optionsService.getOptions(OptionsChannels.class);
        channels = opts.getFgValues();

        RoiManager roiManager = new RoiManager();

        ResultsTable table = new ResultsTable();
        
		for(Molecule m: molecules) {
			
			if(m.points.size() > 10 && m.isBiggest()) {
				numMols++;
				System.out.println("New molecule");
	            table.incrementCounter();
	            table.setLabel(""+numMols, numMols-1);

				List<DefaultWritablePointMask> pl = new ArrayList<DefaultWritablePointMask>();
				List<double[]> pl2 = new ArrayList<>();
				
				FloatPolygon polygon = new FloatPolygon();
				for(Point p: m.getLines()) {
					polygon.addPoint(p.x,  p.y);
					r.setPosition(p.x, 0);
					r.setPosition(p.y, 1);
					System.out.println("set point at " + p.x + "x" + p.y);
					r.setPosition(numMols%3, 2);
					((UnsignedByteType)r.get()).set(255);
				}
				table.addValue("duration", polygon.getBounds().getHeight());//m.points.get(m.points.size()-1).y - m.points.get(0).y);
				table.addValue("Length", polygon.getLength(true));
				PolygonRoi roi = new PolygonRoi(polygon, Roi.POLYLINE);
				roi.setName("Molecule "+polygon.getBounds().x + "x"+polygon.getBounds().y);
				roiManager.addRoi(roi);
			}
		}
		System.out.println("Found " + numMols + " molecules");

		roiManager.runCommand("Show All");
		roiManager.deselect();
		table.show("Durations");
	}
	
}
