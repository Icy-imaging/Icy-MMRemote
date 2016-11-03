package plugins.tprovoost.Microscopy.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.type.point.Point3D;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;

public class MemoryButton extends JButton implements MouseListener
{

    /** Default serial UID */
    private static final long serialVersionUID = 1L;

    /**
     * When the button is pressed, the {@link System#nanoTime()} is stored in
     * this variable.
     */
    long datePressed = 0;

    // IMAGES FOR DRAWING
    private BufferedImage imgMemBtnOn = null;
    private BufferedImage imgMemBtnOff = null;

    /** This variable contains the value of the 3D point in this memory button. */
    private Point3D.Double memoryButtonPoint;

    public MemoryButton(String string, BufferedImage imgMemBtnOn, BufferedImage imgMemBtnOff)
    {
        super(string);

        this.imgMemBtnOn = imgMemBtnOn;
        this.imgMemBtnOff = imgMemBtnOff;
        setOpaque(true);
        addMouseListener(this);
    }

    private void forgetPoint()
    {
        memoryButtonPoint = null;
        setSelected(false);
    }

    private void rememberPoint() throws Exception
    {
        Point3D.Double xyz = StageMover.getXYZ();
        if (memoryButtonPoint == null)
            memoryButtonPoint = xyz;
        else
            memoryButtonPoint.setLocation(xyz);
        setSelected(true);
    }

    private void gotoPoint() throws Exception
    {
        if (memoryButtonPoint != null)
        {
            StageMover.moveXYAbsolute(memoryButtonPoint.x, memoryButtonPoint.y);
            StageMover.moveZAbsolute(memoryButtonPoint.z);
        }
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
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (e.isControlDown())
            forgetPoint();
        else
            datePressed = System.nanoTime();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // quick press ?
        if (System.nanoTime() - datePressed < 1500000000L)
        {
            try
            {
                gotoPoint();
            }
            catch (Exception e1)
            {
                new AnnounceFrame("Error while going to the saved point.");
            }
        }
        else
        {
            // long press
            try
            {
                rememberPoint();
            }
            catch (Exception e1)
            {
                new FailedAnnounceFrame("Failed to save position, please try again", 3);
            }
        }
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        boolean selected = isSelected();
        int width = getWidth();
        int height = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected)
        {
            g2.drawImage(imgMemBtnOn, 0, 0, width, height, null);
            g2.setColor(Color.black);

        }
        else
        {
            g2.drawImage(imgMemBtnOff, 0, 0, width, height, null);
            g2.setColor(Color.LIGHT_GRAY);

        }

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        String toDisplay = getText();
        g2.drawString(toDisplay, width / 2 - fm.charsWidth(toDisplay.toCharArray(), 0, toDisplay.length()) / 2,
                height / 2 + fm.getHeight() / 3);
        g2.dispose();
    }
}