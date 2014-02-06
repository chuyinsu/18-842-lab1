package ipc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.yaml.snakeyaml.Yaml;

import clock.ClockService;
import clock.TimeStamp;

/**
 * This class spawns a thread for each node to receive messages and create a
 * socket to the remote end when sending messages. The receiving thread will
 * keep running and socket created for sending will be saved for later use.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class MessagePasser {
	private static final int CONFIG_CHANGE_CHECK_INTERVAL = 10000;

	private static final String ITEM_CONFIGURATION = "configuration";
	private static final String ITEM_SEND_RULES = "sendRules";
	private static final String ITEM_RECEIVE_RULES = "receiveRules";
	private static final String CLOCK_SERVICE_TYPE = "clockService";
	private static final String CLOCK_SERVICE_LOGICAL = "logical";
	private static final String CLOCK_SERVICE_VECTOR = "vector";
	private static final String CONTACT_NAME = "name";
	private static final String CONTACT_IP = "ip";
	private static final String CONTACT_PORT = "port";
	private static final String RULE_ACTION = "action";
	private static final String RULE_SRC = "src";
	private static final String RULE_DST = "dest";
	private static final String RULE_KIND = "kind";
	private static final String RULE_SEQ_NUM = "seqNum";
	private static final String RULE_DUP = "dupe";
	private static final String ACTION_DROP = "drop";
	private static final String ACTION_DUPLICATE = "duplicate";
	private static final String ACTION_DELAY = "delay";

	private String configurationFileName;
	private String localName;

	private LogTool logger;

	private LinkedBlockingQueue<Message> sendBuffer;
	private LinkedBlockingQueue<Message> receiveBuffer;

	// maps from remote node names to sockets
	private HashMap<String, Socket> socketMap;

	// a global sequence number
	private int seqNum;

	// maps from remote node names to their contact information (IP and port)
	private HashMap<String, Contact> contactMap;

	private ClockService.ClockType type;
	private int localNodeId;

	private ReentrantLock rulesLock;
	private ArrayList<HashMap<String, Object>> sendRules;
	private ArrayList<HashMap<String, Object>> receiveRules;

	private Watcher watcher;
	private Sender sender;
	private Receiver receiver;

	private Thread watcherThread;
	private Thread senderThread;
	private Thread receiverThread;

	private ServerSocket serverSocket;

	private volatile boolean initialized;

	/**
	 * A private class to store remote node information.
	 * 
	 * @author Jason Xi
	 * @author Yinsu Chu
	 * 
	 */
	private class Contact {
		private String IP;
		private int port;

		public Contact(String IP, int port) {
			this.IP = IP;
			this.port = port;
		}
	}

	/**
	 * This thread keeps watching for configuration updates and checking the
	 * health of sender and receiver threads.
	 * 
	 * @author Jason Xi
	 * @author Yinsu Chu
	 * 
	 */
	private class Watcher implements Runnable {
		private volatile boolean configurationParsed;
		private String ETag;
		private String configurationFileURL;
		private String configurationFileNameNew;
		private HttpURLConnection connection;

		public Watcher() {
			this.configurationParsed = false;
			this.ETag = "initial";
			this.configurationFileURL = "https://dl.dropboxusercontent.com/s/cxk7aexjpnq2aor/"
					+ configurationFileName + "?dl=1";
			this.configurationFileNameNew = configurationFileName + ".new";
			this.connection = null;
			downloadConfigurationFile();
			yamlExtraction(configurationFileNameNew, true);
		}

		public void run() {
			logger.info("watcher thread started");
			while (true) {
				try {
					Thread.sleep(CONFIG_CHANGE_CHECK_INTERVAL);
				} catch (InterruptedException ex) {
					logger.error("interrupted when waiting to check config change - "
							+ ex.getMessage());
				}
				if (downloadConfigurationFile()) {
					updateRules();
				}
				if (!senderThread.isAlive()) {
					logger.error("health check: sender thread died");
				}
				if (!receiverThread.isAlive()) {
					logger.error("health check: receiver thread died");
				}
			}
		}

		/**
		 * Detect whether there is a new configuration file and download it.
		 * 
		 * @return True on new configuration file detected, false otherwise.
		 */
		private boolean downloadConfigurationFile() {
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			boolean configChanged = false;
			try {
				URL url = new URL(configurationFileURL);
				connection = (HttpURLConnection) url.openConnection();

				// we use ETag to see whether the configuration file has changed
				connection.setRequestProperty("If-None-Match", ETag);

				int responseCode = connection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					String display = "new config file detected";
					System.out.println(display);
					logger.info(display);
					InputStream is = connection.getInputStream();
					bis = new BufferedInputStream(is);
					FileOutputStream fos = new FileOutputStream(
							configurationFileNameNew);
					bos = new BufferedOutputStream(fos);
					int input = 0;
					while ((input = bis.read()) != -1) {
						bos.write(input);
					}
					bos.flush();
					ETag = connection.getHeaderField("ETag");
					configChanged = true;
				} else if (responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
					logger.error("unexpected HTTP responce code - "
							+ responseCode + ", failed to download config file");
				}
			} catch (Exception ex) {
				logger.error("failed to download config file - "
						+ ex.getMessage());
			} finally {
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException ex) {
						logger.error("failed to close input - "
								+ ex.getMessage());
					}
				}
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException ex) {
						logger.error("failed to close output - "
								+ ex.getMessage());
					}
				}
				if (connection != null) {
					connection.disconnect();
					connection = null;
				}
			}
			return configChanged;
		}

		/**
		 * Delete the old configuration file and use the new one.
		 */
		private void updateRules() {
			logger.info("deleting old config file");
			File oldConfigFile = new File(configurationFileName);
			if (!oldConfigFile.delete()) {
				logger.error("failed to delete old config file");
			}
			logger.info("renaming new config file");
			File newConfigFile = new File(configurationFileNameNew);
			if (!newConfigFile.renameTo(oldConfigFile)) {
				logger.error("failed to rename config file");
			}
			yamlExtraction(configurationFileName, false);
		}

		/**
		 * Load information from YAML configuration file.
		 * 
		 * @param configurationFile
		 *            The name of the configuration file.
		 * @param loadContacts
		 *            True on loading remote node information, false otherwise.
		 */
		@SuppressWarnings("unchecked")
		private void yamlExtraction(String configurationFile,
				boolean loadContacts) {
			Yaml yaml = new Yaml();
			InputStream is = null;
			try {
				is = new FileInputStream(configurationFile);
			} catch (IOException ex) {
				logger.error("failed to open config file - " + ex.getMessage());
			}
			HashMap<String, ArrayList<HashMap<String, Object>>> yamlMap = (HashMap<String, ArrayList<HashMap<String, Object>>>) (yaml
					.load(is));

			/*
			 * send and receive rules might be used by sender and receiver
			 * threads so we need a lock to protect them
			 */
			rulesLock.lock();

			for (Map.Entry<String, ArrayList<HashMap<String, Object>>> entry : yamlMap
					.entrySet()) {
				if (entry.getKey().equals(ITEM_CONFIGURATION) && loadContacts) {
					localConfiguration(entry.getValue());
				} else if (entry.getKey().equals(ITEM_SEND_RULES)) {
					sendRules = entry.getValue();
				} else if (entry.getKey().equals(ITEM_RECEIVE_RULES)) {
					receiveRules = entry.getValue();
				}
			}
			rulesLock.unlock();
			try {
				is.close();
			} catch (IOException ex) {
				logger.error("failed to close config file - " + ex.getMessage());
			}
		}

		/**
		 * Load the configuration part of the YAML file, will be called only
		 * once upon starting.
		 * 
		 * @param configRules
		 *            The configuration part parsed from the YAML file.
		 */
		private void localConfiguration(
				ArrayList<HashMap<String, Object>> configRules) {
			for (HashMap<String, Object> map : configRules) {
				if (map.containsKey(CLOCK_SERVICE_TYPE)) {
					String service = (String) map.get(CLOCK_SERVICE_TYPE);
					if (service.equals(CLOCK_SERVICE_LOGICAL)) {
						type = ClockService.ClockType.LOGICAL;
						logger.info("clock service: logical");
					} else if (service.equals(CLOCK_SERVICE_VECTOR)) {
						type = ClockService.ClockType.VECTOR;
						logger.info("clock service: vector");
					} else {
						logger.error("invalid clock service type");
					}
				} else {
					String name = (String) map.get(CONTACT_NAME);
					String IP = (String) map.get(CONTACT_IP);
					Integer port = (Integer) map.get(CONTACT_PORT);
					Contact contact = new Contact(IP, (int) port);
					contactMap.put(name, contact);
				}
			}
			if (!contactMap.containsKey(localName)) {
				logger.error("local name " + localName
						+ " does not exist in the config file");
				configurationParsed = true;
				return;
			}
			PriorityQueue<String> heap = new PriorityQueue<String>();
			for (String s : contactMap.keySet()) {
				heap.add(s);
			}
			while (!heap.isEmpty()) {
				String name = heap.poll();
				if (name.equals(localName)) {
					break;
				} else {
					localNodeId++;
				}
			}
			configurationParsed = true;
			logger.info("local node ID: " + localNodeId);
			logger.info("total number of nodes: " + contactMap.size());
		}
	}

	/**
	 * This thread keeps taking messages from the send buffer and send them.
	 * 
	 * @author Jason Xi
	 * @author Yinsu Chu
	 * 
	 */
	private class Sender implements Runnable {
		private LinkedBlockingQueue<Message> delayBuffer;

		public Sender() {
			this.delayBuffer = new LinkedBlockingQueue<Message>();
		}

		public void run() {
			while (!initialized) {
				continue;
			}
			logger.info("sender thread started");
			if (!contactMap.containsKey(localName)) {
				return;
			}
			while (true) {
				try {
					Message message = sendBuffer.take();
					Socket clientSocket = null;
					String dest = message.getDest();

					// cannot send if receiver does not exist
					if (!contactMap.containsKey(dest)) {
						logger.error("process with name " + dest
								+ " dose not exist");
						continue;
					}

					// if the socket does not exist, create one before sending
					if (!socketMap.containsKey(dest)) {
						Contact contact = contactMap.get(dest);
						clientSocket = NetTool.createSocket(contact.IP,
								contact.port, logger);
						if (clientSocket == null) {
							logger.error("problem in creating socket when sending message");
							continue;
						}
						socketMap.put(dest, clientSocket);
					} else {
						clientSocket = socketMap.get(dest);
					}

					message.setSource(localName);
					message.setSequenceNumber(seqNum++);
					message.setDupe(false);

					// match rules before sending
					String action = checkRules(message, sendRules);
					if (action == null) {
						if (!sendMessage(clientSocket, message)) {
							logger.error("failed to send message - "
									+ message.toString());
							socketMap.remove(dest);
						} else {
							logger.info("message sent - " + message.toString());
						}
						clearDelayBuffer(clientSocket);
					} else if (action.equals(ACTION_DROP)) {
						logger.info("match drop rule when sending, message "
								+ message.toString() + "dropped");
						continue;
					} else if (action.equals(ACTION_DELAY)) {
						logger.info("match delay rule when sending, message "
								+ message.toString() + "delayed");
						delayBuffer.put(message);
					} else if (action.equals(ACTION_DUPLICATE)) {
						logger.info("match duplicate rule when sending, message "
								+ message.toString() + "duped");
						Message dup = new TimeStampedMessage(null, null, null);
						dup.setDest(dest);
						dup.setKind(message.getKind());
						dup.setData(message.getData());
						dup.setSource(message.getSource());
						dup.setSequenceNumber(message.getSequenceNumber());
						dup.setDupe(true);
						if (!sendMessage(clientSocket, message)) {
							logger.error("failed to send message - "
									+ message.toString());
							socketMap.remove(dest);
						} else {
							logger.info("message sent - " + message.toString());
						}
						if (!sendMessage(clientSocket, dup)) {
							logger.error("failed to send message - "
									+ message.toString());
							socketMap.remove(dest);
						} else {
							logger.info("message sent - " + message.toString());
						}
						clearDelayBuffer(clientSocket);
					}
				} catch (InterruptedException ex) {
					logger.error("interrupted when sending message: "
							+ ex.getMessage());
				}
			}
		}

		/**
		 * Upon each sending, clear delay buffer.
		 * 
		 * @param clientSocket
		 *            The socket to the remote side.
		 */
		private void clearDelayBuffer(Socket clientSocket) {
			while (!delayBuffer.isEmpty()) {
				try {
					Message message = delayBuffer.take();
					if (!sendMessage(clientSocket, message)) {
						logger.error("failed to send message - "
								+ message.toString());
						socketMap.remove(message.getDest());
					} else {
						logger.info("message sent - " + message.toString());
					}
				} catch (InterruptedException e) {
					logger.error("interrutped when clearing delay buffer: "
							+ e.getMessage());
				}
			}
		}

		/**
		 * Send a message from the given socket.
		 * 
		 * @param socket
		 *            The socket to send the message.
		 * @param message
		 *            The message to send.
		 * @return True on success, false otherwise.
		 */
		private boolean sendMessage(Socket socket, Message message) {
			OutputStream output = null;
			ObjectOutputStream objectOutput = null;
			try {
				output = socket.getOutputStream();
			} catch (Exception ex) {
				logger.error("failed to get output stream - " + ex.getMessage());
				return false;
			}
			try {
				objectOutput = new ObjectOutputStream(output);
			} catch (Exception ex) {
				logger.error("failed to create object output stream - "
						+ ex.getMessage());
				if (objectOutput != null) {
					try {
						objectOutput.close();
					} catch (Exception nestedEx) {
						logger.error("failed to close object output stream - "
								+ nestedEx);
					}
				}
				return false;
			}
			try {
				objectOutput.writeObject(message);
			} catch (Exception ex) {
				logger.error("failed to send message - " + ex.getMessage());
				if (objectOutput != null) {
					try {
						objectOutput.close();
					} catch (Exception nestedEx) {
						logger.error("failed to close object output stream - "
								+ nestedEx);
					}
				}
				return false;
			}
			return true;
		}
	}

	/**
	 * This thread listens on the local server socket and spanws a worker thread
	 * if a remote node tries to send messages.
	 * 
	 * @author Jason Xi
	 * @author Yinsu Chu
	 * 
	 */
	private class Receiver implements Runnable {
		private ReentrantLock delayBufferLock;
		private LinkedBlockingQueue<Message> delayBuffer;

		public Receiver() {
			this.delayBufferLock = new ReentrantLock();
			this.delayBuffer = new LinkedBlockingQueue<Message>();
		}

		/**
		 * This thread is created if a remote host tries to send messages to the
		 * local node. Once created, it will continue to run to receive any
		 * future messages.
		 * 
		 * @author Jason Xi
		 * @author Yinsu Chu
		 * 
		 */
		private class ReceiverWorker implements Runnable {
			private Socket clientSocket;

			public ReceiverWorker(Socket clientSocket) {
				this.clientSocket = clientSocket;
			}

			public void run() {
				logger.info("receiver worker for "
						+ clientSocket.getRemoteSocketAddress().toString()
						+ " started");
				while (true) {
					Message message = receiveMessage(clientSocket);

					/*
					 * if failed to receive messages from the socket, it is
					 * probably the case the the socket has failed
					 */
					if (message == null) {
						logger.error("failed to receive message from socket");
						NetTool.destroySocket(clientSocket, logger);
						return;
					} else {
						logger.info("message received - " + message.toString());
					}

					String action = checkRules(message, receiveRules);
					try {
						if (action == null) {
							receiveBuffer.put(message);
							clearDelayBuffer();
						} else if (action.equals(ACTION_DROP)) {
							logger.info("match drop rule when receiving, message "
									+ message.toString() + "dropped");
							continue;
						} else if (action.equals(ACTION_DELAY)) {
							logger.info("match delay rule when receiving, message "
									+ message.toString() + "delayed");
							delayBufferLock.lock();
							delayBuffer.put(message);
							delayBufferLock.unlock();
						} else if (action.equals(ACTION_DUPLICATE)) {
							logger.info("match duplicate rule when receiving, message "
									+ message.toString() + "duped");
							Message dup = new TimeStampedMessage(null, null,
									null);
							dup.setDest(new String(message.getDest()));
							dup.setKind(new String(message.getKind()));
							dup.setData(message.getData());
							dup.setSource(new String(message.getSource()));
							dup.setSequenceNumber(message.getSequenceNumber());
							dup.setDupe(message.isDupe());
							receiveBuffer.put(message);
							receiveBuffer.put(dup);
							clearDelayBuffer();
						}
					} catch (InterruptedException ex) {
						logger.error("interrupted when waiting for empty slots in receive buffers - "
								+ ex.getMessage());
					}
				}
			}

			/*
			 * Upon each receiving, clear delay buffer.
			 */
			private void clearDelayBuffer() {
				try {
					delayBufferLock.lock();
					while (!delayBuffer.isEmpty()) {
						receiveBuffer.put(delayBuffer.take());
					}
					delayBufferLock.unlock();
				} catch (InterruptedException ex) {
					logger.error("interrupted when clearing delay buffer - "
							+ ex.getMessage());
				}
			}

			/**
			 * Receive a message from the given socket.
			 * 
			 * @param socket
			 *            The socket to receive the message.
			 * @return The received message, null on failure.
			 */
			private Message receiveMessage(Socket socket) {
				InputStream input = null;
				ObjectInputStream objectInput = null;
				try {
					input = socket.getInputStream();
				} catch (Exception ex) {
					logger.error("failed to get input stream - "
							+ ex.getMessage());
					return null;
				}
				try {
					objectInput = new ObjectInputStream(input);
				} catch (Exception ex) {
					logger.error("failed to get object input stream - "
							+ ex.getMessage());
					if (objectInput != null) {
						try {
							objectInput.close();
						} catch (Exception nestedEx) {
							logger.error("failed to close object input stream - "
									+ nestedEx);
						}
					}
					return null;
				}
				Message incomingMessage = null;
				try {
					incomingMessage = (Message) objectInput.readObject();
				} catch (Exception ex) {
					logger.error("failed to get incoming message - "
							+ ex.getMessage());
					if (objectInput != null) {
						try {
							objectInput.close();
						} catch (Exception nestedEx) {
							logger.error("failed to close object input stream - "
									+ nestedEx);
						}
					}
					return null;
				}
				return incomingMessage;
			}
		}

		public void run() {
			while (!initialized) {
				continue;
			}
			logger.info("receiver thread started");

			if (!contactMap.containsKey(localName)) {
				return;
			}

			Contact self = contactMap.get(localName);
			serverSocket = NetTool.createServerSocket(self.IP, self.port,
					logger);

			// failure on creating server socket is a fatal error
			if (serverSocket == null) {
				logger.error("cannot create server socket on " + self.IP + ":"
						+ self.port);
				return;
			}

			while (true) {
				Socket clientSocket = null;
				try {
					clientSocket = serverSocket.accept();
				} catch (IOException ex) {
					logger.error("failed to accept incoming request - "
							+ ex.getMessage());
					continue;
				}

				// spawn a worker thread
				ReceiverWorker rw = new ReceiverWorker(clientSocket);
				Thread rwThread = new Thread(rw);
				rwThread.start();
			}
		}
	}

	public MessagePasser(String configurationFileName, String localName) {
		this.initialized = false;
		this.configurationFileName = configurationFileName;
		this.localName = localName;
		this.logger = new LogTool("ipc.log", MessagePasser.class.getName());
		this.sendBuffer = new LinkedBlockingQueue<Message>();
		this.receiveBuffer = new LinkedBlockingQueue<Message>();
		this.socketMap = new HashMap<String, Socket>();
		this.seqNum = 1;
		this.contactMap = new HashMap<String, Contact>();
		this.type = ClockService.ClockType.DEFAULT;
		this.localNodeId = 0;
		this.rulesLock = new ReentrantLock();
		this.watcher = new Watcher();
		this.sender = new Sender();
		this.receiver = new Receiver();
		this.watcherThread = new Thread(watcher);
		this.senderThread = new Thread(sender);
		this.receiverThread = new Thread(receiver);
		this.watcherThread.start();
		this.senderThread.start();
		this.receiverThread.start();
	}

	/**
	 * Match a message against rules.
	 * 
	 * @param message
	 *            The message to check.
	 * @param rules
	 *            The rules (send or receive) to match.
	 * @return ACTION_DROP, ACTION_DELAY or ACTION_DUPLICATE, null on no match.
	 */
	private String checkRules(Message message,
			ArrayList<HashMap<String, Object>> rules) {
		rulesLock.lock();
		for (HashMap<String, Object> rule : rules) {
			boolean match = true;
			if (match && rule.containsKey(RULE_SRC)
					&& (!rule.get(RULE_SRC).equals(message.getSource()))) {
				match = false;
			}
			if (match && rule.containsKey(RULE_DST)
					&& (!rule.get(RULE_DST).equals(message.getDest()))) {
				match = false;
			}
			if (match && rule.containsKey(RULE_KIND)
					&& (!rule.get(RULE_KIND).equals(message.getKind()))) {
				match = false;
			}
			if (match
					&& rule.containsKey(RULE_SEQ_NUM)
					&& ((Integer) rule.get(RULE_SEQ_NUM)) != message
							.getSequenceNumber()) {
				match = false;
			}
			if (match && rule.containsKey(RULE_DUP)
					&& ((Boolean) rule.get(RULE_DUP) != message.isDupe())) {
				match = false;
			}
			if (match) {
				rulesLock.unlock();
				return (String) rule.get(RULE_ACTION);
			}
		}
		rulesLock.unlock();
		return null;
	}

	/**
	 * Put a message into the send buffer.
	 * 
	 * @param message
	 *            The message to send.
	 * @return The updated local time stamp due to this sending event.
	 */
	public TimeStamp send(Message message) {
		TimeStamp ts = null;
		try {
			if (type != ClockService.ClockType.DEFAULT
					&& message instanceof TimeStampedMessage) {
				ts = ClockService.getInstance().updateLocalTime();
				((TimeStampedMessage) message).setTimeStamp(ts);
			}
			sendBuffer.put(message);
		} catch (InterruptedException ex) {
			logger.info("interrupted when sending message - " + ex.getMessage());
		}
		return ts;
	}

	/**
	 * Take the next message from the receive buffer.
	 * 
	 * @return The next message in the receive buffer.
	 */
	public Message receive() {
		Message message = null;
		try {
			message = receiveBuffer.take();
			if (type != ClockService.ClockType.DEFAULT
					&& message instanceof TimeStampedMessage) {
				TimeStamp ts = ClockService.getInstance().updateLocalTime(
						((TimeStampedMessage) message).getTimeStamp());
				((TimeStampedMessage) message).setTimeStamp(ts);
			}
		} catch (InterruptedException ex) {
			logger.info("interrupted when receiving message - "
					+ ex.getMessage());
		}
		return message;
	}

	public int getNumOfNodes() {
		return contactMap.size();
	}

	public boolean parseConfigurationFinished() {
		return watcher.configurationParsed;
	}

	public void initialize() {
		this.initialized = true;
	}

	public ClockService.ClockType getClockServiceType() {
		return type;
	}

	public int getLocalNodeId() {
		return localNodeId;
	}
}
