package com.tracktopell.gadget.viewmywebcam;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.imageio.ImageIO;

/**
 *
 * @author Tracktopell
 */
public class WebcamTargetPanel extends javax.swing.JPanel {

    BufferedImage webcamPreviousImage;
    BufferedImage webcamImage;
    BufferedImage webcamLastPicture;
    BufferedImage webcamLastFastPicture;
    BufferedImage personIconImage;
    BufferedImage noCameraIconImage;
	
	SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS");
	SimpleDateFormat sdf     = new SimpleDateFormat("yyyyMMdd_HHmmssSSSS");
	Date date=null;
	
    boolean paintPersonIcon = false;
	boolean paintTargetIcon = false;
	
    public void setWebcamImage(BufferedImage webcamImage) {
        this.webcamPreviousImage = this.webcamImage;
        this.webcamImage = webcamImage;
		date = new Date();
        repaint();
    }

    public void setPaintPersonIcon(boolean paintPersonIcon) {
        this.paintPersonIcon = paintPersonIcon;
    }

	public boolean isPaintPersonIcon() {
		return paintPersonIcon;
	}
		
	public void setPaintTargetIcon(boolean paintTargetIcon) {
		this.paintTargetIcon = paintTargetIcon;
	}

	public boolean isPaintTargetIcon() {
		return paintTargetIcon;
	}
	
    /**
     * Creates new form WebcamPhotoPreviewPanel
     */
    public WebcamTargetPanel() {
        initComponents();
		date = new Date();
        try {
            personIconImage   = ImageIO.read(WebcamTargetPanel.class.getResourceAsStream("/images/person-icon.png"));            
        } catch (Exception e) {
			e.printStackTrace(System.err);
        }
		try {
            noCameraIconImage = ImageIO.read(WebcamTargetPanel.class.getResourceAsStream("/images/noCamera-icon.png"));
        } catch (Exception e) {
			e.printStackTrace(System.err);
        }
    }

    public void takeSnapshot() {
        webcamLastPicture = webcamImage;
        new Thread() {
            @Override
            public void run() {
                saveWebcamLastPicture();
            }
        }.start();

    }

    public void takeFastSnapshot() {
        webcamLastFastPicture = webcamImage;
        new Thread() {
            @Override
            public void run() {
                saveWebcamFastLastPicture();
            }
        }.start();
    }

	private void lastEdition(BufferedImage img){
		Graphics2D g2d = (Graphics2D) img.getGraphics();
		g2d.setColor(Color.GREEN);
		g2d.drawString(sdfTime.format(date), 5, img.getHeight()- 15);
	}

