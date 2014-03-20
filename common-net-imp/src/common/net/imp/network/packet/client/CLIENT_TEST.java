package common.net.imp.network.packet.client;

import common.net.imp.network.core.AionConnection;
import java.nio.ByteBuffer;

/**
 *
 * @author caoxin
 */
public class CLIENT_TEST extends AionClientPacket{
    
    private String name;

    public CLIENT_TEST(ByteBuffer buf, AionConnection client, Integer opcode) {
        super(buf, client, opcode);
    }

    @Override
    protected void readImpl() {
        name = this.readS();
    }

    @Override
    protected void runImpl() {
        System.out.println("client connection name = {" + name + "} ok!");
    }
}
