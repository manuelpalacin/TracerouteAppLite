package edu.upf.nets.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import edu.upf.nets.mercury.pojo.TracerouteSession;

import javax.swing.ImageIcon;

public class AppLite extends JFrame {
	

	private static final long serialVersionUID = 8566644937168241883L;
	private static final Logger log = Logger
			.getLogger(AppLite.class.getName());
	
	private ExecutorService executorService;
	private static final int POOL = 20;
	
	private JPanel contentPane;
	private JButton buttonStart;
	private JButton buttonStop;
	private JTextField tfCity;
	private JTextField tfISP;
	private JProgressBar progressBar;
	private JLabel labelStatus;
	private JLabel labelTotalDestinations;
	private String locale;
	private List<String> destinations;
	private JLabel labelLocale;
	
	private String internetAnalyticsGetUrls = "http://inetanalytics.nets.upf.edu/getUrls";
	private String mercuryUploadTrace = "http://mercury.upf.edu/mercury/api/traceroute/uploadTrace";
	private String mercuryAddSession = "http://mercury.upf.edu/mercury/api/traceroute/addTracerouteSession";
	private String freeGeoIp = "http://freegeoip.net/json/";
	private JTextField tfAuthor;

	private final CancellationToken cancel = new CancellationToken();
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new AppLite("Internet Analytics - Traceroute App");
			}
		});
	}
	
	
	//We create a Runnable ActionListener
	private class ActionListenerStop implements ActionListener, Runnable {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			labelStatus.setText("Stopping...");
			JOptionPane.showMessageDialog(null,
				    "Stopping...\nPlease be patient, it will take up to 2 minutes to stop the application.");
			labelTotalDestinations.setVisible(false);
			buttonStop.setEnabled(false);
	    	// Cancel the cancellation token.
	    	cancel.cancel();
	    	// Shutdown the threads (doesn't do anything)
	    	executorService.shutdown();
			// Waiting on a thread.
			new Thread(this).start();
		}

		@Override
		public void run() {
	    	try {
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (InterruptedException e) {
				labelStatus.setText("Application does not respond. Please close the application and try again.");
			}
	    	labelStatus.setText("Stopped.");
			buttonStart.setEnabled(true);
		}
	}
	
	
	/**
	 * Create the frame.
	 */
	public AppLite(String title) {
    	super(title);
		setResizable(false);
		setVisible(true);
		setIconImage(Toolkit.getDefaultToolkit().getImage(AppLite.class.getResource("/edu/upf/nets/app/images/GraphBarColor_64.png")));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 480);
		setLookAndFeel();
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu menu = new JMenu("Information");
		menuBar.add(menu);
		
		JMenuItem menuItemAbout = new JMenuItem("About");
    	menuItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ImageIcon ii = new ImageIcon(
						ClassLoader.getSystemResource("edu/upf/nets/app/images/GraphBarColor_64.png"));
				JOptionPane.showMessageDialog(null, "This tool is done by Manuel Palacin and Alex Bikfalvi\n" +
						"at NeTS Research Group under the Mercury Project.\n " +
					    "Please visit us at http://mercury.upf.edu", "About", JOptionPane.PLAIN_MESSAGE, ii);
				
			}
    	});
    	JMenuItem menuItemHelp = new JMenuItem("Help");
    	menuItemHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
		    	JOptionPane.showMessageDialog(null,
					    "This tool allows you to traceroute a set of customized or popular web sites.\n" +
					    "Finally all results are sent to http://mercury.upf.edu\n" +
					    "Send feedback to: manuel.palacin@upf.edu");
				
			}
    	});
    	menu.add(menuItemAbout);
    	menu.add(menuItemHelp);
		

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		final JPanel panelContent = new JPanel();
		contentPane.add(panelContent, BorderLayout.CENTER);
		panelContent.setLayout(null);
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setBounds(0, 0, 484, 191);
		panelContent.add(tabbedPane);
		
		JPanel panelEs = new JPanel();
		tabbedPane.addTab("Espa\u00F1ol", null, panelEs, null);
		panelEs.setLayout(null);
		panelEs.setBackground(Color.WHITE);
		
		JTextPane textPaneEs = new JTextPane();
		textPaneEs.setText("\u00A1Bienvenido!\r\n\r\nGracias por colaborar en nuestro experimento de medici\u00F3n de Internet.\r\n\r\nEsta aplicaci\u00F3n nos ayudar\u00E1 a encontrar el camino que siguen los paquetes de Internet de su computadora a una lista con los sitios web m\u00E1s populares.\r\n\r\nPara comenzar el experimento, haga clic en Start.");
		textPaneEs.setBounds(10, 0, 399, 127);
		panelEs.add(textPaneEs);
		
		JLabel labelFaqEs = new JLabel("Preguntas frecuentes...");
		labelFaqEs.setForeground(Color.BLUE);
		labelFaqEs.setBounds(10, 138, 399, 14);
		labelFaqEs.setCursor(new Cursor(Cursor.HAND_CURSOR));
		labelFaqEs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	try {
					open(new URI("http://mercury.upf.edu"));
				} catch (URISyntaxException e1) {
				}
            }
        });
		panelEs.add(labelFaqEs);
		
		JPanel panelEn = new JPanel();
		tabbedPane.addTab("English", null, panelEn, null);
		panelEn.setLayout(null);
		panelEn.setBackground(Color.WHITE);
		
		JTextPane textPaneEn = new JTextPane();
		textPaneEn.setText("Welcome!\r\n\r\nThank you for collaborating to our Internet measurement experiment.\r\n\r\nThis app will help us find the path from your computer to a list of popular web sites.\r\n\r\nTo begin the experiment, click Start.");
		textPaneEn.setEditable(false);
		textPaneEn.setBounds(10, 0, 399, 127);
		panelEn.add(textPaneEn);
		
		JLabel labelFaqEn = new JLabel("Frequently asked questions...");
		labelFaqEn.setForeground(Color.BLUE);
		labelFaqEn.setFont(UIManager.getFont("Label.font"));
		labelFaqEn.setBounds(10, 138, 399, 14);
		labelFaqEn.setCursor(new Cursor(Cursor.HAND_CURSOR));
		labelFaqEn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	try {
					open(new URI("http://mercury.upf.edu"));
				} catch (URISyntaxException e1) {
				}
            }
        });
		panelEn.add(labelFaqEn);
		
		labelLocale = new JLabel(getUserLocale());
		labelLocale.setBounds(388, 304, 46, 14);
		locale = labelLocale.getText();
		destinations = getURLs(locale, internetAnalyticsGetUrls);
		
		progressBar = new JProgressBar(0, destinations.size());
		progressBar.setBounds(0, 342, 484, 14);
		panelContent.add(progressBar);
		
		buttonStart = new JButton("Start");
		buttonStart.setIcon(new ImageIcon(AppLite.class.getResource("/edu/upf/nets/app/images/PlayStart_16.png")));
		buttonStart.setBounds(0, 300, 89, 23);
		
		buttonStop = new JButton("Stop");
		buttonStop.setIcon(new ImageIcon(AppLite.class.getResource("/edu/upf/nets/app/images/PlayStop_16.png")));
		buttonStop.setEnabled(false);
		buttonStop.setBounds(99, 300, 89, 23);
		
		buttonStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//We load the progress bar with urls
				buttonStart.setEnabled(false);
				buttonStop.setEnabled(true);
				labelTotalDestinations.setText("of "+ String.valueOf(destinations.size()));
				labelTotalDestinations.setVisible(true);
				executeTraceroute(destinations);
			}
		});
		
		buttonStop.addActionListener(new ActionListenerStop());
		
		
		
		panelContent.add(buttonStart);
		panelContent.add(buttonStop);
		
		JLabel labelIsp = new JLabel("Enter your ISP (e.g. Movistar, Vodafone):");
		labelIsp.setBounds(0, 202, 202, 14);
		panelContent.add(labelIsp);
		
		tfCity = new JTextField();
		tfCity.setBounds(212, 230, 272, 20);
		panelContent.add(tfCity);
		tfCity.setColumns(10);
		
		tfISP = new JTextField();
		tfISP.setColumns(10);
		tfISP.setBounds(212, 199, 272, 20);
		panelContent.add(tfISP);
		
		JLabel labelCity = new JLabel("Enter your city:");
		labelCity.setBounds(0, 233, 202, 14);
		panelContent.add(labelCity);
		
		JLabel labelAuthor = new JLabel("Enter your name (optional):");
		labelAuthor.setBounds(0, 258, 202, 14);
		panelContent.add(labelAuthor);
		
		tfAuthor = new JTextField();
		tfAuthor.setColumns(10);
		tfAuthor.setBounds(212, 261, 272, 20);
		panelContent.add(tfAuthor);
		
		panelContent.add(labelLocale);
		
		JPanel panelStatus = new JPanel();
		contentPane.add(panelStatus, BorderLayout.SOUTH);
		
		labelStatus = new JLabel("Stopped.");
		labelStatus.setHorizontalAlignment(SwingConstants.CENTER);
		labelStatus.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				try{
					if(Integer.parseInt(labelStatus.getText()) == 
							Integer.parseInt(labelTotalDestinations.getText().replace("of ", ""))){
						buttonStart.setEnabled(true);
						buttonStop.setEnabled(false);
						JOptionPane.showMessageDialog(null,
							    "Experiment finished.\nThank you for your participation!");
					}
				} catch(Exception e){
				}
				
			}
		});
		panelStatus.add(labelStatus);
		
		labelTotalDestinations = new JLabel("of");
		labelTotalDestinations.setHorizontalAlignment(SwingConstants.CENTER);
		labelTotalDestinations.setVisible(false);
		panelStatus.add(labelTotalDestinations);
		
	}
	
    private void setLookAndFeel(){
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Windows".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // If Windows is not available, you can set the GUI to another look and feel.
			print("LookAndFeel Windows not found. Using default theme.");
		}
    }
	
    protected void executeTraceroute(List<String> destinations){
    	
    	// Reset the cancellation token.
    	this.cancel.reset();
    	
    	//First we create the session in the server
    	String tracerouteSessionId = tfISP.getText() + "_" + tfCity.getText() + "_" + tfAuthor.getText();
    	TracerouteSession tracerouteSession = new TracerouteSession();
    	tracerouteSession.setSessionId(tracerouteSessionId);
    	tracerouteSession.setAuthor(tfAuthor.getText());
    	tracerouteSession.setDescription("Session done from ISP "+tfISP.getText() + " at city of " + tfCity.getText());
    	tracerouteSession.setDateStart(new Date());
    	addTracerouteSession(tracerouteSession);
    	
    	
		//Here we prepare a ThreadPool with X Threads
    	print("Parallel processes: "+POOL);
    	executorService = Executors.newFixedThreadPool(POOL);
		
		labelStatus.setText("0");
		progressBar.setValue(0);
		
		for (String destination : destinations) {
			print(destination);
			TracerouteWorker tw = new TracerouteWorker(
					destination,
					mercuryUploadTrace, 
					progressBar, 
					labelStatus, 
					tracerouteSessionId,
					cancel);
			executorService.submit(tw);
			//tw.execute(); //We don't need this if we use ThreadPool
		}
    }
    

	
	private List<String> getURLs(String locale, String serverURL){
		
		List<String> urlsList = new ArrayList<String>();
		try{

			URL url = new URL(serverURL+"?countryCode="+locale);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			
			int status = connection.getResponseCode();
			if ((status == 200) || (status == 201)) {
				//read the result from the server
				BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = null;
		        while (( line = rd.readLine()) != null) {
		        	urlsList.add(line.replaceAll("http://", ""));
		        }
			} else {
				print("Status: " + status);
				
				urlsList.addAll( Arrays.asList(
						new String[]{"google.com","facebook.com","twitter.com","amazon.com","yahoo.com"}
							)
						);
			}
			connection.disconnect();
			
		} catch(Exception e){
			urlsList.addAll( Arrays.asList(
					new String[]{"google.com","facebook.com","twitter.com","amazon.com","yahoo.com"}
						)
					);
		}
		
		return urlsList;
	}
	
	private int addTracerouteSession(TracerouteSession tracerouteSession){
		int status = 0;
		try{

			URL url = new URL(mercuryAddSession);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			
			status = connection.getResponseCode();
			if ((status == 200) || (status == 201)) {
				//read the result from the server
				BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = null;
				StringBuilder sb = new StringBuilder();
		        while (( line = rd.readLine()) != null) {
		        	sb.append(line);
		        }
		        print(sb.toString());
			} else {
				print("Status: " + status);
			}
			connection.disconnect();
			return status;
			
		} catch(Exception e){
			return status;
		}
	}
	
	private static void open(URI uri) {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(uri);
			} catch (IOException e) { /* TODO: error handling */ }
		} else { /* TODO: error handling */ }
	}
	
	private String getUserLocale(){
		
		//First we try to get locale from http://freegeoip.net/json/{IP_ADDRESS}
		String locale = "";
		try{

			URL url = new URL(freeGeoIp);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			
			int status = connection.getResponseCode();
			if ((status == 200) || (status == 201)) {
				//read the result from the server
				BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = null;
				StringBuilder sb = new StringBuilder();
		        while (( line = rd.readLine()) != null) {
		        	sb.append(line);
		        }
		        try{
		        JSONObject jsonObject = new JSONObject(sb.toString());
		        locale = (String) jsonObject.get("country_code");
		        } catch(Exception e){
					print("Error parsing fregeoip json object. Geting locale from OS.");
					locale = this.getLocale().getCountry();
		        }
			} else {
				print("Request failed. Status: " + status+". Geting locale from OS.");
				locale = this.getLocale().getCountry();
			}
			connection.disconnect();
			
		} catch(Exception e){
			print("Connection failed to http://freegroip.net. Getting locale from OS.");
			locale = this.getLocale().getCountry();
		}
		return locale;
	}
	
	
	
    private void print(String msg, Object... args) {
        log.info(String.format(msg, args));
    }
}
