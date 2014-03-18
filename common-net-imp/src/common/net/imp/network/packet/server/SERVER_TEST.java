package common.net.imp.network.packet.server;

import common.net.imp.network.core.AionConnection;
import java.nio.ByteBuffer;

/**
 *
 * @author caoxin
 */
public class SERVER_TEST extends AionServerPacket {

    public SERVER_TEST(int opcode) {
        super(opcode);
    }

    @Override
    protected void writeImpl(AionConnection con, ByteBuffer buf) {
        this.writeS(buf, "hello world!");
    }
}