package com.tracktopell.gadget.viewmywebcam;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 * com.tracktopell.gadget.viewmywebcam.SearchColorInImage
 * @author tracktopell
 */
public class SearchColorInImage {
	
	public static void main(String[] args) {
		BufferedImage source  = null;
		BufferedImage scalled = null;
		BufferedImage workingImage = null;

		String inputFile = null;
		String outputFile = null;
		String targetColor = null;

		if (args.length == 0) {
			System.err.println("Error in arguments.");
			usage();
			System.exit(1);
		} else {
			for (String arg : args) {
				String[] argValue = arg.split("=");
				if (argValue.length == 2 && argValue[0].equals("-infputFile")) {
					inputFile = argValue[1];
				} else if (argValue.length == 2 && argValue[0].equals("-outputFile")) {
					outputFile = argValue[1];
				} else if (argValue.length == 2 && argValue[0].equals("-targetColor")) {
					targetColor = argValue[1].toUpperCase().replace("#","");
				} else {
					System.err.println("Illegal argument:" + arg);
					usage();
					System.exit(2);
				}
			}
		}
		
		int targetColorRGB = -1;
		
		if(targetColor != null){
			try {
				targetColorRGB = Integer.parseInt(targetColor, 16);
			}catch(NumberFormatException nfe){
				System.err.println("The param targetColor="+targetColor+", is not Valid HEX Color.");
				System.exit(1);
			}
		} else {
			targetColorRGB = 0xFF0000;
		}
		
		Color theTargetColor = new Color(targetColorRGB);
		String fileFormat =  null;
		try {
			source = ImageIO.read(new FileInputStream(inputFile));
			
			BufferedImage imageWithDetection = detectColor(source, theTargetColor);
			if(outputFile.toLowerCase().endsWith("png")){
				fileFormat = "png";
			} else if(outputFile.toLowerCase().endsWith("jpg") || outputFile.toLowerCase().endsWith("jpeg")){
				fileFormat = "jpg";
			}
			ImageIO.write(imageWithDetection, fileFormat, new FileOutputStream(outputFile));
		} catch (IOException ioe) {
			System.err.println("Can't read:" + inputFile);
			System.exit(3);
		}

	}
	
	private static final boolean DEBUG = false;
	
	public static BufferedImage detectColor(BufferedImage source, Color theTargetColor){
		BufferedImage workingImage = null;
		BufferedImage scalled = null;
		
		int scale = 10; // 10%
		int scaledW=0;
		int scaledH=0;
		boolean hasDetected = false;
		try {
			
			scaledW = (source.getWidth()  * scale)/100;
			scaledH = (source.getHeight() * scale)/100;
						
			scalled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
			
			workingImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

		} catch (Exception ioe) {
			System.exit(3);
		}

		final Graphics2D g2dSource  = source.createGraphics();
		final Graphics2D g2dScaled  = scalled.createGraphics();
		final Graphics2D g2dWorking = workingImage.createGraphics();
		float scaleTranform = (float)10/100.0f;
		
		AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleTranform, scaleTranform);
		g2dScaled.transform(scaleTransform);
		g2dScaled.drawImage(source, 0, 0, null);
		
		g2dWorking.setColor(Color.BLUE);
		g2dWorking.drawRect(0, 0, source.getWidth()-1, source.getHeight()-1);
		g2dWorking.drawImage(source, 0, 0, null);

		//find cyan pixels
		int c, r, g, b;
		float[] hsv;
		
		int dx = 5;
		int dy = 5;

		//System.out.println("-----------------------------HSB-------------------------------");
		int cH, cS, cB;

		hsv = new float[3];
		float[] t_hsv = new float[3];
		
		int t_rgb = theTargetColor.getRGB();
		int t_r = (t_rgb>>16)&0xFF;
		int t_g = (t_rgb>>8 )&0xFF;
		int t_b = (t_rgb    )&0xFF;

		Color.RGBtoHSB(t_r,t_g,t_b,t_hsv);
		
		final int MAX_H = 360;
		final int MAX_S = 100;
		final int MAX_B = 100;
		
		int tc_H = (int) Math.floor(t_hsv[0] * MAX_H);
		int tc_S = (int) Math.floor(t_hsv[1] * MAX_S);
		int tc_B = (int) Math.floor(t_hsv[2] * MAX_B);
		
		int min_tc_H;
		int max_tc_H;
		
		int min_tc_S;
		int max_tc_S;
		
		int min_tc_B;
		int max_tc_B;
		
