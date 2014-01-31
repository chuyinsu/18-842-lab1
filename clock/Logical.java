package clock;

/**
 * The logical clock service.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class Logical extends ClockService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see clock.ClockService#updateLocalTime(clock.TimeStamp)
	 */
	public TimeStamp updateLocalTime(TimeStamp newTime) {
		TimeStamp timeStamp = null;
		getLocalTimeLock();
		getLocalTime().setLogical(
				(Math.max(getLocalTime().getLogical(), newTime.getLogical())));
		getLocalTime().advance();
		timeStamp = new TimeStamp(getLocalTime());
		releaseLocalTimeLock();
		return timeStamp;
	}
}
