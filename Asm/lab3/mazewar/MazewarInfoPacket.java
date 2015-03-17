
public class MazewarInfoPacket extends MazewarPacket {
	
	public MazewarInfoPacketType packetType;
	public boolean ok;

	public MazewarInfoPacket(MazewarInfoPacketType packetType, int playerID, boolean ok, Object extraInfo) {
		this.packetType = packetType;
		this.playerID = playerID;
		this.ok = ok;
		this.extraInfo = extraInfo;
	}
}
