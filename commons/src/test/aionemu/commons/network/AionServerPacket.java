package test.aionemu.commons.network;

import com.aionemu.commons.network.packet.BaseServerPacket;
import java.nio.ByteBuffer;

public abstract class AionServerPacket extends BaseServerPacket {

    protected AionServerPacket(int opcode) {
        super(opcode);
    }

    public final void write(AionConnection con, ByteBuffer buf) {
        buf.putShort((short) 0);
        writeImpl(con, buf);
        buf.flip();
    }

    protected abstract void writeImpl(AionConnection con, ByteBuffer buf);
}