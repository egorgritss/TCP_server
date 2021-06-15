package tcp_server;


import java.util.Arrays;
import java.util.Optional;

public enum KeyMapping {

    MAPPING_0(0, 23019, 32037),
    MAPPING_1(1, 32037, 29295),
    MAPPING_2(2, 18789, 13603),
    MAPPING_3(3, 16443, 29533),
    MAPPING_4(4, 18189, 21952);


    private final Integer code;
    private final Integer clientKey;
    private final Integer serverKey;

    public Integer getClientKey() {
        return clientKey;
    }

    public Integer getServerKey() {
        return serverKey;
    }

    KeyMapping(int i, int i1, int i2) {
        code = i;
        clientKey = i2;
        serverKey = i1;
    }

    public static Optional<KeyMapping> getByCode(int code) {
        return Arrays.stream(values())
                .filter(val -> val.code.equals(code))
                .findFirst();
    }
}
