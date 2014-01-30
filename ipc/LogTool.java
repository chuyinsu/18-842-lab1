package ipc;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

public class LogTool {
	private String className;
	private PrintWriter output;
	private SimpleDateFormat datetime;

	public LogTool(String logFileName, String className) {
		this.className = className;
		try {
			this.output = new PrintWriter(logFileName);
		} catch (IOException ex) {
			System.out
					.println("failed to initialize LogTool, logs will be printed to stdout");
			this.output = null;
		}
		this.datetime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
	}

	private String getLogItem(String message, String type) {
		return "[" + datetime.format(System.currentTimeMillis()) + "]["
				+ className + "][" + type + "] " + message;
	}

	private void logToFile(String message, String type) {
		String item = getLogItem(message, type);
		if (output != null) {
			output.println(item);
			output.flush();
		} else {
			System.out.println(item);
		}
	}

	public void info(String message) {
		logToFile(message, "INFO");
	}

	public void error(String message) {
		logToFile(message, "ERROR");
		System.out.println("error: " + message);
	}
}
