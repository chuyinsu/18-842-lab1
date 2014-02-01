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
		getLocalTimeRef()
				.setLogical(
						(Math.max(getLocalTimeRef().getLogical(),
								newTime.getLogical())));
		getLocalTimeRef().advance();
		timeStamp = new TimeStamp(getLocalTimeRef());
		releaseLocalTimeLock();
		return timeStamp;
	}
}
