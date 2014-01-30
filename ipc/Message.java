package ipc;

import java.io.Serializable;

/**
 * 
 * This class holds all necessary information that needs to be transmitted to
 * the destination.
 * 
 * @author Jason Xi
 * @author Yinsu Chu
 * 
 */
public class Message implements Serializable {
	private static final long serialVersionUID = -7606425083609686258L;
	private String dest;
	private String kind;
	private Object data;
	private String source;
	private int sequenceNumber;
	private boolean dupe;

	public Message(String dest, String kind, Object data) {
		this.dest = dest;
		this.kind = kind;
		this.data = data;
	}

	@Override
	public String toString() {
		return "[src] " + source + " [dst] " + dest + " [kind] " + kind
				+ " [seq] " + sequenceNumber + " [dup] "
				+ (dupe ? "true" : "false") + " [data] " + data.toString();
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public boolean isDupe() {
		return dupe;
	}

	public void setDupe(boolean dupe) {
		this.dupe = dupe;
	}

}
