package common.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 *
 * @author caoxin
 */
public class SocketClient {

    public static void main(String... args) throws Exception {
        BufferedReader cmdBufferReader = new BufferedReader(new InputStreamReader(System.in));
        String[] cmd = cmdBufferReader.readLine().split("\\s");
        String ip = "";
        int port = 0;
        short opcode = 0;
        int length = cmd.length;
        if ((length > 0 && "help".equals(cmd[0])) || length == 0 || cmd[0].equals("")) {
            System.out.println("use : {ip}, {port}, {opcode} if {ip} is null or {*} default '127.0.0.1'");
            System.exit(1);
        } else if (length > 0 && cmd[0].equals("default")) {
            ip = "127.0.0.1";
            port = 8000;
            opcode = 1;
        } else if (length == 1) {
            ip = "127.0.0.1";
            port = 8000;
            opcode = new Short(cmd[0]);
        } else if (length == 2) {
            ip = "127.0.0.1";
            port = new Integer(cmd[0]);
            opcode = new Short(cmd[1]);
        } else if (length == 3) {
            ip = cmd[0];
            port = new Integer(cmd[1]);
            opcode = new Short(cmd[2]);
        }
        Socket socket = new Socket(ip, port);
        OutputStream outputStream = socket.getOutputStream();
        Thread outInfoThread = new Thread(new OutputInfo(outputStream, opcode), "write thread");
        outInfoThread.start();
        InputStream inputStream = socket.getInputStream();
        Thread inputInfoThread = new Thread(new InputInfo(inputStream), "read thread");
        inputInfoThread.start();
    }

    static class OutputInfo implements Runnable {

        private static final Logger logger = Logger.getLogger(OutputInfo.class);
        private BufferedReader outBufferedReader = new BufferedReader(new InputStreamReader(System.in));
        private OutputStream outputStream;
        private short opcode;

        public OutputInfo(OutputStream outputStream, short opcode) {
            this.outputStream = outputStream;
            this.opcode = opcode;
        }

        @Override
        public void run() {
            ByteBuffer outputByteBuffer = ByteBuffer.allocate(256);
            outputByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            String outputStr = "Start connect server!";
            logger.info(outputStr);
            while (true) {
                try {
                    if ((outputStr = outBufferedReader.readLine()) != null) {
                        int sizePosition = outputByteBuffer.position();
                        outputByteBuffer.putShort((short) 0);
                        outputByteBuffer.put((byte) opcode);
                        String[] words = outputStr.split("\\s");
                        int num = words.length;
                        if (num % 2 != 0) {
                            logger.info("Please check the incoming data!");
                            continue;
                        }
                        for (int i = 0; i < num; i = i + 2) {
                            if (isInt(words[i])) {
                                outputByteBuffer.putInt(new Integer(words[i + 1]));
                            } else if (isShort(words[i])) {
                                outputByteBuffer.putShort(new Short(words[i + 1]));
                            } else if (isString(words[i])) {
                                String s = words[i + 1];
                                outputByteBuffer.putInt(s.length());
                                for (int j = 0; j < s.length(); j++) {
                                    outputByteBuffer.putChar(s.charAt(j));
                                }
                                outputByteBuffer.putChar('\000');
                            }
                        }
                        int afterPosition = outputByteBuffer.position();
                        outputByteBuffer.position(sizePosition);
                        outputByteBuffer.putShort((short) (afterPosition - sizePosition));
                        outputByteBuffer.position(afterPosition);
                        if (outputByteBuffer.hasArray()) {
                            byte[] b = Arrays.copyOfRange(outputByteBuffer.array(), outputByteBuffer.arrayOffset(), outputByteBuffer.position());
                            outputStream.write(b);
                        }
                        outputByteBuffer.clear();
                    }
                } catch (IOException ex) {
                    logger.debug(ex.getMessage());
                }
            }
        }
    }

    public static boolean isInt(String type) {
        if (type.equalsIgnoreCase("int")) {
            return true;
        }
        return false;
    }

    public static boolean isShort(String type) {
        if (type.equalsIgnoreCase("short")) {
            return true;
        }
        return false;
    }

    public static boolean isString(String type) {
        if (type.equalsIgnoreCase("string")) {
            return true;
        }
        return false;
    }

    static class InputInfo implements Runnable {

        private static final Logger logger = Logger.getLogger(InputInfo.class);
        private InputStream inputStream;

        public InputInfo(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            ByteBuffer inputByteBuffer = ByteBuffer.allocate(256);
            inputByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            logger.info("Begin to accept data!");
            while (true) {
                this.read(inputStream, inputByteBuffer);
            }
        }

        private void read(InputStream inputStream, ByteBuffer inputByteBuffer) {
            int numRead = 0;
            try {
                numRead = inputStream.read(inputByteBuffer.array());
            } catch (IOException ex) {
            }
            if (numRead < 0) {
                return;
            } else {
                inputByteBuffer.position(numRead);
            }
            inputByteBuffer.flip();
            inputByteBuffer.mark();
            while (inputByteBuffer.remaining() > 2 && inputByteBuffer.remaining() >= inputByteBuffer.getShort(inputByteBuffer.position())) { // 读取是否为一个整包（也可能大于一个整包（多个包），因此这里会使用循环）
                if (!parse(inputByteBuffer)) { // 判断包是否合法
                    return;
                }
            }
            if (inputByteBuffer.hasRemaining()) {
                inputByteBuffer.compact(); // 将缓冲区的当前位置和界限之间的字节复制到缓冲区的开始处（为下一个包准备）
            } else {
                inputByteBuffer.clear();
            }
        }

        private boolean parse(ByteBuffer buf) {
            short sz = 0;
            try {
                buf.reset();
                sz = buf.getShort();
                if (sz > 1) {
                    sz -= 2;
                }
                ByteBuffer b = (ByteBuffer) buf.slice().limit(sz); // 创建新的缓冲区
                b.order(ByteOrder.LITTLE_ENDIAN); // 小端模式，高字节存储在高地址
                buf.position(buf.position() + sz); // 写一个包数据开始处
                buf.mark();
                return go(b);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public boolean go(ByteBuffer byteBuffer) {
            byte opcode = byteBuffer.get();
            StringBuilder sb = new StringBuilder();
            char c;
            while ((c = byteBuffer.getChar()) != '\000') {
                sb.append(c);
            }
            logger.info(String.format("packet opcode = {%d} date = {%s}", opcode, sb.toString()));
            return true;
        }
    }
}