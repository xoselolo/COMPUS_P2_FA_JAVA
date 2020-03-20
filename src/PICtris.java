import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

public class PICtris extends JPanel {

    private static final int PIXEL_SIZE = 40;
    private static final int COLUMNS = 8;
    private static final int ROWS = 16;
    public static boolean gameOver = false;

    private static final Point[][][] TETRIMINOS = {
            // I-Piece
            {
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(3, 1)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(1, 3)},
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(3, 1)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(1, 3)}
            },

            // J-Piece
            {
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 0)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 2)},
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 2)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 0)}
            },

            // L-Piece
            {
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(2, 2)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(0, 2)},
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(0, 0)},
                    {new Point(1, 0), new Point(1, 1), new Point(1, 2), new Point(2, 0)}
            },

            // O-Piece
            {
                    {new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1)},
                    {new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1)},
                    {new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1)},
                    {new Point(0, 0), new Point(0, 1), new Point(1, 0), new Point(1, 1)}
            },

            // S-Piece
            {
                    {new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 1)},
                    {new Point(0, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2)},
                    {new Point(1, 0), new Point(2, 0), new Point(0, 1), new Point(1, 1)},
                    {new Point(0, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2)}
            },

            // T-Piece
            {
                    {new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(2, 1)},
                    {new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(1, 2)},
                    {new Point(0, 1), new Point(1, 1), new Point(2, 1), new Point(1, 2)},
                    {new Point(1, 0), new Point(1, 1), new Point(2, 1), new Point(1, 2)}
            },

            // Z-Piece
            {
                    {new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1)},
                    {new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(0, 2)},
                    {new Point(0, 0), new Point(1, 0), new Point(1, 1), new Point(2, 1)},
                    {new Point(1, 0), new Point(0, 1), new Point(1, 1), new Point(0, 2)}
            }
    };

    private static final Color[] TETRIMINO_COLORS = {
            new Color(0, 0, 255), new Color(0, 255, 0),
            new Color(0, 255, 255), new Color(255, 0, 0),
            new Color(255, 0, 255), new Color(255, 255, 0),
            new Color(255, 255, 255)
    };

    private Point pieceOrigin;
    private int currentPiece;
    private int rotation;
    private byte[] framebuffer;

    private int score;
    private Color[][] well;

    public PICtris() {
        setPreferredSize(new Dimension((COLUMNS * PIXEL_SIZE) + (COLUMNS - 1), (ROWS * PIXEL_SIZE) + (ROWS - 1)));

        well = new Color[COLUMNS][ROWS];
        framebuffer = new byte[ROWS];

        for (int i = 0; i < COLUMNS; i++) {
            for (int j = 0; j < ROWS; j++) {
                well[i][j] = Color.BLACK;
            }
        }

        newPiece();
    }

    // Put a new, random piece into the dropping position
    private void newPiece() {

        pieceOrigin = new Point(COLUMNS / 2 - 1, 0);
        rotation = 0;
        currentPiece = ThreadLocalRandom.current().nextInt(0, TETRIMINOS.length);
    }

    // Collision test for the dropping piece
    private boolean collidesAt(int x, int y, int rotation) {
        for (Point p : TETRIMINOS[currentPiece][rotation]) {
            if ((p.x + x) < 0 || (p.y + y) < 0 || well.length <= (p.x + x) || well[p.x + x].length <= (p.y + y) || well[p.x + x][p.y + y] != Color.BLACK) {
                return true;
            }
        }
        return false;
    }

    // Rotate the piece clockwise or counterclockwise
    public void rotate() {
        int newRotation = (rotation + 1) % 4;

        if (newRotation < 0) {
            newRotation = 3;
        }

        if (!collidesAt(pieceOrigin.x, pieceOrigin.y, newRotation)) {
            rotation = newRotation;
        }

        repaint();
    }

    // Move the piece left or right
    public void move(int i) {
        if (!collidesAt(pieceOrigin.x + i, pieceOrigin.y, rotation)) {
            pieceOrigin.x += i;
        }
        repaint();
    }

    public void userDropDown() {
        dropDown();
        if (!gameOver){
            score++;
        }
    }



    public void hardDrop(){
        do {
            if (!collidesAt(pieceOrigin.x, pieceOrigin.y + 1, rotation)) {
                pieceOrigin.y += 1;
            } else {
                fixToWell();
                break;
            }
        }while (true);

        repaint();
    }

    // Drops the piece one line or fixes it to the well if it can't drop
    public void dropDown() {
        if (!collidesAt(pieceOrigin.x, pieceOrigin.y + 1, rotation)) {
            pieceOrigin.y += 1;
        } else {
            fixToWell();
        }

        repaint();
    }

    // Make the dropping piece part of the well, so it is available for
    // collision detection.
    private void fixToWell() {
        if (collidesAt(pieceOrigin.x, pieceOrigin.y, rotation)) {
            gameOver = true;
            return;
        }
        for (Point p : TETRIMINOS[currentPiece][rotation]) {
            well[pieceOrigin.x + p.x][pieceOrigin.y + p.y] = TETRIMINO_COLORS[currentPiece];
        }
        clearRows();
        newPiece();
    }

    private void deleteRow(int row) {
        for (int j = row - 1; j > 0; j--) {
            for (int i = 0; i < COLUMNS; i++) {
                well[i][j + 1] = well[i][j];
            }
        }
    }

    // Clear completed rows from the field and award score according to
    // the number of simultaneously cleared rows.
    private void clearRows() {
        int numClears = 0;

        for (int j = ROWS - 1; j > 0; j--) {
            boolean gap = false;

            for (int i = 0; i < COLUMNS; i++) {
                if (well[i][j] == Color.BLACK) {
                    gap = true;
                    break;
                }
            }

            if (!gap) {
                deleteRow(j);
                j += 1;
                numClears += 1;
            }
        }

        switch (numClears) {
            case 1:
                score += 100;
                break;
            case 2:
                score += 300;
                break;
            case 3:
                score += 500;
                break;
            case 4:
                score += 800;
                break;
        }
    }

    // Draw the falling piece
    private void drawPiece(Graphics g) {
        g.setColor(TETRIMINO_COLORS[currentPiece]);
        for (Point p : TETRIMINOS[currentPiece][rotation]) {
            g.fillRect((p.x + pieceOrigin.x) * (PIXEL_SIZE + 1),
                    (p.y + pieceOrigin.y) * (PIXEL_SIZE + 1),
                    PIXEL_SIZE, PIXEL_SIZE);
            if ((p.x + pieceOrigin.x) <= 7) {
                framebuffer[p.y + pieceOrigin.y] = (byte) (framebuffer[p.y + pieceOrigin.y] | (1 << (7 - (p.x + pieceOrigin.x))));
            }
        }
    }

    public byte[] getFrameBuffer() {
        return framebuffer;
    }

    @Override
    public void paintComponent(Graphics g) {
        // Paint the well
        g.fillRect(0, 0, COLUMNS * PIXEL_SIZE + (COLUMNS - 1), ROWS * PIXEL_SIZE + (ROWS - 1));
        for (int j = 0; j < ROWS; j++) {
            framebuffer[j] = 0;
        }
        for (int i = 0; i < COLUMNS; i++) {
            for (int j = 0; j < ROWS; j++) {
                g.setColor(well[i][j]);
                g.fillRect((PIXEL_SIZE + 1) * i, (PIXEL_SIZE + 1) * j, PIXEL_SIZE, PIXEL_SIZE);
                if (i <= 7) {
                    framebuffer[j] = (byte) (framebuffer[j] | (((well[i][j] != Color.BLACK) ? 1 : 0) << (7 - i)));
                }
            }
        }

        // Display the score
        g.setColor(Color.WHITE);
        g.drawString("" + score, 19 * COLUMNS, ROWS);

        // Draw the currently falling piece
        drawPiece(g);
    }

    public int getScore() {
        return score;
    }
}