package tcp_server;

public enum Orientation {
    UP,
    RIGHT,
    DOWN,
    LEFT,
    UNKNOWN1,
    UNKNOWN2,
    UNKNOWN3;

    public Orientation next() {
        return values()[ordinal()] == LEFT ? UP : values()[ordinal() + 1];
    }
}
