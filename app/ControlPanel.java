package app;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class will accept command line arguments, will initialize MessagePasser
 * class and will accept send command arguments from input console.
 * 
 * @author Ravi Chandra
 * @author Yinsu Chu
 * 
 */
public class ControlPanel {
	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "send <process_name> <kind> <message>";
	private static final String SEND_CMD = "send";
	private static final int SEND_NUM_PARAM = 4;
	private static final String QUIT_CMD = "quit";

	private Logger logger;
	private MessagePasser messagePasser;

	private Receiver receiver;
	private Thread receiverThread;

	/**
	 * 
	 * This class is used by ControlPanel to go through receive buffer and will
	 * print received messages onto console.
	 * 
	 * @author Ravi Chandra
	 * @author Yinsu Chu
	 * 
	 */
	private class Receiver implements Runnable {
		public void run() {
			logger.info("receiver thread started");
			while (true) {
				Message message = messagePasser.receive();
				String display = "message received - " + message.toString();
				System.out.println(display);
				logger.info(display);
			}
		}
	}

	/**
	 * 
	 * This method will accept commands from input console, will do sanity
	 * checks and will construct message.
	 * 
	 * @param configurationFileName
	 *            Name of file which is uploaded in Dropbox.
	 * @param localName
	 *            Name of the local node.
	 * 
	 */
	public void startUserInterface(String configurationFileName,
			String localName) {
		logger = LoggerFactory.getLogger(ControlPanel.class);
		logger.info("user interface started");
		messagePasser = new MessagePasser(configurationFileName, localName);
		logger.info("starting receiver thread...");

		// initializing Receiver class and starting thread
		receiver = new Receiver();
		receiverThread = new Thread(receiver);
		receiverThread.start();

		// accepting commands from input console
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("DS-Lab0>> ");
			String cmd = scanner.nextLine();
			logger.info("user command - " + cmd);
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.startsWith(SEND_CMD)) {
				String[] parsedLine = cmd.split("\\s+", SEND_NUM_PARAM);
				if (parsedLine.length == SEND_NUM_PARAM
						&& parsedLine[0].equals(SEND_CMD)) {
					Message message = new Message(parsedLine[1], parsedLine[2],
							parsedLine[3]);
					messagePasser.send(message);
				} else {
					String display = "invalid command format";
					System.out.println(display);
					logger.error(display);
				}
			} else if (cmd.equals(QUIT_CMD)) {
				logger.info("terminating program...");
				scanner.close();
				System.exit(-1);
			}
			if (!receiverThread.isAlive()) {
				String display = "ControlPanel health check: receiver thread died";
				System.out.println(display);
				logger.error(display);
			}
		}
	}
}
