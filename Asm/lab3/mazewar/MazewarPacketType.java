
public enum MazewarPacketType {
	// game related, sent by server
	GAME_START,
	GAME_OVER,
	MISSLE_TICK,
	// player related, sent by client
	PLAYER_NAME,
	QUIT,
	GO_FORWARD,
	GO_BACKWARD,
	TURN_LEFT,
	TURN_RIGHT,
	FIRE;
}
