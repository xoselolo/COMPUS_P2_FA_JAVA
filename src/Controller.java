import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.Executors;

public class Controller extends JPanel {

    private static boolean CONNECTION_ERROR = false;

    private PICtris pictris;
    private Thread readThread;

    private JComboBox<SerialPort> portsComboBox;
    private JComboBox<Integer> baudRate;
    private JButton connectButton;
    private JButton refreshButton;

    private Integer[] standarBaudrate = {110, 300, 600, 1200, 2400, 4800, 9600, 14400, 19200, 38400, 57600, 115200, 128000 , 256000};

    private boolean paused;
    private boolean isConnected;
    private SerialPort[] availablePorts;
    private SerialPort selectedPort;

    private MusicPlayer music;

    public Controller()  {
        setLayout(new FlowLayout());

        this.portsComboBox = new JComboBox<>();
        add(this.portsComboBox);

        this.baudRate = new JComboBox<Integer>(standarBaudrate);

        add(this.baudRate);




        this.connectButton = new JButton("Connect!");
        this.connectButton.addActionListener(e -> {
            SerialPort selectedPort = (SerialPort) portsComboBox.getSelectedItem();
            for (SerialPort sp : availablePorts) {
                System.out.println(sp.getSystemPortName());
                System.out.println(sp.getDescriptivePortName());
                System.out.println(sp.toString());
            }
            for (SerialPort sp : availablePorts) {
                if (sp.getSystemPortName().equals(selectedPort.getSystemPortName())) {
                    System.out.println("Connecting...");
                    connectToPort(sp);
                    break;
                }
            }
        });
        add(this.connectButton);

        this.refreshButton = new JButton("Refresh ports");
        this.refreshButton.addActionListener(e -> updatePorts());
        add(this.refreshButton);

        this.isConnected = false;
        this.paused = true;

        updatePorts();
    }

    private void sendConnectionTrama(SerialPort selectedPort) {
        // Write your code here
        byte[] writeBuffer = new byte[1];
        writeBuffer[0] = (byte) 0xFF;

        selectedPort.writeBytes(writeBuffer, 1);
    }

    public void bindPICtris(PICtris pictris) {
        this.pictris = pictris;
        configureKeyBindings(this.pictris);

        paused = false;

        // Make the falling piece drop every second
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (!paused && !pictris.gameOver) {
                        pictris.dropDown();
                    }
                    if (pictris.gameOver){
                        Alumne.gameOver(pictris,selectedPort);
                        JOptionPane.showMessageDialog(new JFrame(), "Tu puntuacion ha sido de " + pictris.getScore() + ". Gracias por jugar el programa se cerrara en 8 segundos");
                        Thread.sleep(8000);
                        System.exit(0);
                        break;
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    private void updatePorts() {
        availablePorts = SerialPort.getCommPorts();

        portsComboBox.setModel(new DefaultComboBoxModel<>(availablePorts));
        portsComboBox.setEnabled(availablePorts.length > 0);
    }

    private void connectToPort(SerialPort port) {
        if (port.isOpen()) {
            System.out.println("\"" + port.getSystemPortName() + "\" is already open!");
            return;
        }else{
            port.setBaudRate(standarBaudrate[baudRate.getSelectedIndex()]);
            selectedPort = port;
            isConnected = true;
            port.openPort();

            // ---------------------- CONNECTION PROTOCOL (start) -----------------------
            // Enviamos trama de conexion
            sendConnectionTrama(selectedPort);
            // ---------------------- CONNECTION PROTOCOL (end) -----------------------

            if (!CONNECTION_ERROR){
                JFrame f = new JFrame("PICtris");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setResizable(false);
                f.setLayout(new BorderLayout());
                f.setIconImage(new ImageIcon("icon.png").getImage());


                Thread readThread = new Thread()
                {
                    public void run()
                    {
                        do {
                            try {
                                getIntput();
                                sleep(Alumne.TIME_WAIT_MS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }while (true);
                    }
                };
                readThread.start();

                final PICtris pictris = new PICtris() {
                    @Override
                    public void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        //sendData(); // commented to debug
                    }
                };
                bindPICtris(pictris);
                f.add(pictris, BorderLayout.CENTER);

                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);


                try {
                    music = new MusicPlayer("song/TUSA.wav");
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }
            }

        }

        if (!port.openPort()) {
            System.out.println("Error while opening \"" + port.getSystemPortName() + "\"!");
            return;
        }
    }

    private void getIntput() {
        if (isConnected) {
            Alumne.getInput(pictris, selectedPort);
        }
    }

    private void configureKeyBindings(PICtris pictris) {
        InputMap im = pictris.getInputMap();
        ActionMap am = pictris.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "MOVE_UP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "MOVE_DOWN");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "MOVE_LEFT");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "MOVE_RIGHT");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "PAUSE");

        am.put("MOVE_UP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!paused) {
                    pictris.rotate();
                }
            }
        });

        am.put("MOVE_DOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!paused) {
                    pictris.userDropDown();
                }
            }
        });

        am.put("MOVE_LEFT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!paused) {
                    pictris.move(-1);
                }
            }
        });

        am.put("MOVE_RIGHT", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!paused) {
                    pictris.move(1);
                }
            }
        });

        am.put("PAUSE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                paused = !paused;
                try {
                    music.pause();
                } catch (UnsupportedAudioFileException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (LineUnavailableException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void sendData() {
        if (isConnected) {
            Alumne.sendFramebuffer(pictris, selectedPort);
        }
    }

    private static void printFramebuffer(byte[] fb) {
        for (byte b : fb) {
            for (int i = 0; i < 8; i++) {
                System.out.print(((b >> (7 - i)) & 0x01));
            }
            System.out.println();
        }
    }
}
