package clock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * This class defines the clock service while hiding the details. It is the base
 * class of Logical and Vector which are the two types of service in our system.
 * The main job of this class is to keep track of local time stamp and provide
 * access to the instance using singleton design pattern.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public abstract class ClockService {

	// the increment step of clock
	public static final int STEP = 1;

	// protects and keeps track of local time stamp
	private static ReentrantLock localTimeLock;
	private static TimeStamp localTime;

	// singleton design pattern
	private static ClockService instance;

	// dimension of the vector clock
	private static int dimension;

	// type of the clock service
	private static ClockType type;

	// the id of the local node (used as the index into the vector clock)
	private static int localNodeId;

	// DEFAULT - no clock service
	public enum ClockType {
		LOGICAL, VECTOR, DEFAULT
	}

	/**
	 * This method must be called before using.
	 * 
	 * @param dimension
	 *            The dimension of the vector clock, i.e. the number of nodes in
	 *            the distributed system.
	 * @param type
	 *            The type of this clock service.
	 * @param localNodeId
	 *            The id of the local node, which is used as the index into the
	 *            vector clock.
	 */
	public static void initialize(int d, ClockType t, int id) {
		localTimeLock = null;
		localTime = null;
		instance = null;
		dimension = d;
		type = t;
		localNodeId = id;
	}

	protected void getLocalTimeLock() {
		localTimeLock.lock();
	}

	protected void releaseLocalTimeLock() {
		localTimeLock.unlock();
	}

	public TimeStamp getLocalTime() {
		TimeStamp timeStamp = null;
		localTimeLock.lock();
		timeStamp = new TimeStamp(localTime);
		localTimeLock.unlock();
		return timeStamp;
	}

	public static ClockService getInstance() {
		if (instance == null) {
			localTimeLock = new ReentrantLock();
			localTime = new TimeStamp(dimension, type, localNodeId);
			if (type == ClockType.LOGICAL) {
				instance = new Logical();
			} else if (type == ClockType.VECTOR) {
				instance = new Vector();
			}
		}
		return instance;
	}

	/**
	 * Advance the local time stamp by the step defined in ClockService and
	 * return the updated time stamp.
	 * 
	 * @return The updated time stamp;
	 */
	public TimeStamp updateLocalTime() {
		TimeStamp timeStamp = null;
		localTimeLock.lock();
		localTime.advance();
		timeStamp = new TimeStamp(localTime);
		localTimeLock.unlock();
		return timeStamp;
	}

	/**
	 * Update the local time stamp by comparing to the given time stamp. If the
	 * given time stamp is newer than the local one, the local one will be
	 * updated. Typically this is used when receiving a message.
	 * 
	 * @param newTime
	 *            The time stamp to compare with.
	 * @return The updated time stamp.
	 */
	public abstract TimeStamp updateLocalTime(TimeStamp newTime);
}
