package ipc;

import clock.TimeStamp;

public class TimeStampedMessage extends Message {
	private static final long serialVersionUID = -5908777442179653889L;
	private TimeStamp timeStamp;

	public TimeStampedMessage(String dest, String kind, Object data,
			TimeStamp timeStamp) {
		super(dest, kind, data);
		this.timeStamp = timeStamp;
	}

	public TimeStamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(TimeStamp timeStamp) {
		this.timeStamp = timeStamp;
	}
}
