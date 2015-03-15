
public class MazewarGamePacket extends MazewarPacket implements Comparable<MazewarGamePacket> {
	
	public MazewarGamePacketType packetType;
	public double lamport;

	public MazewarGamePacket(MazewarGamePacketType packetType, int playerID, double lamport, String msg) {
		this.packetType = packetType;
		this.playerID = playerID;
		this.lamport = lamport;
		this.msg = msg;
	}
	
	@Override
	public int compareTo(MazewarGamePacket o) {
		return Double.valueOf(this.lamport).compareTo(Double.valueOf(o.lamport));
	}
}
