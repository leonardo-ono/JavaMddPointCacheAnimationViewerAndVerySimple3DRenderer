import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.logging.*;
import java.util.List;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * MDD (point cache) Animation + Very simple experimental 3D Renderer.
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class View extends JPanel {

    BufferedImage skin;
    double[][] points, sts, va = new double[3][5];
    int[][] faces, ps = new int [2][3];
    
    public View() {
        try {
            InputStream is = getClass().getResourceAsStream("female_char.jpg");
            skin = ImageIO.read(is);
            try ( DataInputStream dis = new DataInputStream(
                    getClass().getResourceAsStream("female_char.mdd")) ) {

                int totalFrames = dis.readInt();
                int totalPoints = dis.readInt();
                dis.read(new byte[totalFrames * 4]); // unused datas
                points = new double[totalFrames][totalPoints * 3];
                for (int s = 0; s < totalFrames; s++) {
                    for (int p = 0; p < totalPoints * 3; p++) {
                        points[s][p] = dis.readFloat();
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        try ( Scanner sc = new Scanner(
                getClass().getResourceAsStream("female_char.obj")) ) {
            
            List<int[]> facesTmp = new ArrayList<>();
            List<double[]> stsTmp = new ArrayList<>();
            sc.useDelimiter("[ /\n]");
            while (sc.hasNext()) {
                String token = sc.next();
                if (token.equals("vt")) {
                    stsTmp.add(new double[] { sc.nextDouble() * skin.getWidth()
                            , (1 - sc.nextDouble()) * skin.getHeight() } );
                }
                else if (token.equals("f")) {
                    facesTmp.add( new int[] { sc.nextInt() - 1, sc.nextInt() - 1
                      , sc.nextInt() - 1, sc.nextInt() - 1
                      , sc.nextInt() - 1, sc.nextInt() - 1 } );
                }
            }
            faces = facesTmp.toArray(new int[0][0]);
            sts = stsTmp.toArray(new double[0][0]);
        }
    }

    double currentDepth, frame;
    double[] depthBuffer = new double[800 * 600];
    BufferedImage nbimodel 
            = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);

    WritableRaster wr = new WritableRaster(nbimodel.getSampleModel()
                            , new DataBufferInt(800 * 600), new Point()) {
            
        @Override
        public void setDataElements(int x, int y, Object inData) {
            if (currentDepth >= depthBuffer[y * 800 + x]) {
                super.setDataElements(x, y, inData); 
                depthBuffer[y * 800 + x] = currentDepth;
            }
        }
    };
    
    ColorModel cm = ColorModel.getRGBdefault();
    BufferedImage nbi = new BufferedImage(cm, wr, false, null);
    Graphics2D nbig = nbi.createGraphics();
    Polygon polygon = new Polygon(ps[0], ps[1], 3);
    AffineTransform[] ts = { new AffineTransform(), new AffineTransform() };
    
    public void drawAffineTextured3DTriangle(double[][] v) {
        currentDepth = v[0][2] + v[1][2] + v[2][2];
        for (int i = 0; i < 3; i++) {
            ps[0][i] = (int) (400 + 450 * v[i][0] / -v[i][2]);
            ps[1][i] = (int) (300 - 450 * v[i][1] / -v[i][2]); 
        }
        polygon.xpoints = ps[0];
        polygon.ypoints = ps[1];
        ts[0].setTransform(v[0][3] - v[2][3], v[0][4] - v[2][4]
                , v[1][3] - v[2][3], v[1][4] - v[2][4], v[2][3], v[2][4]);
        
        try {
            ts[0].invert();
        } catch (NoninvertibleTransformException ex) { }
        Shape originalClip = nbig.getClip();
        nbig.clip(polygon);
        ts[1].setTransform(ps[0][0] - ps[0][2], ps[1][0] - ps[1][2]
            , ps[0][1] - ps[0][2], ps[1][1] - ps[1][2], ps[0][2], ps[1][2]);
        
        ts[1].concatenate(ts[0]);
        nbig.drawImage(skin, ts[1], null);
        nbig.setClip(originalClip);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        nbig.clearRect(0, 0, 800, 600);
        Arrays.fill(depthBuffer, -Double.MAX_VALUE);
        frame += 0.750;
        if (frame > points.length - 1) frame -= (points.length - 1);
        double p = frame - (int) frame;
        for (int[] face : faces) {
            for (int fv = 0; fv < 3; fv++) {
                for (int i = 0; i < 3; i++) {
                    va[fv][i] = points[(int) frame][face[fv * 2] * 3 + i];
                }
                va[fv][3] = sts[face[fv * 2 + 1]][0];
                va[fv][4] = sts[face[fv * 2 + 1]][1];
            }
            drawAffineTextured3DTriangle(va);
        }
        g.drawImage(nbi, 0, 0, this);
        try {                        // please implement your own
            Thread.sleep(1000 / 120); // more decent game loop later xD
        } catch (InterruptedException ex) { }
        repaint();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("MDD (Pointcache) Animation Test #1");
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new View());
            frame.setVisible(true);
        });
    } 
    
}
