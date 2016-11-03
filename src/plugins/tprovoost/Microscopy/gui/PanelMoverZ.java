package plugins.tprovoost.Microscopy.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

/**
 * @author Irsath Nguyen
 */
public class PanelMoverZ extends JPanel implements MouseListener, MouseMotionListener, Runnable
{
    /**
     * Generated serial UID
     */
    private static final long serialVersionUID = -5025582239086787935L;

    // CONSTANTS
    private static final int SIZE_PANEL_MOVERZ_W = 50;
    private static final int SIZE_PANEL_MOVERZ_H = 200;

    private static final double BARS_NUMBER = 200;

    private BufferedImage imgZBg = null;
    private BufferedImage imgZBar = null;

    /** Movement Vector */
    int oldY;

    private int startPos = 0;
    private RemoteFrame frame;
    private Thread moveThread;
    private double offZ = 0d;

    public PanelMoverZ(RemoteFrame frame)
    {
        super();

        this.frame = frame;
        setOpaque(true);
        setDoubleBuffered(true);
        setSize(new Dimension(SIZE_PANEL_MOVERZ_W, SIZE_PANEL_MOVERZ_H));
        setPreferredSize(new Dimension(SIZE_PANEL_MOVERZ_W, SIZE_PANEL_MOVERZ_H));
        addMouseListener(this);
        addMouseMotionListener(this);

        imgZBg = frame.plugin.getImageResource(RemoteFrame.currentPath + "remote_backgroundZ.png");
        imgZBar = frame.plugin.getImageResource(RemoteFrame.currentPath + "singleBarZ.png");
        moveThread = new Thread(this, "Remote Z mover");
        moveThread.start();
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(39, 39, 39));
        g2.fillRect(0, 0, w, h);

        if (imgZBg != null && imgZBar != null)
        {
            // draw the background + joystick
            g2.drawImage(imgZBg, 0, 0, w, h, null);
            double ecartNormal = (double) h / 8d;
            double lastPos = h / 2d + startPos;
            for (int i = 0; i < BARS_NUMBER; ++i)
            {
                g2.drawImage(imgZBar, 0, (int) lastPos, w, imgZBar.getHeight(null) * w / imgZBar.getWidth(null), null);
                lastPos = lastPos + ecartNormal / (1d + 0.1 * i * i);
                if (lastPos > h)
                    break;
            }
            lastPos = h / 2d + startPos;
            for (int i = 0; i < BARS_NUMBER; ++i)
            {
                g2.drawImage(imgZBar, 0, (int) lastPos, w, imgZBar.getHeight(null) * w / imgZBar.getWidth(null), null);
                lastPos = lastPos - ecartNormal / (1d + 0.1 * i * i);
                if (lastPos < 0)
                    break;
            }
        }
        g2.dispose();
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        final int movY = e.getY() - oldY;
        if (movY == 0)
            return;

        oldY = e.getY();
        double ecartNormal = getHeight() / 8d / 2d;
        if (movY > 0)
        {
            ++startPos;
            if (startPos > ecartNormal)
                startPos = (int) -ecartNormal;
        }
        else
        {
            --startPos;
            if (startPos < -ecartNormal)
                startPos = (int) ecartNormal;
        }
        final int percent = frame._sliderSpeed.getValue();

        synchronized (this)
        {
            offZ += movY * 0.1 * percent * percent;
        }

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
    public void mousePressed(MouseEvent e)
    {
        oldY = e.getY();
        setCursor(new Cursor(Cursor.N_RESIZE_CURSOR | Cursor.S_RESIZE_CURSOR));
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    @Override
    public void run()
    {
        while (!moveThread.isInterrupted())
        {
            final double off;

            synchronized (this)
            {
                off = offZ;
                offZ = 0d;
            }

            if (off != 0d)
            {
                try
                {
                    StageMover.moveZRelative(off, false);
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
    }

    public void shutdown()
    {
        // end process
        moveThread.interrupt();
    }
}
