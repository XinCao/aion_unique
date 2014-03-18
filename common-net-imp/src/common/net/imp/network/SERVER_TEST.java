package common.net.imp.network;

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
        throw new UnsupportedOperationException("Not supported yet.");
    }
}