    private void saveWebcamLastPicture() {
        String fileName = "./F1_CameraSnapshot_" + sdf.format(new Date()) + ".png";
        try {
			lastEdition(webcamLastPicture);
            ImageIO.write(webcamLastPicture, "png", new FileOutputStream(fileName));
            System.out.println("..ok saved to " + fileName);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }
	
    private void saveWebcamFastLastPicture() {
        String fileName = "./F2_CameraFastSnapshot_" + sdf.format(new Date()) + ".png";
        try {
			lastEdition(webcamLastFastPicture);
            ImageIO.write(webcamLastFastPicture, "png", new FileOutputStream(fileName));
            System.out.println("..ok saved to " + fileName);
            webcamLastFastPicture = null;
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void cameraMode() {
        webcamLastPicture = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //System.err.println("->paintComponent:");
    }
    static double mv = Math.PI;

    static boolean rotated = false;

    public void setRotated(boolean r) {
        rotated = r;
        repaint();
    }

    public static boolean isRotated() {
        return rotated;
    }

    private BufferedImage getRotatedImage(BufferedImage img) {
        BufferedImage rotatedImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = (Graphics2D) rotatedImage.getGraphics();
        if (rotated) {
            g2d.rotate(mv);
            g2d.drawImage(img, -img.getWidth(), -img.getHeight(), null);
        } else {
            g2d.drawImage(img, 0, 0, null);
        }

        return rotatedImage;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        //System.err.println("->paint:");
        BufferedImage imageToPaint = null;

        if (webcamImage != null) {

            if (webcamLastPicture != null) {
                imageToPaint = webcamLastPicture;
            } else {
                imageToPaint = webcamImage;
            }

            int imgW = imageToPaint.getWidth();
            int imgH = imageToPaint.getHeight();
            double rImg = (double) getWidth() / (double) imgW;

            int imgY = 0;

            Graphics2D g2d = (Graphics2D) g;
			
            AffineTransform atB = g2d.getTransform();
            AffineTransform at = new AffineTransform();

            at.scale(rImg, rImg);

            g2d.setTransform(at);

            imgY = (int) (((getHeight() / 2.0) / rImg) - (imgH / 2.0));

            BufferedImage rotatedImage = getRotatedImage(imageToPaint);
            g2d.drawImage(rotatedImage, 0, imgY, null);
						
            g2d.setTransform(atB);
			
			g2d.setColor(Color.GREEN);
			g2d.drawString(sdfTime.format(date), 5, getHeight()- 15);

            if (webcamLastPicture == null) {

                if (paintPersonIcon && personIconImage != null) {
                    AffineTransform atp = new AffineTransform();
                    Composite acB = g2d.getComposite();

                    int imgWP = personIconImage.getWidth();
                    int imgHP = personIconImage.getHeight();
                    double rImgP = (double) getWidth() / ((double) imgWP);
                    atp.scale(rImgP, rImgP);
                    AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

                    g2d.setTransform(atp);
                    g2d.setComposite(ac);

                    int imgYP = (int) (((getHeight() / 2.0) / rImgP) - (imgHP / 2.0));

                    g2d.drawImage(personIconImage,
                            0,
                            imgYP,
                            null);

                    g2d.setTransform(atB);
                    g2d.setComposite(acB);
                }
                //==================================================================
				if(paintTargetIcon){
					g.setColor(Color.RED);

					int imgHS = (int) (imgH * rImg);

					g.drawRect(0, (getHeight() - imgHS )/ 2,
							getWidth()-1, imgHS  );
					
					//VERTICAL
					g.drawLine(getWidth() / 2, getHeight() / 2 - imgHS / 2,
							getWidth() / 2, getHeight() / 2 + imgHS / 2);
					// HORIZONTAL
					g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);

					int r = getWidth() / 6;
					int r2 = getWidth() / 2;

					g.drawOval(getWidth() / 2 - r / 2, getHeight() / 2 - r / 2,
							r, r);
					g.drawOval(getWidth() / 2 - r2 / 2, getHeight() / 2 - r2 / 2,
							r2, r2);
				}
            } else {
                g.setColor(Color.GREEN);

                int imgHS = (int) (imgH * rImg);
                int imgWS = (int) (imgW * rImg);

                g.drawRect(0, getHeight() / 2 - imgHS / 2,
                        imgWS - 1, imgHS - 1);

            }
        } else if (noCameraIconImage != null) {
            //g.setColor(getBackground());
            //g.clearRect(0, 0, getWidth(), getHeight());

            imageToPaint = noCameraIconImage;

            int imgW = imageToPaint.getWidth();
            int imgH = imageToPaint.getHeight();
            double rImg = (double) getWidth() / (double) imgW;

            int imgY = 0;

            Graphics2D g2d = (Graphics2D) g;
            AffineTransform atB = g2d.getTransform();

            AffineTransform at = new AffineTransform();

            at.scale(rImg, rImg);

            g2d.setTransform(at);

            imgY = (int) (((getHeight() / 2.0) / rImg) - (imgH / 2.0));

            g2d.drawImage(imageToPaint,
                    0,
                    imgY,
                    null);
            g2d.setTransform(atB);

        } else {
            g.setColor(getBackground());
            g.clearRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.RED);
            g.drawString("WEB CAM", 10, getHeight() / 2);
        }
    }

    @Override
    public void paintAll(Graphics g) {
        super.paintAll(g);
        //System.err.println("->paintAll:");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setPreferredSize(new java.awt.Dimension(240, 320));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 240, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 320, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
