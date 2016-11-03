package plugins.tprovoost.Microscopy.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

/**
 * @author Irsath Nguyen
 */
public class PanelMoverXY extends JPanel implements MouseListener, MouseMotionListener, Runnable
{
   
    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = -5025582239086787935L;

    private BufferedImage imgXYBg = null;
    private static final int SIZE_PANEL_MOVER = 200;

    /** Movement Vector */
    private Point2D vector;
    private RemoteFrame frame;
    private Thread moveThread;

    public PanelMoverXY(RemoteFrame frame)
    {
        super();

        this.frame = frame;
        vector = new Point2D.Double(0, 0);
        setOpaque(true);
        setDoubleBuffered(true);
        setSize(new Dimension(SIZE_PANEL_MOVER, SIZE_PANEL_MOVER));
        setPreferredSize(new Dimension(SIZE_PANEL_MOVER, SIZE_PANEL_MOVER));
        addMouseListener(this);
        addMouseMotionListener(this);

        imgXYBg = frame.plugin.getImageResource(RemoteFrame.currentPath + "remote_backgroundXY.png");
        moveThread = new Thread(this, "Remote XY mover");
        moveThread.start();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        AffineTransform at;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(39, 39, 39));
        g2.fillRect(0, 0, w, h);

        if (imgXYBg != null)
        {
            g2.drawImage(imgXYBg, 0, 0, w, h, null);
            int stickBallDiameter = w / 5;
            Point2D centerBall = new Point2D.Double(w / 2d + vector.getX(), h / 2d + vector.getY());
            g2.setColor(Color.darkGray);
            double normVector = norm(vector);
            if (vector.getX() != 0 || vector.getY() != 0)
            {
                g2.setColor(Color.blue);
                // ----------
                // draw stick
                // ----------
                Graphics2D g3 = (Graphics2D) g2.create();
                at = AffineTransform.getTranslateInstance(centerBall.getX(), centerBall.getY());
                at.rotate(-vector.getX(), -vector.getY());
                at.translate(0, -stickBallDiameter / 4);
                g3.transform(at);
                g3.setPaint(new GradientPaint(new Point2D.Double(0, 0), Color.BLACK, new Point2D.Double(0,
                        (getWidth() / 5) / 4), Color.LIGHT_GRAY, true));
                g3.fillRoundRect(0, 0, (int) normVector, stickBallDiameter / 2, stickBallDiameter / 2,
                        stickBallDiameter / 2);
                g3.dispose();
            }
            // ---------
            // draw ball
            // ---------
            Paint defaultPaint = g2.getPaint();
            Point2D centerGradient = new Point2D.Double(centerBall.getX(), centerBall.getY());
            float radiusGradient = stickBallDiameter / 2;
            Point2D focusSpotLightGradient;
            if (Math.abs(vector.getX()) <= 1 && Math.abs(vector.getY()) <= 1)
            {
                focusSpotLightGradient = new Point2D.Double(centerBall.getX(), centerBall.getY());
            }
            else
            {
                focusSpotLightGradient = new Point2D.Double(centerBall.getX() + vector.getX() * (radiusGradient - 5)
                        / normVector, centerBall.getY() + vector.getY() / normVector * (radiusGradient - 5));
            }
            float[] dist = {0.1f, 0.3f, 1.0f};
            Color[] colors = {new Color(0.9f, 0.9f, 0.9f), Color.LIGHT_GRAY, Color.DARK_GRAY};
            RadialGradientPaint p = new RadialGradientPaint(centerGradient, radiusGradient, focusSpotLightGradient,
                    dist, colors, CycleMethod.NO_CYCLE);
            g2.setPaint(p);
            g2.fillOval((int) centerBall.getX() - stickBallDiameter / 2, (int) centerBall.getY() - stickBallDiameter
                    / 2, stickBallDiameter, stickBallDiameter);
            g2.setPaint(defaultPaint);
            g2.setColor(Color.BLACK);
            g2.drawOval((int) centerBall.getX() - stickBallDiameter / 2, (int) centerBall.getY() - stickBallDiameter
                    / 2, stickBallDiameter, stickBallDiameter);
        }
        g2.dispose();
    }

    public void applyMovementXY() throws Exception
    {
        double vx = vector.getX();
        double vy = vector.getY();
        if (vx == 0 && vy == 0)// no movement, do not send a signal
            return;
        
        double normV = norm(vector);
        double x = vx / normV;
        double y = vy / normV;
        
        double movementX;
        double movementY;
        double percent = norm(vector) * frame._sliderSpeed.getValue();
        double pxSize = MicroManager.getPixelSize();
        
        if (pxSize == 0d)
        {
            movementX = x * 0.001 * percent * percent;
            movementY = y * 0.001 * percent * percent;
        }
        else
        {
            movementX = x * 0.001 * pxSize * percent * percent;
            movementY = y * 0.001 * pxSize * percent * percent;
        }

        StageMover.moveXYRelative(movementX, movementY, true);
    }

    private double norm(Point2D vector)
    {
        double x = vector.getX();
        double y = vector.getY();
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        int width = getWidth();
        int height = getHeight();

        if (x < 0)
            x = 0;
        if (x > width)
            x = width;

        if (y < 0)
            y = 0;
        if (y > height)
            y = height;
        vector.setLocation(x - width / 2, y - height / 2);
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        vector.setLocation(e.getX() - (getWidth() / 2), e.getY() - (getHeight() / 2));
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        vector.setLocation(0, 0);
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }
    
    @Override
    public void run()
    {
        while (!moveThread.isInterrupted())
        {
            try
            {
                applyMovementXY();
                Thread.sleep(1);
            }
            catch (InterruptedException e)
            {
                moveThread.interrupt();
            }
            catch (Exception e)
            {
                System.err.println(e.getMessage());
            }
        }
    }

    public void shutdown()
    {
        // end process
        moveThread.interrupt();
    }
}
