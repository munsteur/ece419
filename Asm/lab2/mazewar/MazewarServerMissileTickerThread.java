
public class MazewarServerMissileTickerThread extends Thread {
	private MazewarServer mazewarServer;

	public MazewarServerMissileTickerThread(MazewarServer mazewarServer) {
		super("MazewarServerMissileTickerThread");
		this.mazewarServer = mazewarServer;
		System.out.println("Created MazewarServerMissileTickerThread");
	}

	public void run() {
		while (!mazewarServer.isShutDown()) {
			try {
				Thread.sleep(200); 
				mazewarServer.packetQueue.add(new MazewarPacket(
								MazewarPacketType.MISSLE_TICK, 
								-1, 
								mazewarServer.sequenceNumber.getAndIncrement(),
								null));
			} catch (InterruptedException e) {
				
			}
		}
	}
}
