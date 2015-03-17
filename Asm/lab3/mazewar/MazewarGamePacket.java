
public class MazewarGamePacket extends MazewarPacket implements Comparable<MazewarGamePacket> {
	
	public MazewarGamePacketType packetType;
	public long lamport;
	public double extendLamport;
	public double ackLamport;

	public MazewarGamePacket(MazewarGamePacketType packetType, int playerID, long lamport, double ackLamport, Object extraInfo) {
		this.packetType = packetType;
		this.playerID = playerID;
		this.lamport = lamport;
		this.ackLamport = ackLamport;
		this.extraInfo = extraInfo;
		this.extendLamport = lamport + 0.1*playerID;
	}
	
	@Override
	public int compareTo(MazewarGamePacket o) {
		/*int cmp = Double.valueOf(this.lamport).compareTo(Double.valueOf(o.lamport));
		if (cmp == 0)
			return Integer.valueOf(this.playerID).compareTo(Integer.valueOf(o.playerID));
		return cmp;*/
		return Double.valueOf(extendLamport).compareTo(Double.valueOf(extendLamport));
	}
}
