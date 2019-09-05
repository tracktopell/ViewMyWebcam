package com.tracktopell.gadget.viewmywebcam;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		try {
			source = ImageIO.read(new FileInputStream(inputFile));
			if(DEBUG){
				System.out.println("-> detectColor:"+source.getWidth()+"x"+source.getHeight()+
						": theTargetColor="+Integer.toHexString(theTargetColor.getRGB())+", outputFile="+outputFile);
			}
			BufferedImage imageWithDetection = detectColor(source, theTargetColor);
			
			if(DEBUG){
				System.out.println("-> imageWithDetection:"+imageWithDetection.getWidth()+"x"+imageWithDetection.getHeight());
			}
			
			if(outputFile.toLowerCase().endsWith("png")){
				fileFormat = "png";
			} else if(outputFile.toLowerCase().endsWith("jpg") || outputFile.toLowerCase().endsWith("jpeg")){
				fileFormat = "jpg";
			}
			ImageIO.write(imageWithDetection, fileFormat, new FileOutputStream(outputFile));
			ImageIO.write(imageWithDetection, "png"     , new FileOutputStream(outputFile+"_"+sdf.format(new Date())+".png"));
		} catch (IOException ioe) {
			System.err.println("Can't read:" + inputFile);
			System.exit(3);
		}

	}
	
	public static void main2(String[] args) {
		BufferedImage img1  = null;
		BufferedImage img2  = null;
		BufferedImage imgM  = null;
		final String f1 = args[0];
		final String f2 = args[1];
		final String f3 = args[2];
		String fileFormat=null;
		try {
			
			img1 = ImageIO.read(new FileInputStream(f1));
			
			img2 = ImageIO.read(new FileInputStream(f2));
			
			imgM = compareImages(img1, img2);
			if(imgM != null){
				if(f1.toLowerCase().endsWith("png")){
					fileFormat = "png";
				} else if(f1.toLowerCase().endsWith("jpg") || f1.toLowerCase().endsWith("jpeg")){
					fileFormat = "jpg";
				}

				ImageIO.write(imgM, fileFormat, new FileOutputStream(f3));
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

	}
		
	private static final boolean DEBUG = true;
	
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
		
		
		int  d_H = 20;
		int tc_H = (int) Math.floor(t_hsv[0] * MAX_H);
		int tc_S = (int) Math.floor(t_hsv[1] * MAX_S);
		int tc_B = (int) Math.floor(t_hsv[2] * MAX_B);
		
		int li_H = 0;
		int ls_H = 0;
		int   mh = -1;
		String  cc="*-|-";
		if(       ( tc_H - d_H) <= 0){
			li_H =  360 -( tc_H - d_H);
			ls_H =       ( tc_H + d_H);
			cc="*|-|-";
			mh = -1;
		} else if(( tc_H + d_H) > 360){
			li_H =       ( tc_H - d_H);
			ls_H = -360 +( tc_H + d_H);
			cc="-|-|*";
			mh = 1;
		} else {
			li_H =       ( tc_H - d_H);
			ls_H =       ( tc_H + d_H);
			cc="-|*|-";
			mh = 0;
		}
		
		ArrayList<Point2D> ptsArrayList=new ArrayList<Point2D>();
		Point2D[] ptsArray;
		String strDebug = null;
		
		if(DEBUG){
			System.out.println("Target Color: H="+tc_H+", S="+tc_S+", B="+tc_B+", d_H="+d_H+", [ "+li_H+" , "+ls_H+" ] cc="+cc+", mh="+mh);
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
					strDebug = "\tColor@("+scX+","+scY+")["+scaledW+","+scaledH+"] [" + cH + "," + cS + "," + cB + "] ~ [ "+li_H+" , "+ls_H+" ] cc="+cc+", mh="+mh;
				}
				
				if(	(	(mh==-1&&( (cH >= li_H) || (cH <= ls_H) )) || 
						(mh== 0&&( (cH <= li_H) && (cH >= ls_H) )) || 
						(mh== 1&&( (cH >= li_H) || (cH <= ls_H) ))   ) &&
						(cS <= tc_S+30 )&&(cS >= tc_S-30 )&&
						(cB <= tc_B+45 )&&(cB >= tc_B-45 )){
					
					ptsArrayList.add(new Point2D(x, y));
					g2dWorking.drawOval(x,y,dx,dy);		
					hasDetected = true;
					if(DEBUG){
						strDebug += "\t [ X ] ";
					}
				}
				
				if(DEBUG){
					System.out.println(strDebug);
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
		if(! hasDetected){
			workingImage=source;
		}
		
		return workingImage;
	}
	
	public static BufferedImage compareImages(BufferedImage image1, BufferedImage image2){
		
		if(		image1.getWidth()	!=	image2.getWidth() || 
				image2.getHeight()	!=	image2.getHeight()    ){
			throw new IllegalStateException("Diferent size's images");
		}
		
		BufferedImage scalled1    = null;
		BufferedImage scalled2    = null;
		BufferedImage resultImage = null;
		
		int scale  = 10; // 10%
		int scaledW =0;
		int scaledH =0;
		
		boolean hasDetected = false;
		
			
		scaledW = (image1.getWidth()  * scale)/100;
		scaledH = (image1.getHeight() * scale)/100;

		scalled1      = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
		scalled2      = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);	

		scalled1.getGraphics().drawImage(image1.getScaledInstance(scaledW, scaledH, Image.SCALE_FAST),0,0,null);
		scalled2.getGraphics().drawImage(image2.getScaledInstance(scaledW, scaledH, Image.SCALE_FAST),0,0,null);
		
		resultImage   = new BufferedImage(image1.getWidth(), image1.getHeight(), BufferedImage.TYPE_INT_ARGB);			


		final Graphics2D g2dResult   = resultImage.createGraphics();
		
		float scaleTranform = 0.1f;
		
		AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleTranform, scaleTranform);		
		
		//--------------------------------------------------
		g2dResult .drawImage(image2, 0, 0, null);		
		
		g2dResult .drawImage(scalled1, 0, 0, null);	
		
		g2dResult .setColor(Color.CYAN);
		g2dResult .drawRect (0, 0, scaledW-1,scaledH-1);					
		
		g2dResult .drawImage(scalled2, scaledW, 0, null);
		g2dResult .setColor(Color.GREEN);
		g2dResult .drawRect (scaledW						, 0, 
							 scaledW-1	,scaledH-1);			
		
		
		ArrayList<Point2D> ptsArrayList=new ArrayList<Point2D>();
		Point2D[] ptsArray;
		
		int dx=10;
		int dy=10;
		
		for (int y = 0; y < scaledH; y += 1) {
			for (int x = 0; x < scaledW; x += 1) {
				
				int c1 = scalled1.getRGB(x, y);
				int c2 = scalled2.getRGB(x, y);

				if( ! looksLikeColor( c1, c2) ){
				//if(  c1 != c2 ){	
					ptsArrayList.add(new Point2D(x *dx,y *dy));
					//g2dResult.setColor(Color.RED);
					//g2dResult.drawOval(	x *dx,y *dy,
					//					dx   ,dy     );		
					hasDetected = true;
				}
			}			
		}
		
		if(hasDetected && ptsArrayList.size()>0){
			
			ptsArray = new Point2D[ptsArrayList.size()];
			
			ptsArrayList.toArray(ptsArray);
			GrahamScan gs = new GrahamScan(ptsArray);
			final Iterable<Point2D> hull = gs.hull();

			Point2D p2dBefore=null;
			Point2D p2dFirst =null;
			Point2D p2dLast=null;
			for(Point2D p2d: hull){
				g2dResult.setColor(Color.BLUE);
				g2dResult.drawOval((int)p2d.x(),(int)p2d.y(),dx,dy);

				if(p2dBefore!=null){
					g2dResult.setColor(Color.YELLOW);
					g2dResult.drawLine((int)p2dBefore.x()	+dx/2	,(int)p2dBefore.y()	+dy/2,
										(int)p2d.x()		+dx/2	,(int)p2d.y()		+dy/2);
				} else {
					p2dFirst = p2d;
				}
				p2dBefore = p2d;
				p2dLast   = p2d;
			}

			if(p2dFirst != p2dLast) {
				g2dResult.setColor(Color.YELLOW);
				g2dResult.drawLine((int)p2dFirst.x()	+dx/2	,(int)p2dFirst.y()	+dy/2,
									(int)p2dLast.x()	+dx/2	,(int)p2dLast.y()	+dy/2);
			}
		}
		
		
		if(hasDetected){
			return resultImage;
		}else{
			return null;
		}
	}
	
	private static boolean looksLikeColor(int c_rgb1,int c_rgb2){
		boolean e=false;
		Color c1=new Color(c_rgb1);
		Color c2=new Color(c_rgb2);
		
		float[] hsb_c1 = Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), null);
		float[] hsb_c2 = Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), null);
		
		if(		Math.abs(hsb_c1[0]-hsb_c2[0]) <= 0.8 && 
				Math.abs(hsb_c1[1]-hsb_c2[1]) <= 0.8 &&
				Math.abs(hsb_c1[2]-hsb_c2[2]) <= 0.8) {
			e = false;
		}
		
		return e;
	}
	
	
	private static boolean _looksLikeColor(int c_rgb1,int c_rgb2){
		boolean e=true;
		
		byte rgb1[]=new byte[3];
		byte rgb2[]=new byte[3];
		
		
		rgb1[0] = (byte)((c_rgb1>>16 )&0xFF);
		rgb1[1] = (byte)((c_rgb1>>8  )&0xFF);
		rgb1[2] = (byte)((c_rgb1     )&0xFF);

		rgb2[0] = (byte)((c_rgb2>>16 )&0xFF);
		rgb2[1] = (byte)((c_rgb2>>8  )&0xFF);
		rgb2[2] = (byte)((c_rgb2     )&0xFF);
		
		int d  = 0;
		int dd = 0;
		for(int i=0;i<3;i++){
			d  = (rgb2[i]-rgb1[i]);
			dd = d*d;
			e  = e && (dd <= 625);
		}
		
		return e;
	}

	private static void usage() {
		System.err.println("Usage: " + SearchColorInImage.class.getName() + " -infputFile=<infputFile>   -outputFile=<outputFile>  -targetColor=#ff0000");
	}

}
