
public class MazewarAdminPacket extends MazewarPacket {
	
	public MazewarAdminPacketType packetType;
	public boolean ok;

	public MazewarAdminPacket(MazewarAdminPacketType packetType, int playerID, boolean ok, String msg) {
		this.packetType = packetType;
		this.playerID = playerID;
		this.ok = ok;
		this.msg = msg;
	}
}
