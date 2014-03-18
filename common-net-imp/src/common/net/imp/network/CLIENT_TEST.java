package common.net.imp.network;

import java.nio.ByteBuffer;

/**
 *
 * @author caoxin
 */
public class CLIENT_TEST extends AionClientPacket{

    public static final Class<CLIENT_TEST> clazz = CLIENT_TEST.class;

    public CLIENT_TEST(ByteBuffer buf, AionConnection client, int opcode) {
        super(buf, client, opcode);
    }

    @Override
    protected void readImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void runImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
