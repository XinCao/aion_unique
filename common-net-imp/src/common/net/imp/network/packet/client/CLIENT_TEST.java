package common.net.imp.network.packet.client;

import common.net.imp.network.core.AionConnection;
import java.nio.ByteBuffer;

/**
 *
 * @author caoxin
 */
public class CLIENT_TEST extends AionClientPacket{

    public CLIENT_TEST(ByteBuffer buf, AionConnection client, int opcode) {
        super(buf, client, opcode);
    }

    @Override
    protected void readImpl() {
    }

    @Override
    protected void runImpl() {
    }
    
}
