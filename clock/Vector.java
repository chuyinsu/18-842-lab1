package clock;

public class Vector extends ClockService {

	/*
	 * (non-Javadoc)
	 * 
	 * @see clock.ClockService#updateLocalTime(clock.TimeStamp)
	 */
	public TimeStamp updateLocalTime(TimeStamp newTime) {
		TimeStamp timeStamp = null;
		getLocalTimeLock();
		TimeStamp localTime = getLocalTimeRef();
		for (int i = 0; i < localTime.getVector().length; i++) {
			(localTime.getVector())[i] = Math.max((localTime.getVector())[i],
					(newTime.getVector())[i]);
		}
		localTime.advance();
		timeStamp = new TimeStamp(localTime);
		releaseLocalTimeLock();
		return timeStamp;
	}
}
