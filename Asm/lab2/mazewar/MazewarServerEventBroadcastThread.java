import java.net.*;
import java.io.*;

import com.sun.corba.se.impl.naming.pcosnaming.NameServer;

public class MazewarServerEventBroadcastThread extends Thread {
	private Socket[] sockets;
	private MazewarServer mazewarServer;

	public MazewarServerEventBroadcastThread(MazewarServer mazewarServer) {
		super("EchoServerHandlerThread");
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
		
		while (!mazewarServer.isShutDown()) {
			MazewarPacket packet = null;
			int sequenceNumber = mazewarServer.sequenceNumber.incrementAndGet();
			
			if (sequenceNumber == 1) { // first packet
				String msg = "";
				msg += mazewarServer.activePlayers.size();
				for (int i = 0; i < mazewarServer.numPlayers; i++) {
					if (mazewarServer.playerNames[i] != null)
						msg += " " + i + " " + mazewarServer.playerNames[i]; 
				}
				packet = new MazewarPacket(
					MazewarPacketType.GAME_START, 
					-1, 
					sequenceNumber, 
					msg);
			}
			else {
				try {
					packet = mazewarServer.packetQueue.take();
				} 
				catch (InterruptedException e) {}
			}
			for (int i = 0; i < mazewarServer.numPlayers; i++) {
				if (mazewarServer.activePlayers.contains(i)) {
					try {
						packet.playerID = i;
						toClient[i].writeObject(packet);
					}
					catch (IOException e) {
						System.err.println("ERROR: Could not broadcast to player " + i);
					}
				}
			}
			
		}


		for (int i = 0; i < mazewarServer.numPlayers; i++) {
			try {
				//if (mazewarServer.activePlayers.contains(i)) {
				toClient[i].close();
				sockets[i].close();
				//}
			}
			catch (IOException e) {
				System.err.println("ERROR: Could not close broadcast connection to player " + i);
			}

		}


	}
}
