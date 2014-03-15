package common.net.imp.network;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.Dispatcher;
import com.aionemu.commons.network.PacketProcessor;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;

public class AionConnection extends AConnection {

    private static final Logger log = Logger.getLogger(AionConnection.class);
    private final static PacketProcessor<AionConnection> processor = new PacketProcessor<AionConnection>(1, 8);
    private final Deque<AionServerPacket> sendMsgQueue = new ArrayDeque<AionServerPacket>();
    private int sessionId = hashCode();
    private boolean joinedGs;
    private State state;
    public static enum State {
        CONNECTED,
        AUTHED_GG,
        AUTHED_LOGIN
    }

    public AionConnection(SocketChannel sc, Dispatcher d) throws IOException {
        super(sc, d);
        state = State.CONNECTED;
        String ip = getIP();
        log.info("connection from: " + ip);
//        sendPacket(new SM_INIT(this, blowfishKey));
        
    }

    @Override
    protected final boolean processData(ByteBuffer data) {
        AionClientPacket pck = AionPacketHandler.handle(data, this);
        log.info("recived packet: " + pck);
        if ((pck != null) && pck.read()) {
            processor.executePacket(pck);
        }
        return true;
    }

    @Override
    protected final boolean writeData(ByteBuffer data) {
        synchronized (guard) {
            AionServerPacket packet = sendMsgQueue.pollFirst();
            if (packet == null) {
                return false;
            }
            packet.write(this, data);
            return true;
        }
    }

    @Override
    protected final long getDisconnectionDelay() {
        return 0;
    }

    /**
     * DisconnectionTask 调用（可以添加服务逻辑）
     */
    @Override
    protected final void onDisconnect() {
    }

    /**
     * shutdown 调用 （可以添加服务逻辑）
     */
    @Override
    protected final void onServerClose() {
        close(true);
    }

    public final void sendPacket(AionServerPacket bp) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }
            log.debug("sending packet: " + bp);
            sendMsgQueue.addLast(bp);
            enableWriteInterest();
        }
    }

    public final void close(AionServerPacket closePacket, boolean forced) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }
            log.info("sending packet: " + closePacket + " and closing connection after that.");
            pendingClose = true;
            isForcedClosing = forced;
            sendMsgQueue.clear();
            sendMsgQueue.addLast(closePacket);
            enableWriteInterest();
        }
    }

    public final int getSessionId() {
        return sessionId;
    }

    public final State getState() {
        return state;
    }

    public final void setState(State state) {
        this.state = state;
    }

    public final void setJoinedGs() {
        joinedGs = true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}