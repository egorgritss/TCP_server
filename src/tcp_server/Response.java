package tcp_server;

public class Response {
    public String message;
    public Boolean closeAfter;
    public static String EMPTY = "";

    public Response(String message, Boolean closeAfter) {
        this.message = message;
        this.closeAfter = closeAfter;
    }

    public Response(String message) {
        this.message = message;
        this.closeAfter =false;
    }
}
