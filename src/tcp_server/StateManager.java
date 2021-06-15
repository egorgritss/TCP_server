package tcp_server;

import java.util.Optional;

public class StateManager {

    private static final String SERVER_MOVE = "102 MOVE\u0007\b";
    private static final String SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b";
    private static final String SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b";
    private static final String SERVER_PICK_UP = "105 GET MESSAGE\u0007\b";
    private static final String SERVER_LOGOUT = "106 LOGOUT\u0007\b";
    private static final String SERVER_KEY_REQUEST = "107 KEY REQUEST\u0007\b";
    private static final String SERVER_OK = "200 OK\u0007\b";
    private static final String SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b";
    private static final String SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\b";
    private static final String SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b";
    private static final String SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE\u0007\b";
    private static final String CLIENT_RECHARGING = "RECHARGING";
    private static final String CLIENT_FULL_POWER = "FULL POWER";

    private static final Integer TIMEOUT = 1;
    private static final Integer TIMEOUT_RECHARGING = 5;

    private Boolean onCharge;
    private StringBuffer buffer;
    private CommunicationState communicationState;
    private Robot robot;

    StateManager() {
        this.communicationState = CommunicationState.RECEIVING_NAME;
        this.onCharge = false;
        this.buffer = new StringBuffer("");
        this.robot = null;
    }


    public Integer getTimeout() {
        return 1000 * (onCharge ? TIMEOUT_RECHARGING : TIMEOUT);
    }

    public Response getResponse(String msg) {

        msg = buffer.append(msg).toString();
        buffer.delete(0, buffer.length());

        if(!messageValidate(msg)){
            return new Response(SERVER_SYNTAX_ERROR, true);
        }

        if (onCharge) {
            if (!msg.equals(CLIENT_FULL_POWER)){
                return new Response(SERVER_LOGIC_ERROR, true);
            }else {
                onCharge = false;
                return new Response("");
            }
        }
        if (msg.equals(CLIENT_RECHARGING)) {
            onCharge = true;
            return new Response("");
        }

        switch (communicationState) {
            case RECEIVING_NAME:
                return createRobotName(msg);
            case RECEIVING_KEY_ID:
                return checkKeyId(msg);
            case RECEIVING_CLIENT_CONFIRMATION:
                return checkClientConfirmation(msg);
            case RECEIVING_COORDS1:
            case RECEIVING_COORDS2:
            case INIT_COORDS_RECEIVED:
            case NAVIGATING:
                return setRobotCoords(msg);
            case ARRIVED:
                return new Response(SERVER_LOGOUT, true);
        }

        return new Response(Response.EMPTY);
    }

    private boolean messageValidate(String msg) {
        if (!messageLengthIsValid(msg.length()) && !msg.equals(CLIENT_RECHARGING) && !msg.equals(CLIENT_FULL_POWER)) {
            System.out.println("Returned Syntax Error\n");
            return false;
        }
        return true;
    }

    public boolean isInputValid(String msg){
        if(CLIENT_RECHARGING.contains(msg) || CLIENT_FULL_POWER.contains(msg))
            return true;
        return messageLengthIsValid(msg.length());
    }

    public Response checkIfMessageValid(String in) {
        if (messageLengthIsValid(buffer.length() + in.length())) {
            buffer.append(in);
            return new Response(Response.EMPTY);
        }
        return new Response(SERVER_SYNTAX_ERROR, true);
    }

    public boolean messageLengthIsValid(int len) {

        return switch (communicationState) {
            case RECEIVING_NAME -> len <= 18;
            case RECEIVING_KEY_ID -> len <= 3;
            case RECEIVING_CLIENT_CONFIRMATION -> len <= 5;
            case RECEIVING_COORDS1, RECEIVING_COORDS2, INIT_COORDS_RECEIVED, NAVIGATING -> len <= 10;
            case ARRIVED -> len <= 98;
        };
    }

    private Response checkKeyId(String in) {
        if (!in.matches("-?\\d+"))
            return new Response(SERVER_SYNTAX_ERROR, true);
        int keyId = Integer.parseInt(in);
        Optional<KeyMapping> keyMapping = KeyMapping.getByCode(keyId);

        if (keyMapping.isEmpty())
            return new Response(SERVER_KEY_OUT_OF_RANGE_ERROR, true);
        hash(keyMapping.get());
        communicationState = CommunicationState.RECEIVING_CLIENT_CONFIRMATION;
        return new Response(robot.getRobotHashServerKey().toString()+"\u0007\b");
    }

    private Response checkClientConfirmation(String in) {
        if (!in.matches("-?\\d+"))
            return new Response(SERVER_SYNTAX_ERROR, true);
        Integer clientConfirm = Integer.parseInt(in);
        if (clientConfirm.equals(robot.getRobotHashClientKey())) {
            communicationState = CommunicationState.RECEIVING_COORDS1;
            return new Response(SERVER_OK);
        }
        return new Response(SERVER_LOGIN_FAILED, true);
    }

    private Response createRobotName(String name) {
        robot = new Robot(name);
        communicationState = CommunicationState.RECEIVING_KEY_ID;
        return new Response(SERVER_KEY_REQUEST);
    }

