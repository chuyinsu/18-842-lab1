package clock;

import java.io.Serializable;
import java.util.Arrays;

/**
 * This class defines a time stamp. It is designed to be operated only by the
 * ClockService instances to hide the complexity.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class TimeStamp implements Serializable, Comparable<TimeStamp> {

	// DEFAULT - cannot determine the relationship
	// e.g. when the clock service type is invalid
	public enum RelationShip {
		BEFORE, AFTER, CONCURRENT, SAME, DEFAULT
	}

	private static final long serialVersionUID = -7491707524322400477L;

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

	/**
	 * Compare two time stamps, applicable to both Logical and Vector class.
	 * 
	 * @param ts
	 *            The time stamp to compare to.
	 * @return The comparison result.
	 */
	public RelationShip compare(TimeStamp ts) {
		if (type == ClockService.ClockType.LOGICAL) {
			if (this.logical < ts.logical) {
				return RelationShip.BEFORE;
			} else if (this.logical > ts.logical) {
				return RelationShip.AFTER;
			} else {
				return RelationShip.CONCURRENT;
			}
		} else if (type == ClockService.ClockType.VECTOR) {
			boolean biggerThan = false;
			boolean smallerThan = false;
			for (int i = 0; i < this.vector.length; i++) {
				if ((this.vector)[i] > (ts.vector)[i]) {
					biggerThan = true;
				} else if ((this.vector)[i] < (ts.vector)[i]) {
					smallerThan = true;
				}
			}
			if (biggerThan && (!smallerThan)) {
				return RelationShip.AFTER;
			} else if ((!biggerThan) && smallerThan) {
				return RelationShip.BEFORE;
			} else if ((!biggerThan) && (!smallerThan)) {
				return RelationShip.SAME;
			} else {
				return RelationShip.CONCURRENT;
			}
		} else {
			return RelationShip.DEFAULT;
		}
	}

	@Override
	public int compareTo(TimeStamp ts) {
		if (type == ClockService.ClockType.LOGICAL) {
			return this.logical - ts.logical;
		} else if (type == ClockService.ClockType.VECTOR) {
			boolean biggerThan = false;
			boolean smallerThan = false;
			for (int i = 0; i < this.vector.length; i++) {
				if ((this.vector)[i] > (ts.vector)[i]) {
					biggerThan = true;
				} else if ((this.vector)[i] < (ts.vector)[i]) {
					smallerThan = true;
				}
			}
			if (biggerThan && (!smallerThan)) {
				return 1;
			} else if ((!biggerThan) && smallerThan) {
				return -1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
}
