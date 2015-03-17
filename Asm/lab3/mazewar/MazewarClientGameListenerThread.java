import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.io.*;


public class MazewarClientGameListenerThread extends Thread {
	private Socket socket = null;
	private MazewarClient mazewarClient;
	private int playerID;

	public MazewarClientGameListenerThread(MazewarClient mazewarClient, int playerID, Socket socket) {
		super("MazewarClientGameListenerThread");
		this.mazewarClient = mazewarClient;
		this.playerID = playerID;
		this.socket = socket;
	}

	public void run() {
		System.out.println("Started MazewarClientGameListenerThread");

		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(socket.getInputStream());
		} 
		catch (IOException e) {
			System.err.println("ERROR: Could not open input stream to player " + playerID);
		}

		while (!mazewarClient.playerShutdown.containsKey(playerID) && !mazewarClient.isShutDown()) {
			MazewarGamePacket packet = null;
			try {
				packet = (MazewarGamePacket) ois.readObject();
			}
			catch (IOException | ClassNotFoundException e) {
				System.err.println("ERROR: Could not read packets from player " + playerID);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {}
			}
			
			if (packet != null) {
				if (packet.packetType == MazewarGamePacketType.ACK) {
					double msgLamport = (double)packet.ackLamport;
					if (!mazewarClient.acks.containsKey(msgLamport)) {
						mazewarClient.acks.put(msgLamport, new ConcurrentHashMap<Integer, MazewarGamePacket>());
					}
					mazewarClient.acks.get(msgLamport).put(playerID, packet);
	
					// increment lamport
					mazewarClient.lamport.set(Math.max(mazewarClient.lamport.incrementAndGet(), packet.lamport+1));
	
					System.out.println("Received player " + packet.playerID + " " + packet.packetType + " event");
				}
				else {
					if (packet.packetType == MazewarGamePacketType.JOIN) { // TODO: do for remove
						
						// TODO: stop keyboard input
						mazewarClient.isPaused = true;
						
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {}
						
						playerID = packet.playerID;
						mazewarClient.playerShutdown.remove(packet.playerID);
						mazewarClient.gameSenderQueues.put(playerID, new PriorityBlockingQueue<MazewarGamePacket>());
						(new MazewarClientGameSenderThread(mazewarClient, playerID, socket)).start();
						
						
	
						
					}
					
	
					// implicit ack from msg sender
					double msgLamport = packet.extendLamport;
					if (!mazewarClient.acks.containsKey(packet.extendLamport)) {
						mazewarClient.acks.put(msgLamport, new ConcurrentHashMap<Integer, MazewarGamePacket>());
					}
					mazewarClient.acks.get(msgLamport).put(playerID, packet);
	
					// increment lamport
					mazewarClient.lamport.set(Math.max(mazewarClient.lamport.incrementAndGet(), packet.lamport+1));
	
					// send ACK to all other clients
					for (PriorityBlockingQueue<MazewarGamePacket> queue : mazewarClient.gameSenderQueues.values()) {
						if (packet.packetType == MazewarGamePacketType.JOIN) {
							queue.add(mazewarClient.buildPacket(MazewarGamePacketType.ACK, msgLamport, 
									new String[] {
										""+mazewarClient.guiClient.getScore(),
										""+mazewarClient.guiClient.getPoint().getX(),
										""+mazewarClient.guiClient.getPoint().getY(),
										""+mazewarClient.guiClient.getOrientation().toVal(),
										mazewarClient.guiClient.getName()
									}
							));
						}
						else {
							queue.add(mazewarClient.buildPacket(MazewarGamePacketType.ACK, msgLamport, null));
						}
					}
	
					mazewarClient.gameListenerQueue.add(packet);
					
	
					System.out.println("Received player " + packet.playerID + " " + packet.packetType + " event");
	
				}
			}

		}



		try {
			if (ois != null)
				ois.close();
			socket.close();
		}
		catch (IOException e) {
			System.err.println("ERROR: Could not close listener connection to player " + playerID);
		}

		System.out.println("Exitted MazewarClientGameListenerThread");

	}
}