    private void hash(KeyMapping keyMapping) {
        int nameSum = 0;
        int nameLen = robot.getName().length();
        for (int i = 0; i < nameLen; i++){
            nameSum += robot.getName().charAt(i);
        }

        robot.setRobotHashName(nameSum);
        robot.setRobotHashServerKey(keyMapping.getServerKey());
        robot.setRobotHashClientKey(keyMapping.getClientKey());

    }

    private Response setRobotCoords(String in) {
        Optional<Coords> coordsOptional = Coords.parse(in); // TODO: OPTIONAL OF NULLEBLE MAP
        Coords coords = null;
        if(communicationState != CommunicationState.RECEIVING_COORDS1){
            if (coordsOptional.isEmpty())
                return new Response(SERVER_SYNTAX_ERROR, true);
            else {
                coords = coordsOptional.get();
                if (coords.atEndPoint()) {
                    communicationState = CommunicationState.ARRIVED;
                    return new Response(SERVER_PICK_UP);
                }
            }
        }

        switch (communicationState) {
            case NAVIGATING:
                if (robot.stuck(coords) || robot.getStatus() != RobotStatus.FREE) {
                    return solveStuck();
                }
                return nextMove(coords);
            case RECEIVING_COORDS1:
                communicationState = CommunicationState.RECEIVING_COORDS2;
                System.out.println("Init rotate r1\n");
                return new Response(SERVER_TURN_RIGHT);
            case RECEIVING_COORDS2:
                robot.setCoords(coords);
                communicationState = CommunicationState.INIT_COORDS_RECEIVED;
                System.out.println("Init move r2\n");
                return new Response(SERVER_MOVE);
            case INIT_COORDS_RECEIVED:
                robot.setOrientation(robot.getOrientation(coords));
                communicationState = CommunicationState.NAVIGATING;
                return nextMove(coords);
        }
        return new Response(Response.EMPTY);
    }

    private Response nextMove(Coords coords) {

        if(robot.getOrientation() == Orientation.UNKNOWN1){
            robot.setOrientation(Orientation.UNKNOWN2);
            System.out.println("Robot set Unknown2\n");
            return new Response(SERVER_TURN_RIGHT);
        }
        if(robot.getOrientation() == Orientation.UNKNOWN2){
            robot.setOrientation(Orientation.UNKNOWN3);
            System.out.println("Robot Unknown3\n");
            return new Response(SERVER_MOVE);
        }
        if(robot.getOrientation() == Orientation.UNKNOWN3){
            robot.setOrientation(robot.getOrientation(coords));
            System.out.println("Robot rotated auto"+ robot.getOrientation() + "\n");
        }

        robot.setCoords(coords);
        System.out.println("x: " + robot.getCoords().x + ", y: " + robot.getCoords().y + ", o: " + robot.getOrientation() + "\n");
        if (robot.needRotation()) {
            robot.rotate();
            System.out.println("Robot rotated auto\n");
            robot.setLastTurnWasStraight(false);
            return new Response(SERVER_TURN_RIGHT);
        }
        System.out.println("Robot moved " + robot.getOrientation() + " auto\n");
        robot.setLastTurnWasStraight(true);
        return new Response(SERVER_MOVE);

    }

    private Response solveStuck() {
        if (robot.getStatus() == RobotStatus.FREE)
            robot.setStatus(RobotStatus.STUCK);
        switch (robot.getStatus()) {
            case STUCK:
                System.out.println("Robot stuck\n");
                Turn turn = robot.calculateBestRotation();
                robot.setAvoidTurn(turn);
                robot.setStatus(RobotStatus.ROTATED_TO_AVOID_STUCK);
                System.out.println("Robot rotated" + turn + "\n");
                robot.setLastTurnWasStraight(false);
                if (turn == Turn.RIGHT) {
                    return new Response(SERVER_TURN_RIGHT);
                } else {
                    return new Response(SERVER_TURN_LEFT);
                }
            case ROTATED_TO_AVOID_STUCK:
                robot.setStatus(RobotStatus.MOVED_TO_AVOID_STUCK);
                System.out.println("Robot moved to avoid stuck\n");
                robot.setLastTurnWasStraight(true);
                return new Response(SERVER_MOVE);
            case MOVED_TO_AVOID_STUCK:
                robot.setStatus(RobotStatus.ROTATED_AFTER_MOVE);
                System.out.println("Robot rotated after move to" + robot.getAvoidTurn() + "\n");
                robot.setLastTurnWasStraight(false);
                if (robot.getAvoidTurn() == Turn.RIGHT) {
                    return new Response(SERVER_TURN_LEFT);
                } else {
                    return new Response(SERVER_TURN_RIGHT);
                }
            case ROTATED_AFTER_MOVE:
                robot.setStatus(RobotStatus.FREE);
                robot.setLastTurnWasStraight(true);
                System.out.println("Robot moved after back rotation \n");
                return new Response(SERVER_MOVE);
        }
        System.out.println("Don't know how to solve stuck.\n");
        return new Response(Response.EMPTY);
    }

}
