package app;

import ipc.Message;
import ipc.MessagePasser;

import java.util.Scanner;

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
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "dummy";
	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "send <process_name> <kind> <message>";
	private static final String SEND_CMD = "send";
	private static final int SEND_NUM_PARAM = 4;
	private static final String QUIT_CMD = "quit";

	private MessagePasser messagePasser;

	private Receiver receiver;
	private Thread receiverThread;

	/**
	 * 
	 * This class is used by ControlPanel to go through receive buffer and
	 * prints received messages to console.
	 * 
	 * @author Ravi Chandra
	 * @author Yinsu Chu
	 * 
	 */
	private class Receiver implements Runnable {
		public void run() {
			while (true) {
				Message message = messagePasser.receive();
				System.out.println("message received - " + message.toString());
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
		messagePasser = new MessagePasser(configurationFileName, localName);

		// initializing Receiver class and starting thread
		receiver = new Receiver();
		receiverThread = new Thread(receiver);
		receiverThread.start();

		// accepting commands from input console
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("DS-Lab0>> ");
			String cmd = scanner.nextLine();
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
					System.out.println("invalid command");
				}
			} else if (cmd.equals(QUIT_CMD)) {
				scanner.close();
				System.exit(-1);
			}
			if (!receiverThread.isAlive()) {
				System.out
						.println("ControlPanel health check: receiver thread died");
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.out.println(USAGE);
			System.exit(-1);
		}
		ControlPanel cp = new ControlPanel();
		cp.startUserInterface(args[0], args[1]);
	}
}
