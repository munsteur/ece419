import java.net.*;
import java.io.*;

public class MazewarClientGameSenderThread extends Thread {
	private Socket socket = null;
	private MazewarClient mazewarClient;
	private int dstPlayerID;

	public MazewarClientGameSenderThread(MazewarClient mazewarClient, int dstPlayerID, Socket socket) {
		super("MazewarClientGameSenderThread");
		this.mazewarClient = mazewarClient;
		this.dstPlayerID = dstPlayerID;
		this.socket = socket;

	}

	public void run() {

		System.out.println("Started MazewarClientGameSenderThread");
		
		try {
			Thread.sleep(500);
		} 
		catch (InterruptedException e) {}

		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not connect to player " + dstPlayerID);
		}

		while (!mazewarClient.playerShutdown.containsKey(dstPlayerID) && !mazewarClient.isShutDown) {

			try {
				Thread.sleep(10);
			} 
			catch (InterruptedException e) {}

			try {
				if (!mazewarClient.gameSenderQueues.get(dstPlayerID).isEmpty()) {
					MazewarGamePacket packet = mazewarClient.gameSenderQueues.get(dstPlayerID).poll();
					oos.writeObject(packet);
					if (packet.packetType == MazewarGamePacketType.ACK)
						System.out.println("Sent to player " + dstPlayerID + " " + packet.packetType + " for " + packet.ackLamport);
					else
						System.out.println("Sent to player " + dstPlayerID + " " + packet.packetType + " event " + packet.extendLamport);
				}
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not send packets to player " + dstPlayerID);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {}
			}

		}

		try {
			if (oos != null)
				oos.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close connection to player " + dstPlayerID);
		}

		System.out.println("Exitted MazewarClientGameSenderThread");

	}
}
