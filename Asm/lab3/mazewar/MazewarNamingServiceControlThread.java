import java.util.Scanner;

public class MazewarNamingServiceControlThread extends Thread {
	
	MazewarNamingService namingService;
	
	public MazewarNamingServiceControlThread(MazewarNamingService namingService) {
		super("MazewarNamingServiceControlThread");
		this.namingService = namingService;
	}
	
	public void run() {
		System.out.println("Started MazewarNamingServiceControlThread");
		
		Scanner sc = new Scanner(System.in);
		String input = "";
		while (!input.equalsIgnoreCase("q"))
			input = sc.nextLine();
		sc.close();
		namingService.shutDown();
	}
}
