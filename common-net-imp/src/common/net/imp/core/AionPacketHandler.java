package common.net.imp.core;

import common.net.imp.packet.server.AionServerPacket;
import common.net.imp.packet.client.AionClientPacket;
import common.net.imp.packet.client.CLIENT_TEST;
import common.net.imp.packet.server.SERVER_TEST;
import common.net.imp.core.AionConnection.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.log4j.Logger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class AionPacketHandler {

    private static final Logger log = Logger.getLogger(AionPacketHandler.class);

    public enum AionClientKind {

        CLIENT_TEST(0x0001, CLIENT_TEST.class, State.CONNECTED)
        ;
        public static final Map<Integer, AionClientKind> authedLoginAionClientKindMap = new HashMap<Integer, AionClientKind>();
        public static final Map<Integer, AionClientKind> connectedAionClientKindMap = new HashMap<Integer, AionClientKind>();
        public static final Map<Integer, AionClientKind> authedGGAionClientKindMap = new HashMap<Integer, AionClientKind>();

        static {
            for (AionClientKind ack : values()) {
                switch (ack.getState()) {
                    case AUTHED_LOGIN: {
                        authedLoginAionClientKindMap.put(ack.getOpcode(), ack);
                        break;
                    }
                    case CONNECTED: {
                        connectedAionClientKindMap.put(ack.getOpcode(), ack);
                        break;
                    }
                    case AUTHED_GG: {
                        authedGGAionClientKindMap.put(ack.getOpcode(), ack);
                        break;
                    }
                }
            }
        }
        private int opcode;
        private Class<AionClientPacket> clazz;
        private State state;

        private AionClientKind(int opcode, Class clazz, State state) {
            this.opcode = opcode;
            this.clazz = clazz;
            this.state = state;
        }

        public int getOpcode() {
            return this.opcode;
        }

        public Class<AionClientPacket> getClazz() {
            return this.clazz;
        }

        public State getState() {
            return this.state;
        }
    }

    /**
     * 获得客户端包
     * 
     * @param data
     * @param client
     * @return 
     */
    public static AionClientPacket handle(ByteBuffer data, AionConnection client) {
        AionClientPacket msg = null;
        State state = client.getState();
        int opcode = data.get() & 0xff;
        AionClientKind ack = null;
        switch (state) {
            case CONNECTED: {
                ack = AionClientKind.connectedAionClientKindMap.get(opcode);
                break;
            }
            case AUTHED_GG: {
                ack = AionClientKind.authedGGAionClientKindMap.get(opcode);
                break;
            }
            case AUTHED_LOGIN: {
                ack = AionClientKind.authedLoginAionClientKindMap.get(opcode);
                break;
            }
            default: {
                unknownPacket(state, opcode);
            }
        }
        if (ack != null) {
            try {
                Class<AionClientPacket> clazz = ack.getClazz();
                Constructor<AionClientPacket> constructor = clazz.getConstructor(ByteBuffer.class, AionConnection.class, Integer.class);
                msg = constructor.newInstance(data, client, ack.getOpcode());
            } catch (NoSuchMethodException ex) {
                log.warn(ex);
            } catch (SecurityException ex) {
                log.warn(ex);
            } catch (InstantiationException ex) {
                log.warn(ex);
            } catch (IllegalAccessException ex) {
                log.warn(ex);
            } catch (IllegalArgumentException ex) {
                log.warn(ex);
            } catch (InvocationTargetException ex) {
                log.warn(ex);
            }
        }
        return msg;
    }

    private static void unknownPacket(State state, int id) {
        log.warn(String.format("Unknown packet recived from Aion client: 0x%02X state=%s", id, state.toString()));
    }

    public static enum AionServerKind {

        SERVER_TEST(0x0002, SERVER_TEST.class, State.CONNECTED)
        ;
        private int opcode;
        private Class<AionServerPacket> clazz;
        private State state;

        private AionServerKind(int opcode, Class clazz, State state) {
            this.opcode = opcode;
            this.clazz = clazz;
            this.state = state;
        }

        public int getOpcode() {
            return this.opcode;
        }

        public Class<AionServerPacket> getClazz() {
            return this.clazz;
        }

        public State getState() {
            return this.state;
        }
    }

    /**
     * 获得服务器包
     * 
     * @param ask
     * @param client
     * @return 
     */
    public static AionServerPacket getServerPacketByASK(AionServerKind ask, AionConnection client) {
        AionServerPacket msg = null;
        State state = client.getState();
        if (ask.getState() == state) {
            try {
                Class<AionServerPacket> clazz = ask.getClazz();
                Constructor<AionServerPacket> constructor = clazz.getConstructor(Integer.class);
                msg = constructor.newInstance(ask.getOpcode());
            } catch (NoSuchMethodException ex) {
                log.warn(ex);
            } catch (SecurityException ex) {
                log.warn(ex);
            } catch (InstantiationException ex) {
                log.warn(ex);
            } catch (IllegalAccessException ex) {
                log.warn(ex);
            } catch (IllegalArgumentException ex) {
                log.warn(ex);
            } catch (InvocationTargetException ex) {
                log.warn(ex);
            }
        }
        return msg;
    }
}