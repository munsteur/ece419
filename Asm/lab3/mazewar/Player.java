import java.io.Serializable;


public class Player implements Serializable {
	public String host;
	public int port;
	public int id;
	public String name;
	
	public Player(String host, int port, int id, String name) {
		this.host = host;
		this.port = port;
		this.id = id;
		this.name = name;
	}
}
