package com.tracktopell.gadget.viewmywebcam;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamMotionEvent;
import com.github.sarxos.webcam.WebcamMotionListener;
import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;

/**
 * com.tracktopell.gadget.viewmywebcam.Main
 *
 * @author tracktopell
 */
public class Main extends javax.swing.JFrame implements WebcamMotionListener {

	/**
	 * Creates new form Main
	 */
	private static WebcamTargetPanel wcPanel;
	private SimpleDateFormat sdfVideo = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private SimpleDateFormat sdfRecordingVideo = new SimpleDateFormat("mm:ss.S");
	private SimpleDateFormat sdfMonitoring = new SimpleDateFormat("ss.SSS");
	private DecimalFormat dfFrames = new DecimalFormat("00000");
	private BufferedImage lastImage;
	private BufferedImage prevImage;
	private BufferedImage movingImage;
	private boolean hideMode = true;
	private long videoStartTime = 0;
	private boolean recording = false;
	private Webcam webcam;
	private Webcam webcamNowSelected;
	private List<Webcam> webcamsConnectedList;

	public Main() {
		initComponents();
		wcPanel = (WebcamTargetPanel) wcp;
		wcPanel.setPaintTargetIcon(true);
		
		hideMode = false;
		updateHideShowInfo();

		hideModeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hideMode = !hideMode;
				updateHideShowInfo();
			}
		});

		superfotoBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						teakePicture();
					}
				}.start();
			}
		});

		videoStartStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!recording) {
					videoStart();
				} else {
					videoStop();
				}
			}
		});

		webcamsConnectedList = Webcam.getWebcams();
		System.out.println("Webcam.getWebcams:");
		int nwc = 0;
		String[] webcamsConnectedNames = new String[webcamsConnectedList.size()];
		for (Webcam wc : webcamsConnectedList) {
			System.out.println("=>[" + nwc + "]:" + wc);
			webcamsConnectedNames[nwc] = wc.getName();
			nwc++;
		}

		cameraComboBox.setModel(new DefaultComboBoxModel<String>(webcamsConnectedNames));

		cameraComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) {
					String cameraName = (String)event.getItem();

					webcamNowSelected = null;
					for (Webcam wc : webcamsConnectedList) {
						if(wc.getName().equalsIgnoreCase(cameraName)){
							webcamNowSelected = wc;
							break;
						}
					}
					if(webcamNowSelected != null) {
						
						selectAnotherCamera();
					}
				}
			}
		});

		webcam = Webcam.getDefault();
		webcamNowSelected = null;
		
		Webcam.addDiscoveryListener(new WebcamDiscoveryListener() {
			public void webcamFound(WebcamDiscoveryEvent wde) {
				Webcam webcamF = wde.getWebcam();
				System.out.println("-> Webcam found:" + webcamF);
			}

			public void webcamGone(WebcamDiscoveryEvent wde) {
				Webcam webcamG = wde.getWebcam();
				System.out.println("-> Webcam gone:" + webcamG);
			}
		});

		/*
        WebcamMotionDetector detector = new WebcamMotionDetector(Webcam.getDefault());
        detector.setInterval(intervarMotionDetection);
        detector.addMotionListener(this);
        detector.start();
        new Thread(){
            @Override
            public void run() {
                passivateMotionIndicator();
            }            
        }.start();
		 */
	}

	private void selectAnotherCamera() {
		new Thread(){
			@Override
			public void run() {
				webcam.close();
				webcam = null;
				webcam = webcamNowSelected;
				webcam.open();
				webcamNowSelected = null;
			}
		}.start();
	}

	private int intervarMotionDetection = 500;
	private int lastMotionStrength = 0;
	private long lastMotionTime = 0;
	private boolean runningPaivateMS = false;

	private void passivateMotionIndicator() {
		runningPaivateMS = true;
		long now = System.currentTimeMillis();
		long intervalWaitingMotion = intervarMotionDetection / 2;
		try {
			int monitoringStrength = 0;
			final int maxTimeToWait = intervarMotionDetection * 5;
			long dt = 0;
			while (runningPaivateMS) {
				now = System.currentTimeMillis();
				dt = (now - lastMotionTime);

				if (lastMotionStrength > 0 && dt >= maxTimeToWait) {
					lastMotionStrength = 0;
					System.out.println("=>passivateMotionIndicator: PASIVATE ! DT=" + sdfMonitoring.format(new Date(dt)));
				}

				Thread.sleep(intervalWaitingMotion);
				if (monitoringStrength != lastMotionStrength) {
					int level = (int) ((lastMotionStrength / 10000.0) * 100);
					System.out.println("=>passivateMotionIndicator: lastMotionStrength=" + lastMotionStrength + ", level=" + level);

					movementStrength.setValue(level);
				}
				monitoringStrength = lastMotionStrength;
			}
		} catch (InterruptedException ie) {

		}
	}

	private void updateHideShowInfo() {
		if (hideMode) {
			hideModeBtn.setText("S");
			hideModeBtn.setToolTipText("Show Panel Camera");
		} else {
			hideModeBtn.setText("H");
			hideModeBtn.setToolTipText("Hide Panel Camera");
		}
	}

	@Override
	public void motionDetected(WebcamMotionEvent wme) {
		System.out.println("Detected motion at:" + sdfVideo.format(new Date())
				+ "=>WebcamMotionEvent: strength:" + wme.getStrength());
		lastMotionStrength = wme.getStrength();
		lastMotionTime = System.currentTimeMillis();
	}

	private void lauchCaptureImages() {
		new Thread() {
			@Override
			public void run() {
				captureImages();
			}
		}.start();

	}

	private void videoStart() {
		new Thread() {
			@Override
			public void run() {
				videoRecording();
			}
		}.start();
	}

	private void teakePicture() {
		if (lastImage != null) {
			try {
				ImageIO.write(lastImage, "JPG", new File("CameraFastSnapshot_" + sdfVideo.format(new Date()) + ".jpg"));
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}

	private void videoRecording() {

//		File file = new File("Recording_"+sdfVideo.format(new Date())+".ts");
//		IMediaWriter writer = ToolFactory.makeWriter(file.getName());
//		Dimension size = WebcamResolution.QVGA.getSize();
//		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);

		try {
			if (webcam != null) {
				System.out.println("-> Webcam open: Start video Recording");
				long currentTimeVideo = 0;
				long diffTime = 0;
				int frame = 0;
				recording = true;
				videoStartStop.setText("[_]");
				videoStartTime = System.currentTimeMillis();
				Date dateStart = new Date(videoStartTime);
				while (true) {

					if (lastImage != null) {
						File dirRecodings = new File("./recordings/");
						if (!dirRecodings.exists()) {
							dirRecodings.mkdirs();
						}
						File fileRecording = new File(dirRecodings, "Recording_" + sdfVideo.format(dateStart) + "_F" + dfFrames.format(frame) + ".jpg");
						ImageIO.write(lastImage, "JPG", fileRecording);
					} else {

					}
					currentTimeVideo = System.currentTimeMillis();
					diffTime = currentTimeVideo - videoStartTime;
					String recordingText = sdfRecordingVideo.format(new Date(diffTime));
					//System.out.println("-> Recording:"+recordingText);
					videoTimeRecording.setText(recordingText);
					Thread.sleep(200);

					if (!recording) {
						break;
					}

					frame++;
				}
				System.out.println("-> Webcam open: Finished video Recording");

			} else {
				System.out.println("-> No webcam :( ");
				wcPanel.repaint();
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}

	private void videoStop() {
		System.out.println("-> STOP Recording ");
		videoStartStop.setText("O");
		recording = false;
		videoTimeRecording.setText("");
	}

	private Color theTargetColor = Color.RED;
	private boolean detectColor = false;

	public void setDetectColor(boolean detectColor) {
		this.detectColor = detectColor;
        System.out.println("-> setDetectColor:"+this.detectColor);
	}

	public boolean isDetectColor() {
		return detectColor;
	}

	private void captureImages() {
		try {

			//Webcam webcam = Webcam.getDefault();			
			if (webcam != null) {
				System.out.println("-> Webcam name:" + webcam.getName());
				System.out.println("-> Webcam class:" + webcam.getClass());

				webcam.open();
				System.out.println("-> Webcam open");
				while (true) {
					if (webcam != null) {
						lastImage = webcam.getImage();

						if (lastImage != null) {
							if (detectColor) {
                                lastImage = SearchColorInImage.detectColor(lastImage, theTargetColor);
								BufferedImage imgM = null;
								if (prevImage != null) {
									//----------------------- strategy 1
	//								movingImage = SearchColorInImage.compareImages(prevImage, lastImage);
	//								if(movingImage != null){
	//									imgM = movingImage;
	//								} else {
	//									imgM = lastImage;
	//								}
									//----------------------- strategy 2

									ImageCompare ic = new ImageCompare(prevImage, lastImage);
									ic.setParameters(20, 8, 1, 10);
									ic.setDebugMode(0);
									ic.compare();
									if (!ic.match()) {
										imgM = ic.getChangeIndicator();
									} else {
										imgM = lastImage;
									}
								}
								if (!hideMode) {
									wcPanel.setWebcamImage(imgM);
								} else {
									wcPanel.setWebcamImage(null);
								}
								prevImage = lastImage;
							} else if (!hideMode) {
								wcPanel.setWebcamImage(lastImage);
							} else {
								wcPanel.setWebcamImage(null);
							}
						} else {
						}
					} else {
						System.out.println("...Waiting webcam ! ");
					}
					Thread.sleep(100);
				}
			} else {
				System.out.println("-> No webcam :( ");
				wcPanel.repaint();
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	private void captureVideo() {
		try {

			Webcam webcam = Webcam.getDefault();

			if (webcam != null) {
				System.out.println("-> Webcam name:" + webcam.getName());
				System.out.println("-> Webcam class:" + webcam.getClass());
				webcam.open();
				System.out.println("-> Webcam open");

			} else {
				System.out.println("-> No webcam :( ");
				wcPanel.repaint();
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        wcp = new WebcamTargetPanel();
        toolBarControls = new javax.swing.JToolBar();
        jPanel2 = new javax.swing.JPanel();
        detectColorFigure = new javax.swing.JCheckBox();
        detectCol = new javax.swing.JCheckBox();
        hideModeBtn = new javax.swing.JButton();
        superfotoBtn = new javax.swing.JButton();
        videoStartStop = new javax.swing.JButton();
        videoTimeRecording = new javax.swing.JTextField();
        movementStrength = new javax.swing.JProgressBar();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        cameraComboBox = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Tracktopell : ViewMyWebcam");

        javax.swing.GroupLayout wcpLayout = new javax.swing.GroupLayout(wcp);
        wcp.setLayout(wcpLayout);
        wcpLayout.setHorizontalGroup(
            wcpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 469, Short.MAX_VALUE)
        );
        wcpLayout.setVerticalGroup(
            wcpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 290, Short.MAX_VALUE)
        );

        getContentPane().add(wcp, java.awt.BorderLayout.CENTER);

        toolBarControls.setRollover(true);

        detectColorFigure.setText("M");
        detectColorFigure.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                detectColorFigureStateChanged(evt);
            }
        });
        jPanel2.add(detectColorFigure);

        detectCol.setText("C");
        jPanel2.add(detectCol);

        hideModeBtn.setFont(new java.awt.Font("Monospaced", 1, 12)); // NOI18N
        hideModeBtn.setText("H");
        jPanel2.add(hideModeBtn);

        superfotoBtn.setFont(new java.awt.Font("Monospaced", 1, 12)); // NOI18N
        superfotoBtn.setText("P");
        jPanel2.add(superfotoBtn);

        videoStartStop.setFont(new java.awt.Font("Monospaced", 1, 12)); // NOI18N
        videoStartStop.setForeground(new java.awt.Color(255, 0, 0));
        videoStartStop.setText("O");
        jPanel2.add(videoStartStop);

        videoTimeRecording.setBackground(new java.awt.Color(0, 0, 0));
        videoTimeRecording.setColumns(7);
        videoTimeRecording.setFont(new java.awt.Font("Monospaced", 1, 12)); // NOI18N
        videoTimeRecording.setForeground(new java.awt.Color(0, 255, 0));
        videoTimeRecording.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        jPanel2.add(videoTimeRecording);

        movementStrength.setForeground(new java.awt.Color(255, 0, 0));
        movementStrength.setPreferredSize(new java.awt.Dimension(90, 10));
        jPanel2.add(movementStrength);

        toolBarControls.add(jPanel2);

        getContentPane().add(toolBarControls, java.awt.BorderLayout.SOUTH);

        jLabel1.setText("Camera :");
        jPanel1.add(jLabel1);
        jPanel1.add(cameraComboBox);

        getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_START);

        setSize(new java.awt.Dimension(477, 400));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void detectColorFigureStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_detectColorFigureStateChanged
		if (detectColorFigure.isSelected()) {
			detectColor = true;
		} else {
			detectColor = false;
		}
    }//GEN-LAST:event_detectColorFigureStateChanged
	private static Main captureHdn = null;

	public static void main(String[] args) {
		captureHdn = new Main();
		captureHdn.setVisible(true);
		captureHdn.lauchCaptureImages();
	}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cameraComboBox;
    private javax.swing.JCheckBox detectCol;
    private javax.swing.JCheckBox detectColorFigure;
    private javax.swing.JButton hideModeBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JProgressBar movementStrength;
    private javax.swing.JButton superfotoBtn;
    private javax.swing.JToolBar toolBarControls;
    private javax.swing.JButton videoStartStop;
    private javax.swing.JTextField videoTimeRecording;
    private javax.swing.JPanel wcp;
    // End of variables declaration//GEN-END:variables
}
