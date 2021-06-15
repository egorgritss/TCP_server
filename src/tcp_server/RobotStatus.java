package tcp_server;

public enum RobotStatus {
    FREE,
    STUCK,
    ROTATED_TO_AVOID_STUCK,
    MOVED_TO_AVOID_STUCK,
    ROTATED_AFTER_MOVE,
}
