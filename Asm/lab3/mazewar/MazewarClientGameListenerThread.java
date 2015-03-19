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
			synchronized( this ) {
				try {
					packet = (MazewarGamePacket) ois.readObject();
				}
				catch (IOException | ClassNotFoundException e) {
					System.err.println("ERROR: Could not read packets from player " + playerID);
					try {
						Thread.sleep(50);
					} catch (InterruptedException e1) {}
				}
				
				if (packet != null) {
					if (packet.packetType == MazewarGamePacketType.ACK) {
						double msgLamport = (double)packet.ackLamport;
						mazewarClient.acks.putIfAbsent(msgLamport, new ConcurrentHashMap<Integer, MazewarGamePacket>());
						mazewarClient.acks.get(msgLamport).put(playerID, packet);
		
						// increment lamport
						mazewarClient.lamport.set(Math.max(mazewarClient.lamport.incrementAndGet(), packet.lamport+1));
		
						System.out.println(packet.extendLamport + ": Received player " + packet.playerID + " ACK event for " + msgLamport);
					}
					else {
						if (packet.packetType == MazewarGamePacketType.JOIN) { // TODO: do for remove
							
							// TODO: stop keyboard input
							mazewarClient.isPaused = true;
							
							while (!mazewarClient.gameListenerQueue.isEmpty()) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {}
							}
							
							playerID = packet.playerID;
							mazewarClient.players.put(playerID, new Player("", -1, playerID, ""));
							mazewarClient.playerShutdown.remove(packet.playerID);
							mazewarClient.gameSenderQueues.put(playerID, new PriorityBlockingQueue<MazewarGamePacket>());
							(new MazewarClientGameSenderThread(mazewarClient, playerID, socket)).start();
							
							// implicit ack from msg sender for JOIN only
							double msgLamport = packet.extendLamport;
							mazewarClient.acks.putIfAbsent(msgLamport, new ConcurrentHashMap<Integer, MazewarGamePacket>());
							mazewarClient.acks.get(msgLamport).put(playerID, packet);
		
						}
						
						mazewarClient.gameListenerQueue.add(packet);
						
						System.out.println(packet.extendLamport + ": Received player " + packet.playerID + " " + packet.packetType + " event");
		
					}
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
