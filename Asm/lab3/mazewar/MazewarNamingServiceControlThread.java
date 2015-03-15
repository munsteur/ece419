import java.util.Scanner;

public class MazewarNamingServiceControlThread extends Thread {
	
	MazewarNamingService namingService;
	
	public MazewarNamingServiceControlThread(MazewarNamingService namingService) {
		super("MazewarNamingServiceControlThread");
		this.namingService = namingService;
		System.out.println("Starting MazewarNamingServiceControlThread");
	}
	
	public void run() {
		Scanner sc = new Scanner(System.in);
		String input = "";
		while (!input.equalsIgnoreCase("q"))
			input = sc.nextLine();
		sc.close();
		namingService.shutDown();
	}
}