		int range_H =  MAX_H / 2;
		int range_S = (MAX_S / 6) * 5;
		int range_B = (MAX_B / 6) * 5;
		
		min_tc_H = (tc_H - range_H/2) % MAX_H;
		if(min_tc_H<0){
			min_tc_H = 360 + min_tc_H;
		}		
		max_tc_H = (tc_H + range_H/2) % MAX_S;
		
		min_tc_S = (tc_S - range_S/2);
		if(min_tc_S<0){
			min_tc_S = 0;
		}
		max_tc_S = (tc_S + range_S/2);
		if(max_tc_S > MAX_S){
			max_tc_S = MAX_S;
		}
		
		min_tc_B = (tc_B - range_B/2);
		if(min_tc_B<0){
			min_tc_B = 0;
		}
		max_tc_B = (tc_B + range_B/2);
		if(max_tc_B > MAX_B){
			max_tc_B = MAX_B;
		}
		
			
		
		ArrayList<Point2D> ptsArrayList=new ArrayList<Point2D>();
		Point2D[] ptsArray;
		String strDebug = null;
		
		if(DEBUG){
			System.out.println("Target Color: H="+tc_H+" ( "+min_tc_H+","+max_tc_H+" ), S="+tc_S+" ("+min_tc_S+","+max_tc_S+"), B="+tc_B+" ("+min_tc_B+","+max_tc_B+")");
			System.out.println("===>> SCANNING:");
		}
	
		for (int y = 0; y < source.getHeight(); y += dy) {
			for (int x = 0; x < source.getWidth(); x += dx) {
				
				int scX = (x*scale)/100;
				int scY = (y*scale)/100;
				
				scX = scX>=scaledW?scaledW-1:scX;
				scY = scY>=scaledH?scaledH-1:scY;
				
				c = scalled.getRGB(scX, scY);				

				r = (c>>16)&0xFF;
				g = (c>>8)&0xFF;
				b = c&0xFF;
				
				hsv = new float[3];
				hsv = Color.RGBtoHSB(r, g, b, hsv);				
				
				cH = (int) Math.floor(hsv[0] * 360);
				cS = (int) Math.floor(hsv[1] * 100);
				cB = (int) Math.floor(hsv[2] * 100);
				if(DEBUG){
					strDebug = "\tColor@("+scX+","+scY+")["+scaledW+","+scaledH+"] [" + cH + "," + cS + "," + cB + "] ";
				}
				
				if(		(cH >= min_tc_H || cH<=max_tc_H)&& 
						(cS >= min_tc_S && cS<=max_tc_S)&&
						(cB >= min_tc_B && cB<=max_tc_B)){
					
					ptsArrayList.add(new Point2D(x, y));
					g2dWorking.drawOval(x,y,dx,dy);		
					hasDetected = true;
					if(DEBUG){
						strDebug += "\t [ X ] ";
						System.out.println(strDebug);
					}
				}
				
			}
			
		}
		
		if(ptsArrayList.size()>0){
			
			ptsArray = new Point2D[ptsArrayList.size()];
			
			ptsArrayList.toArray(ptsArray);
			GrahamScan gs = new GrahamScan(ptsArray);
			final Iterable<Point2D> hull = gs.hull();

			Point2D p2dBefore=null;
			Point2D p2dFirst =null;
			Point2D p2dLast=null;
			for(Point2D p2d: hull){
				g2dWorking.setColor(Color.GREEN);
				g2dWorking.fillOval((int)p2d.x()+dx/2,(int)p2d.y()+dy/2,dx/2,dy/2);

				if(p2dBefore!=null){
					g2dWorking.setColor(Color.YELLOW);
					g2dWorking.drawLine((int)p2dBefore.x()	+dx/2	,(int)p2dBefore.y()	+dy/2,
										(int)p2d.x()		+dx/2	,(int)p2d.y()		+dy/2);
				} else {
					p2dFirst = p2d;
				}
				p2dBefore = p2d;
				p2dLast   = p2d;
			}

			if(p2dFirst != p2dLast) {
				g2dWorking.setColor(Color.YELLOW);
				g2dWorking.drawLine((int)p2dFirst.x()	+dx/2	,(int)p2dFirst.y()	+dy/2,
									(int)p2dLast.x()	+dx/2	,(int)p2dLast.y()	+dy/2);
			}
		}
		if(hasDetected){
			return workingImage;
		} else {
			return source;
		}
	}
	

	private static void usage() {
		System.err.println("Usage: " + SearchColorInImage.class.getName() + " -infputFile=<infputFile>   -outputFile=<outputFile>  -targetColor=#ff0000");
	}

}