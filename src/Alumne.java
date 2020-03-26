import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class Alumne {

    private static final char[] GAME_OVER_MESSAGE = "Game Over".toCharArray();
    private static final char GAME_OVER_END_CHARACTER = '+';

    // Send trama
    private static int fila = 1;

    // Trama type
    public final static String TRAMA_TYPE_ACK = "41"; // = A
    private final static String TRAMA_TYPE_BUTTON = "42"; // = B
    private final static String TRAMA_TYPE_JOYSTICK = "4A"; // = J

    // Buttons types
    private final static String TRAMA_BUTTON_UP = "55";
    private final static String TRAMA_BUTTON_DOWN = "44";
    private final static String TRAMA_BUTTON_LEFT = "4C";
    private final static String TRAMA_BUTTON_RIGHT = "52";
    private final static String TRAMA_BUTTON_DOWN_OPCIONAL = "4F";


    private final static char[] CONST = "0123456789ABCDEF".toCharArray();

    public static final int TIME_WAIT_MS = 10;

    public static void sendFramebuffer(PICtris pictris, SerialPort serialPort) {
        byte[] framebuffer = pictris.getFrameBuffer();

        // Write your code here
        byte[] writeBuffer = new byte[2];
        writeBuffer[0] = (byte) 0x00; // 0000 0000 (porque no es game over)
        writeBuffer[1] = framebuffer[fila]; // FILA

        serialPort.writeBytes(writeBuffer, 2);
    }

    public static void getInput(PICtris pictris, SerialPort serialPort) {
        /* Example code:
           pictris.move(-1); // Moves the current piece 1 block to the left
           pictris.move(1); // Moves the current piece 1 block to the right
           pictris.rotate(); // Rotates the current piece
           pictris.userDropDown(); // Drops the current piece 1 position
           pictris.hardDrop(); // Drops the current piece 1 position
         */


        // Write your code here
        if (serialPort.bytesAvailable() > 0) {

            byte[] bytes = new byte[1];
            serialPort.readBytes(bytes, 1);
            String receivedValue = convertBytes(bytes).substring(0, 2);

            //System.out.println("Received: " + receivedValue);

            switch (receivedValue){
                case TRAMA_TYPE_ACK:
                    sendFila(pictris, serialPort);
                    break;
                case TRAMA_TYPE_BUTTON:
                    readButton(pictris, serialPort);
                    break;
                case TRAMA_TYPE_JOYSTICK:
                    readJoysticks(pictris, serialPort);
                    break;
            }
        }
    }

    private static void sendFila(PICtris pictris, SerialPort serialPort) {
        sendFramebuffer(pictris, serialPort);
        fila++;
        if (fila == 16){
            fila = 0;
        }
    }

    private static void readButton(PICtris pictris, SerialPort serialPort) {
        // Read button
        while (serialPort.bytesAvailable() < 1){ }

        byte[] byteButton = new byte[1];
        serialPort.readBytes(byteButton, 1);

        String buttonValue = convertBytes(byteButton).substring(0, 2);

        switch (buttonValue){
            case TRAMA_BUTTON_UP:
                pictris.rotate();
                break;
            case TRAMA_BUTTON_DOWN:
                pictris.userDropDown();
                break;
            case TRAMA_BUTTON_LEFT:
                pictris.move(-1);
                break;
            case TRAMA_BUTTON_RIGHT:
                pictris.move(1);
                break;
            case TRAMA_BUTTON_DOWN_OPCIONAL:
                pictris.hardDrop();
                break;
        }
    }

    private static void readJoysticks(PICtris pictris, SerialPort serialPort) {
        while (serialPort.bytesAvailable() < 2){ }


        byte[] bytes = new byte[2];

        serialPort.readBytes(bytes, 2);

        int joy_x = bytes[0];
        int joy_y = bytes[1];

        boolean centerx = false,
                centery = false,
                up = false,
                down = false,
                left = false,
                right = false;

        if (joy_x >= 0 && joy_x < 103){
            left = true;
        }else{
            if (joy_x <= -1 && joy_x > -103){
                right = true;
            }else{
                centerx = true;
            }
        }

        if (joy_y >= 0 && joy_y < 103){
            up = true;
        }else{
            if (joy_y <= -1 && joy_y > -103){
                down = true;
            }else{
                centery = true;
            }
        }

        if (!(centerx && centery)){
            // passa del limbo en almenos una de las direcciones
            if (centerx){
                if (up){
                    // UP
                    pictris.rotate();
                }else{
                    // DOWN
                    pictris.userDropDown();
                }
            }else{
                if (centery){
                    if (left){
                        // LEFT
                        pictris.move(-1);
                    }else{
                        // RIGHT
                        pictris.move(1);
                    }
                }else{
                    int difx = Math.abs(joy_x);
                    int dify = Math.abs(joy_y);

                    if (difx < dify){
                        if (left){
                            // LEFT
                            pictris.move(-1);
                        }else {
                            // RIGHT
                            pictris.move(1);
                        }
                    }else{
                        if (up){
                            // UP
                            pictris.rotate();
                        }else{
                            // DOWN
                            pictris.userDropDown();
                        }
                    }
                }
            }
            System.out.println(System.lineSeparator());
            System.out.println(System.lineSeparator());
            System.out.println("Left" + left);
            System.out.println("Right" + right);
            System.out.println("Up" + up);
            System.out.println("Down" + down);
            System.out.println("X : " + bytes[0] + " , " + joy_x);
            System.out.println("Y : " + bytes[1] + " , " + joy_y);

        }
    }

    public static String convertBytes(byte[] bytesReceived){
        int length = bytesReceived.length;
        char[] hexValues = new char[length * 2];
        for (int i = 0; i < length; i++){
            int value = bytesReceived[i] & 0xFF;
            hexValues[i * 2] = CONST[value >>> 4];
            hexValues[i * 2 + 1] = CONST[value & 0x0F];
        }

        return new String(hexValues);
    }

    public static void gameOver(PICtris pictris, SerialPort serialPort) {
        System.out.println("GAME OVER");
        int score = pictris.getScore();
        String stringScore = String.valueOf(score);
        char[] scoreArray = stringScore.toCharArray();
        boolean isAck = false;
        String receivedValue;

        byte[] bytes = new byte[1];

        while(!isAck){
            while (serialPort.bytesAvailable() < 1){}

            serialPort.readBytes(bytes, 1);
            receivedValue = convertBytes(bytes).substring(0, 2);

            if (TRAMA_TYPE_ACK.equals(receivedValue)) {
                isAck = true;
            } else {
                isAck = false;
            }
        }

        /*
        PROTOCOLO DE GAME OVER
        0) Recibir ACK por parte de la PIC
        1) Enviar caracter '1111 1111' (porque en asembler -> IS_END = 1)
        2) Enviar mensaje de "game over"
        3) Enviar caracter ' '
        4) Enviar puntuacion "1452"
        5) Enviar caracter '+'

         */

        int gameOverLength = GAME_OVER_MESSAGE.length;
        int scoreLength = stringScore.length();
        int totalLength = gameOverLength + scoreLength + 3;

        byte[] writeBuffer = new byte[totalLength];
        writeBuffer[0] = (byte) 0xFF; // 1111 1111 porque indica que es games over (IS_END = 1)
        int i = 1;
        for(int j = 0; j < gameOverLength; j++, i++){
            writeBuffer[i] = (byte)GAME_OVER_MESSAGE[j];
        }
        writeBuffer[i] = (byte) ' ';
        i++;
        for(int j = 0; j < scoreLength; j++, i++){
            writeBuffer[i] = (byte)scoreArray[j];
        }
        writeBuffer[i] = (byte) GAME_OVER_END_CHARACTER;

        // Enviamos la trama de game over ["1game over 1452+]
        byte[] bytesRecived = new byte[1];
        int bytesSent = 0;

        // Enviamos el 1
        serialPort.writeBytes(writeBuffer, 1, bytesSent);

        bytesSent++;
        while (bytesSent < totalLength){
            // wait for ACK
            while (serialPort.bytesAvailable() < 1){}
            System.out.println("ACK : " + bytesSent);

            // read ACK
            serialPort.readBytes(bytesRecived, 1);
            // No miramos que hemos recibido porque lo importante es que la PIC nos
            // vaya enviando bytes para comunicarnos que esta lista para recibir un
            // nuevo byte

            // send next byte
            serialPort.writeBytes(writeBuffer, 1, bytesSent);
            bytesSent++;
        }

        // El caracter entra por MARQUESINA_0 y sale por MARQUESINA_1

    }
}
