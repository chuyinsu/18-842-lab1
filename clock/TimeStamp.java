package clock;

import java.util.Arrays;

/**
 * This class defines a time stamp. It is designed to be operated only by the
 * ClockService instances to hide the complexity.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class TimeStamp {
	private ClockService.ClockType type;
	private int localNodeId;
	private int logical;
	private int[] vector;

	/**
	 * Instantiate a new time stamp.
	 * 
	 * @param dimension
	 *            The dimension of the vector clock.
	 * @param type
	 *            The type of the clock service.
	 * @param localNodeId
	 *            The id of the local node, which is used as the index into the
	 *            vector clock.
	 */
	protected TimeStamp(int dimension, ClockService.ClockType type,
			int localNodeId) {
		this.logical = 0;
		this.vector = new int[dimension];
		Arrays.fill(vector, 0);
		this.type = type;
		this.localNodeId = localNodeId;
	}

	/**
	 * Instantiate using the given time stamp.
	 * 
	 * @param timeStamp
	 *            The time stamp to copy over.
	 */
	protected TimeStamp(TimeStamp timeStamp) {
		this.type = timeStamp.type;
		this.localNodeId = timeStamp.localNodeId;
		this.logical = timeStamp.logical;
		this.vector = new int[timeStamp.vector.length];
		for (int i = 0; i < this.vector.length; i++) {
			(this.vector)[i] = (timeStamp.vector)[i];
		}
	}

	/**
	 * Advance the time stamp according to the given type. Most of the time a
	 * time stamp should remain static (such a time stamp in a received
	 * message), this method should only be used to update the local time stamp
	 * when needed.
	 * 
	 * @param type
	 *            The type of the clock service.
	 * @param index
	 *            The index of the vector clock.
	 */
	protected void advance() {
		if (type == ClockService.ClockType.LOGICAL) {
			logical += ClockService.STEP;
		} else if (type == ClockService.ClockType.VECTOR) {
			vector[localNodeId] += ClockService.STEP;
		}
	}

	@Override
	public String toString() {
		if (type == ClockService.ClockType.LOGICAL) {
			return String.valueOf(logical);
		} else {
			return Arrays.toString(vector);
		}
	}

	protected int getLogical() {
		return logical;
	}

	protected void setLogical(int logical) {
		this.logical = logical;
	}

	protected int[] getVector() {
		return vector;
	}

	protected void setVector(int[] vector) {
		this.vector = vector;
	}
}
