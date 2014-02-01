package app;

import ipc.MessagePasser;
import ipc.TimeStampedMessage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import clock.ClockService;
import clock.TimeStamp;

/**
 * This class demonstrates the centralized logging facility for our distributed
 * system.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class Logger {
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: java -cp :snakeyaml-1.11.jar app/Logger <configuration_file_name> <log_name>";
	private static final String HELP_CMD = "help";
	private static final String HELP_CONTENT = "dump (type quit to exit)";
	private static final String DUMP_CMD = "dump";
	private static final String QUIT_CMD = "quit";
	private static final String EVENT_CMD = "event";
	private static final String TIME_CMD = "time";
	private static final String LOG_NAME = "logger.txt";

	private MessagePasser messagePasser;

	// store all the massages that received
	private ArrayList<TimeStampedMessage> allMsg;
	private ReentrantLock msgLock;

	private class LoggerWorker implements Runnable {
		public LoggerWorker() {
			allMsg = new ArrayList<TimeStampedMessage>();
			msgLock = new ReentrantLock();
		}

		public void run() {
			while (true) {
				TimeStampedMessage tsm = (TimeStampedMessage) messagePasser
						.receive();
				msgLock.lock();
				allMsg.add(tsm);
				msgLock.unlock();
			}
		}
	}

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
	public void startLogger(String configurationFileName, String logName) {
		FileWriter logWriter = null;
		messagePasser = new MessagePasser(configurationFileName, logName);
		while (!messagePasser.parseConfigurationFinished()) {
			continue;
		}
		if (messagePasser.getClockServiceType() != ClockService.ClockType.DEFAULT) {
			ClockService.initialize(messagePasser.getNumOfNodes(),
					messagePasser.getClockServiceType(),
					messagePasser.getLocalNodeId());
		}
		messagePasser.initialize();

		LoggerWorker lw = new LoggerWorker();
		Thread lwThread = new Thread(lw);
		lwThread.start();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("Logger>> ");
			String cmd = scanner.nextLine();
			if (cmd.equals(HELP_CMD)) {
				System.out.println(HELP_CONTENT);
			} else if (cmd.startsWith(DUMP_CMD)) {
				try {
					logWriter = new FileWriter(LOG_NAME);
					msgLock.lock();
					if (allMsg.size() == 0) {
						msgLock.unlock();
						continue;
					}
					Collections.sort(allMsg);
					TimeStampedMessage pre = allMsg.get(0);
					logWriter.write(pre.toString());
					for (int i = 1; i < allMsg.size(); ++i) {
						TimeStampedMessage cur = allMsg.get(i);
						if (cur.compareTo(pre) == 0) {
							logWriter.write("\t\t" + cur.toString());
						} else if (cur.compareTo(pre) != 0) {
							logWriter.write('\n' + cur.toString());
						}
						pre = cur;
					}
					logWriter.close();
					msgLock.unlock();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} else if (cmd.equals(EVENT_CMD)) {
				TimeStamp ts = ClockService.getInstance().updateLocalTime();
				System.out.println("local time updated to: " + ts.toString());
			} else if (cmd.equals(TIME_CMD)) {
				TimeStamp ts = ClockService.getInstance().getLocalTimeCopy();
				System.out.println("local time: " + ts.toString());
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
