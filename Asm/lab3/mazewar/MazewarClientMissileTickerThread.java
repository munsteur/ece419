

public class MazewarClientMissileTickerThread extends Thread {
	private MazewarClient mazewarClient;

	public MazewarClientMissileTickerThread(MazewarClient mazewarClient) {
		super("MazewarClientMissileTickerThread");
		this.mazewarClient = mazewarClient;
	}

	public void run() {
		System.out.println("Started MazewarClientMissileTickerThread");

		while (mazewarClient.isTicker) {
			try { 
				Thread.sleep(10); // prevent hogging of thread in simple loop
			} catch (InterruptedException e1) {} 
			while (!mazewarClient.isPaused) {
				try {
					Thread.sleep(200); 
					MazewarGamePacket tick = mazewarClient.buildPacket(MazewarGamePacketType.MISSLE_TICK, -1, null);
					mazewarClient.broadcastPacket(tick);
				} catch (InterruptedException e) {}
			}
		}
	}
}
