import java.io.Serializable;

public class MazewarPacket implements Serializable, Comparable<MazewarPacket> {
	
	public MazewarPacketType packetType;
	public int playerID;
	public int sequenceNumber;
	public String msg;

	public MazewarPacket(MazewarPacketType packetType, int playerID, int sequenceNumber, String msg) {
		this.packetType = packetType;
		this.playerID = playerID;
		this.sequenceNumber = sequenceNumber;
		this.msg = msg;
	}
	
	public MazewarPacketType getPacketType() {
		return packetType;
	}

	@Override
	public int compareTo(MazewarPacket o) {
		return Integer.compare(this.sequenceNumber, o.sequenceNumber);
	}
}
