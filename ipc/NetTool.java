package ipc;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

/**
 * Helper methods to create and destroy sockets and server sockets. They are
 * from Yinsu Chu and Ming Zhong's project of 15-440/640 in Fall 2013.
 * 
 * @author Ming Zhong
 * @author Yinsu Chu
 * 
 */
public class NetTool {

	/**
	 * Create a server socket.
	 * 
	 * @param IP
	 *            The address to bind.
	 * @param port
	 *            Port to listen on.
	 * @param logger
	 *            Logger of the calling method.
	 * @return A new server socket, null on failure.
	 */
	public static ServerSocket createServerSocket(String IP, int port,
			Logger logger) {
		ServerSocket socket = null;
		InetSocketAddress address = new InetSocketAddress(IP, port);
		try {
			socket = new ServerSocket();
			socket.bind(address);
		} catch (Exception ex) {
			logger.error("failed to create server socket on " + IP + ":" + port
					+ " - " + ex.getMessage());
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception nestedEx) {
					logger.error("failed to close server socket on " + IP + ":"
							+ port + " - " + nestedEx.getMessage());
				}
			}
			return null;
		}
		return socket;
	}

	/**
	 * Create socket with remote host.
	 * 
	 * @param IP
	 *            IP address of the remote host.
	 * @param port
	 *            Port number to create the socket to.
	 * @param logger
	 *            Logger of the calling method.
	 * @return The socket to the remote host, null on failure.
	 */
	public static Socket createSocket(String IP, int port, Logger logger) {
		Socket socket = null;
		try {
			socket = new Socket(IP, port);
		} catch (Exception ex) {
			logger.error("failed to create socket to " + IP + ":" + port
					+ " - " + ex.getMessage());
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception nestedEx) {
					logger.error("failed to close socket to " + IP + ":" + port
							+ " - " + nestedEx.getMessage());
				}
			}
			return null;
		}
		return socket;
	}

	/**
	 * Destroy the server socket.
	 * 
	 * @param socket
	 *            The serverSocket to destroy.
	 * @param logger
	 *            Logger of the calling method.
	 */
	public static void destroyServerSocket(ServerSocket socket, Logger logger) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (Exception ex) {
			logger.error("failed to close server socket - " + ex.getMessage());
		}
	}

	/**
	 * Destroy a socket.
	 * 
	 * @param socket
	 *            The socket to destroy.
	 * @param logger
	 *            Logger of the calling method.
	 */
	public static void destroySocket(Socket socket, Logger logger) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (Exception ex) {
			logger.error("failed to close socket - " + ex.getMessage());
		}
	}
}
