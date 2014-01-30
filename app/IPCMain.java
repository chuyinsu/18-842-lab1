package app;

/**
 * 
 * This is the main class which will do sanity checks on command line arguments
 * and will invoke ControlPanel class.
 * 
 * @author Ravi Chandra
 * @author Yinsu Chu
 * 
 */
public class IPCMain {
	private static final int NUM_CMD_ARG = 2;
	private static final String USAGE = "usage: "
			+ "java -Dlogback.configurationFile=./logback.xml "
			+ "-jar lab0.jar <configurationFileName> <localName>";

	public static void main(String[] args) {
		if (args.length != NUM_CMD_ARG) {
			System.err.println(USAGE);
			System.exit(-1);
		}
		ControlPanel cp = new ControlPanel();
		cp.startUserInterface(args[0], args[1]);
	}
}
