package tcp_server;

public class Robot {
    private String name;
    private Integer robotHashName;
    private Integer robotHashServerKey;
    private Integer robotHashClientKey;
    private Coords coords;
    private Orientation orientation;
    private RobotStatus status;
    private Turn avoidTurn;
    private boolean lastTurnWasStraight;


    public Robot(String name) {
        this.name = name;
        this.robotHashName = null;
        this.robotHashServerKey = null;
        this.robotHashClientKey = null;
        this.coords = null;
        this.status = RobotStatus.FREE;
        this.avoidTurn = null;
        this.lastTurnWasStraight=false;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RobotStatus getStatus() {
        return status;
    }

    public void setStatus(RobotStatus status) {
        this.status = status;
    }

    public Turn getAvoidTurn() {
        return avoidTurn;
    }

    public void setAvoidTurn(Turn avoidTurn) {
        this.avoidTurn = avoidTurn;
    }

    public boolean lastTurnWasStraight() {
        return lastTurnWasStraight;
    }

    public void setLastTurnWasStraight(boolean lastTurnWasStraight) {
        this.lastTurnWasStraight = lastTurnWasStraight;
    }

    public Integer getRobotHashName() {
        return robotHashName;
    }

    public void setRobotHashName(Integer nameSum) {
        this.robotHashName = (nameSum * 1000) % 65536;
    }

    public Integer getRobotHashServerKey() {
        return robotHashServerKey;
    }

    public void setRobotHashServerKey(Integer serverKey) {
        this.robotHashServerKey = (robotHashName + serverKey) % 65536;
    }

    public Integer getRobotHashClientKey() {
        return robotHashClientKey;
    }

    public void setRobotHashClientKey(Integer clientKey) {
        this.robotHashClientKey = (robotHashName + clientKey) % 65536;
    }

    public Coords getCoords() {
        return coords;
    }

    public void setCoords(Coords coords) {
        this.coords = coords;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public Orientation getOrientation(Coords nextStepCoords) {
        if (getCoords().x > nextStepCoords.x)
            return Orientation.LEFT;
        if (getCoords().x < nextStepCoords.x)
            return Orientation.RIGHT;
        if (getCoords().y > nextStepCoords.y)
            return Orientation.DOWN;
        if (getCoords().y < nextStepCoords.y)
            return Orientation.UP;
        return Orientation.UNKNOWN1;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    public boolean stuck(Coords nextStepCoords) {
        return (getCoords().x == nextStepCoords.x && getCoords().y == nextStepCoords.y && lastTurnWasStraight);
    }

    public Turn calculateBestRotation() {
        int robotQuadrant = getQuadrant();
        switch (robotQuadrant) {
            case 0:
                return calcMoveFor0Q();
            case 1:
                return calcMoveFor1Q();
            case 2:
                return calcMoveFor2Q();
            case 3:
                return calcMoveFor3Q();
            case 4:
                return calcMoveFor4Q();
        }
        return Turn.RIGHT;
    }

    public boolean needRotation(){
        if(getCoords().x > 0){
            return getOrientation() != Orientation.LEFT;
        }
        if(getCoords().x < 0){
            return getOrientation() != Orientation.RIGHT;
        }
        if(getCoords().y > 0){
            return getOrientation() != Orientation.DOWN;
        }
        if(getCoords().y < 0){
            return getOrientation() != Orientation.UP;
        }
        return true;
    }

    public void rotate(){
        setOrientation(getOrientation().next());
    }

    private int getQuadrant() {
        if (getCoords().x < 0 && getCoords().y > 0)
            return 1;
        if (getCoords().x > 0 && getCoords().y > 0)
            return 2;
        if (getCoords().x < 0 && getCoords().y < 0)
            return 3;
        if (getCoords().x > 0 && getCoords().y < 0)
            return 4;
        return 0;
    }

    private Turn calcMoveFor0Q() {
        return Turn.RIGHT;
    }

    private Turn calcMoveFor1Q() {
        return switch (getOrientation()) {
            case UNKNOWN1, UNKNOWN2, UNKNOWN3, UP, RIGHT -> Turn.RIGHT;
            case LEFT, DOWN -> Turn.LEFT;
        };
    }

    private Turn calcMoveFor2Q() {
        return switch (getOrientation()) {
            case UNKNOWN1, UNKNOWN2, UNKNOWN3, RIGHT, DOWN -> Turn.RIGHT;
            case UP, LEFT -> Turn.LEFT;

        };
    }

    private Turn calcMoveFor3Q() {
        return switch (getOrientation()) {
            case UNKNOWN1, UNKNOWN2, UNKNOWN3, UP, LEFT -> Turn.RIGHT;
            case RIGHT, DOWN -> Turn.LEFT;
        };
    }

    private Turn calcMoveFor4Q() {
        return switch (getOrientation()) {
            case UNKNOWN1, UNKNOWN2, UNKNOWN3, LEFT, DOWN -> Turn.RIGHT;
            case UP, RIGHT -> Turn.LEFT;
        };
    }
}
