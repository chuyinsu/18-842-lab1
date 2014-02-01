package app;

import ipc.Message;
import ipc.MessagePasser;
import ipc.TimeStampedMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

import clock.ClockService;


/**
 * This class demonstrates the centralized logging facility for
 * our distributed system
 * 
 * @author Yinsu Chu
 * @author Jason Xi
 * 
 */
public class Logger {

	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: java -cp :snakeyaml-1.11.jar app/Logger <configuration_file_name> <log_name>";
	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "dump(type quit to exit)";
	private static final String DUMP_CMD = "dump";
	private static final String QUIT_CMD = "quit";
	private static final String LOG_NAME = "logger.txt";

	private MessagePasser messagePasser;

	/**
	 * This method launches a simple command-line user interface to operate on
	 * the centralized logging facility.
	 * 
	 * @param configurationFileName
	 *            Name of file which is uploaded in Dropbox.
	 * @param logName
	 *            Name of the local node.
	 * 
	 */
	public void startLogger(String configurationFileName,
			String logName) {

		messagePasser = new MessagePasser(configurationFileName, logName);

		FileWriter logWriter = null;
		while (!messagePasser.parseConfigurationFinished()) {
			continue;
		}
		if (messagePasser.getClockServiceType() != ClockService.ClockType.DEFAULT) {
			ClockService.initialize(messagePasser.getNumOfNodes(),
					messagePasser.getClockServiceType(),
					messagePasser.getLocalNodeId());
		}
		messagePasser.initialize();
		
		// Store all the massages that received
		ArrayList<TimeStampedMessage> allMsg = new ArrayList<TimeStampedMessage>();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("Logger>> ");
			String cmd = scanner.nextLine();
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.startsWith(DUMP_CMD)) {
				LinkedBlockingQueue<Message> buffer = messagePasser.getReceiveBuffer();
				try {
					logWriter = new FileWriter(LOG_NAME);
					while(buffer.size() > 0) {
						TimeStampedMessage tmp = (TimeStampedMessage) messagePasser.receive();
						allMsg.add(tmp);
					}
					// Sort all the messages
					Collections.sort(allMsg);
					if(allMsg.size() == 0)
						continue;
					TimeStampedMessage pre = allMsg.get(0);
					logWriter.write(pre.toString());
					for(int i = 1; i < allMsg.size(); ++i) {
						TimeStampedMessage cur = allMsg.get(i);
						if(cur.compareTo(pre) == 0) {
							logWriter.write("\t\t" + cur.toString() );
						}
						else if(cur.compareTo(pre) != 0) {
							logWriter.write('\n' + cur.toString() );
						}
						pre = cur;
					}
					logWriter.close();	
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
			} else if (cmd.equals(QUIT_CMD)) {
				scanner.close();
				System.exit(-1);
			}
		}

	}

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.out.println(USAGE);
			System.exit(-1);
		}
		Logger log = new Logger();
		log.startLogger(args[0], args[1]);
	}


}
