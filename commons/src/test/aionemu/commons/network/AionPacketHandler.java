package test.aionemu.commons.network;

import org.apache.log4j.Logger;
import test.aionemu.commons.network.AionConnection.State;
import java.nio.ByteBuffer;

public class AionPacketHandler {

    private static final Logger log = Logger.getLogger(AionPacketHandler.class);

    public static AionClientPacket handle(ByteBuffer data, AionConnection client) {
        AionClientPacket msg = null;
        State state = client.getState();
        int id = data.get() & 0xff;
        switch (state) {
            case CONNECTED: {
                switch (id) {
                    case 0x07:
                        break;
                    case 0x08:
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED_GG: {
                switch (id) {
                    case 0x0B:
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
            case AUTHED_LOGIN: {
                switch (id) {
                    case 0x05:
                        break;
                    case 0x02:
                        break;
                    default:
                        unknownPacket(state, id);
                }
                break;
            }
        }
        return msg;
    }

    private static void unknownPacket(State state, int id) {
        log.warn(String.format("Unknown packet recived from Aion client: 0x%02X state=%s", id, state.toString()));
    }
}