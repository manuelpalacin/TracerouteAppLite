package edu.upf.nets.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;


public class TracerouteWorker extends SwingWorker<String, Integer> {

	
	private static final Logger log = Logger
			.getLogger(TracerouteWorker.class.getName());
	
	private final String destination;
	private final String mercuryUploadTrace;
	private final JProgressBar progressBar;
	private final JLabel labelStatus;
	private int status;
	private final String sessionId;
	private final CancellationToken cancel;
	
	/**
	 * Constructor
	 * We pass it the textarea component to be updated
	 * @param destination
	 * @param mercuryUploadTrace
	 * @param taTraceroute
	 */
    public TracerouteWorker(
    		String destination, 
    		String mercuryUploadTrace, 
    		JProgressBar progressBar, 
    		JLabel labelStatus,
    		String sessionId,
    		CancellationToken cancel) {
    	
    	this.destination = destination;
    	this.mercuryUploadTrace = mercuryUploadTrace; 
        this.progressBar = progressBar;
        this.labelStatus = labelStatus;
        this.sessionId = sessionId;
        this.cancel = cancel;
    }
	
    /**
     * SwingWorker demanding task is executed here
     * 
     */
	@Override
	protected String doInBackground() throws Exception {

		traceroute(destination);
		
		return null;
	}
	
    /**
     * Task done, SwingWorker calls this method in the event thread.
     * Here we update the component xxxx
     */
    @Override
    protected void done() {
    }
	
    /**
     * SwingWorker calls this method in the events thread when 
     * we call the publish method and it passes the same parameters.
     * We use it for updating the progress bar
     */
    @Override
    protected void process(List<Integer> chunks) {
    	progressBar.setValue(progressBar.getValue()+1);
    	//This is because when we stop the App, we only kill the Threads but not the traceroute Runtimes
    	// and this makes that when the remaining traceroutes finish, they update the field wrongly
    	try{
    		labelStatus.setText( String.valueOf(Integer.parseInt(labelStatus.getText())+1) );
    	} catch(Exception e){
    		progressBar.setValue(0);
    		labelStatus.setText("Stopped.");
    	}
    }

	private void traceroute(String destination){
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("windows") != -1) {
			// -d to avoid name resolution
			String[] command = { "tracert", "-d", "-w", "500", "-h", "30", destination }; 
			exec(command);
		} else if (osName.indexOf("mac os x") != -1) {
			// -n to avoid name resolution, -w to set waittime, -m to set the max TTL
			String[] command = { "traceroute", "-n", "-w", "1", "-m", "30", destination };
			exec(command);
		} else {
			// -n to avoid name resolution
			String[] command = { "traceroute", "-n", "-w", "1", "-m", "30", destination }; 
			exec(command);
		}
		print(osName);
	}
	
	private void exec(String[] command) {

		//This checks if the app is canceled
		if (this.cancel.isCanceled()) return;
		
		try {
			
			ProcessBuilder builder = new ProcessBuilder(command);
			final Process process = builder.start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			List<String> lines = new ArrayList<String>();
			
			while ((line = br.readLine()) != null) {
				print(line);
				//taTraceroute.append(line + "\n");
				//taTraceroute.update(taTraceroute.getGraphics());
				
				lines.add(line);
			}

			int retval = process.waitFor();
			print("Retval: " + retval);

			// Patterns
			String patternHop = "(^\\s+\\d{1,2}|^\\d{1,2})";
			String patternIp = "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})";

			Matcher matcherReply;
			String firstLine;
			String traceroute = "{\"destination\":\"" + command[6]
					+ "\",\"hops\":[]}";
			// When traceroute process finishes, we analyze the output line per
			// line
			if (retval == 0) {
				// We obtain the destination IP from the first line
				if ((firstLine = lines.get(0)) != null) {
					// Workaround for Windows. Data starts at second line
					if ((firstLine = lines.get(0)).equals("")) {
						if ((firstLine = lines.get(1)) != null) {
							if ((matcherReply = Pattern.compile(patternIp)
									.matcher(firstLine)).find()) {
								firstLine = matcherReply.group(0);
							}
						}
					} else {
						if ((matcherReply = Pattern.compile(patternIp).matcher(
								firstLine)).find()) {
							firstLine = matcherReply.group(0);
						}
					}
				}
				// We process each hop
				String hops = "";
				for (String lineAux : lines) {
					String hop;
					if ((matcherReply = Pattern.compile(patternHop).matcher(
							lineAux)).find()) {
						String hopId = matcherReply.group(0).trim();
						// hop.setId(matcherReply.group(0).trim()); //We add the
						// hop Id
						if ((matcherReply = Pattern.compile(patternIp).matcher(
								lineAux)).find()) {
							hop = "{\"id\":\"" + hopId + "\",\"ip\":\""
									+ matcherReply.group(0) + "\" }";
							// hop.setIp(matcherReply.group(0)); //We add the
							// hop Ip
						} else {
							hop = "{\"id\":\"" + hopId
									+ "\",\"ip\":\"destination unreachable\" }";
							// hop.setIp("host unreachable"); //We add the hop
							// Ip unreachable
						}
						// traceroute.addHops(hop);
						if (hops.equals("")) {
							hops = hop;
						} else {
							hops = hops + "," + hop;
						}
					}
				}
				
				String srcIp = "";//InetAddress.getLocalHost().getHostAddress();
				String srcName = InetAddress.getLocalHost().getHostName();
				traceroute = "{\"srcIp\":\"" + srcIp
						+ "\",\"srcName\":\"" + srcName
						+ "\",\"sessionId\":\"" + sessionId
						+ "\",\"dstName\":\"" + command[6]
						+ "\",\"dstIp\":\"" + firstLine + "\",\"hops\":[" + hops
						+ "]}";
			}

			//We store a file and then we send the data
			saveData(traceroute, destination);
			String result = postData(traceroute);
			print(result);
			publish(1);


		} catch (Exception exception) {
			print("Interrupted exception because we have stopped the application");
		}

	}
	
	
	private synchronized void saveData(String data, String destination){
		try {
			 
 
			File file = new File(sessionId+"/"+destination+"-"+new Date().getTime()+".json");
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				//file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(data);
			bw.close();
 
			print("Traceroute data stored in >>> "+file.getCanonicalPath());
 
		} catch (IOException e) {
			e.printStackTrace();
			print("Problems writing the file");
		}
	}
	
	private String postData(String data) {

		String result;
		status = 0;
		try {
			URL url = new URL(mercuryUploadTrace);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setRequestProperty("Content-Type",
					"application/json;charset=UTF-8");

			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.write(data.getBytes("UTF-8"));
			wr.flush();
			wr.close();

			this.status = connection.getResponseCode();
			print("Status: " + status);
			if ((status == 200) || (status == 201)) {
				print("Added traceroute data!");
				result = "Added traceroute data to Mercury. Thanks for participating!";
			} else {
				print("Problems adding entry. Server response status: "
						+ status);
				result = "Problems adding traceroute data to Mercury server. Try again later. Server response status: "
						+ status;
			}

			connection.disconnect();

		} catch (Exception e) {
			result = "Problems adding entry. i.e. Mercury server not reachable";
		}
		return result;
	}
	
    private void print(String msg, Object... args) {
        log.info(String.format(msg, args));
    }


}

