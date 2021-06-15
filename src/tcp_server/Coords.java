package tcp_server;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Coords {
    public int x;
    public int y;


    public Coords(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Coords(Coords coords) {
        this.x = coords.x;
        this.y = coords.y;
    }


    public static Optional<Coords> parse(String message) {

        Pattern pattern = Pattern.compile("OK\\s(-?\\d+)\\s(-?\\d+)");
        Matcher m = pattern.matcher(message);
        if(!m.matches())
            return Optional.empty();

        return Optional.of(new Coords(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
    }

    public boolean atEndPoint(){
        return x == 0 && y == 0;
    }
}
