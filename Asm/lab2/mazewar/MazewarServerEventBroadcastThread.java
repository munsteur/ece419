import java.net.*;
import java.io.*;

public class MazewarServerEventBroadcastThread extends Thread {
	private Socket[] sockets;
	private MazewarServer mazewarServer;

	public MazewarServerEventBroadcastThread(MazewarServer mazewarServer) {
		super("MazewarServerEventBroadcastThread");
		this.mazewarServer = mazewarServer;
		this.sockets = mazewarServer.clientSockets;
		System.out.println("Created MazewarServerEventBroadcastThread");
	}

	public void run() {

		ObjectOutputStream[] toClient = new ObjectOutputStream[mazewarServer.numPlayers];
		
		for (int i = 0; i < mazewarServer.numPlayers; i++) {
			if (mazewarServer.activePlayers.contains(i)) {
				try {
					toClient[i] = new ObjectOutputStream(sockets[i].getOutputStream());
				} 
				catch (IOException e) {
					System.err.println("ERROR: Could not connect to player " + i);
					mazewarServer.activePlayers.remove(i);
				}
			}
		}
		
//		try {
//			Thread.sleep(1000);
//		}
//		catch (InterruptedException e) {}
		
		boolean quitReceived = false;
		
		while (!quitReceived) {
			MazewarPacket packet = null;
			
			if (mazewarServer.sequenceNumber.get() == 0) { // GAME_START packet
				String msg = "";
				msg += mazewarServer.activePlayers.size();
				for (int i = 0; i < mazewarServer.numPlayers; i++) {
					if (mazewarServer.playerNames[i] != null)
						msg += " " + i + " " + mazewarServer.playerNames[i]; 
				}
				packet = new MazewarPacket(
					MazewarPacketType.GAME_START, 
					-1, 
					mazewarServer.sequenceNumber.get(), 
					msg);
				mazewarServer.sequenceNumber.incrementAndGet();
				
				for (int i = 0; i < mazewarServer.numPlayers; i++) {
					if (mazewarServer.activePlayers.contains(i)) {
						try {
							packet.playerID = i;
							toClient[i].writeObject(packet);
						}
						catch (IOException e) {
							System.err.println("ERROR: Could not send GAME_START packet to player " + i);
						}
					}
				}
			}
			else {
				try {
					packet = mazewarServer.packetQueue.take();
				} 
				catch (InterruptedException e) {}
				
				for (int i = 0; i < mazewarServer.numPlayers; i++) {
					if (mazewarServer.activePlayers.contains(i)) {
						try {
							toClient[i].writeObject(packet);
						}
						catch (IOException e) {
							System.err.println("ERROR: Could not broadcast to player " + i);
						}
					}
				}
				
				if (packet.packetType == MazewarPacketType.QUIT)
					quitReceived = true;
			}
			
			
		}


		for (int i = 0; i < mazewarServer.numPlayers; i++) {
			try {
				//if (mazewarServer.activePlayers.contains(i)) {
				if (toClient[i] != null)
					toClient[i].close();
				if (sockets[i] != null)
					sockets[i].close();
				//}
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not close broadcast connection to player " + i);
			}

		}


	}
}
