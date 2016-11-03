package plugins.tprovoost.Microscopy.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.image.ImageUtil;
import icy.network.NetworkUtil;
import icy.resource.icon.IcyIcon;
import icy.type.point.Point3D;
import icy.util.StringUtil;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover.StageListener;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;
import plugins.tprovoost.Microscopy.MicroscopeRemote.MicroscopeRemotePlugin;

/**
 * This class is the core of the Remote Plugin. Three different threads are
 * running while using this class:
 * <ul>
 * <li>Refreshing the coordinates.</li>
 * <li>Moving the XY Stage</li>
 * <li>Moving the Z Stage.</li>
 * </ul>
 * 
 * @author Thomas Provoost
 */
public class RemoteFrame extends IcyFrame implements StageListener
{
    // -------
    // GUI
    // -------
    MicroscopePlugin plugin;
    public JSlider _sliderSpeed;
    private JLabel _lblX;
    private JLabel _lblY;
    private JLabel _lblZ;
    private InverterCheckBox _cbInvertX;
    private InverterCheckBox _cbInvertY;
    private InverterCheckBox _cbInvertZ;

    // --------
    // IMAGES
    // --------
    public Color transparentColor = new Color(255, 255, 255, 0);
    private BufferedImage imgRemoteBg = null;
    private BufferedImage imgSliderKnob = null;
    private BufferedImage imgMemBtnOn = null;
    private BufferedImage imgMemBtnOff = null;
    private BufferedImage imgInvertSwitchOn = null;
    private BufferedImage imgInvertSwitchOff = null;
    private BufferedImage imgInvertLightOn = null;
    private BufferedImage imgInvertLightOff = null;

    // CONSTANTS
    public final static String currentPath = "plugins/tprovoost/Microscopy/images/";

    // -----------
    // PREFERENCES
    // -----------
    private Preferences _prefs;
    private PanelMoverXY panelMoverXY;
    private PanelMoverZ panelMoverZ;
    private static final String REMOTE = "prefs_remote";
    private static final String SPEED = "speed";

    public RemoteFrame(MicroscopeRemotePlugin plugin)
    {
        super("Remote", false, true, false, true);

        this.plugin = plugin;

        // LOAD ALL IMAGES
        imgRemoteBg = plugin.getImageResource(currentPath + "RemoteFull_2.png");
        imgMemBtnOn = plugin.getImageResource(currentPath + "memoryOn.png");
        imgMemBtnOff = plugin.getImageResource(currentPath + "memoryOff.png");
        imgInvertSwitchOn = plugin.getImageResource(currentPath + "btn_switchOn.png");
        imgInvertSwitchOff = plugin.getImageResource(currentPath + "btn_switchOff.png");
        imgInvertLightOn = plugin.getImageResource(currentPath + "btnRound.png");
        imgInvertLightOff = plugin.getImageResource(currentPath + "btnRound_off.png");
        imgSliderKnob = plugin.getImageResource(currentPath + "knob.png");

        initializeGui();
        setVisible(true);
        addToDesktopPane();
        requestFocus();
        loadPreferences();
        StageMover.addListener(this);
    }

    private void initializeGui()
    {
        // -------------
        // MOUSE MOVER
        // ------------
        panelMoverXY = new PanelMoverXY(RemoteFrame.this);
        panelMoverZ = new PanelMoverZ(RemoteFrame.this);

        JPanel panelMover = GuiUtil.generatePanel();
        panelMover.setLayout(new BoxLayout(panelMover, BoxLayout.X_AXIS));
        panelMover.add(panelMoverXY);
        panelMover.add(Box.createRigidArea(new Dimension(20, 10)));
        panelMover.add(panelMoverZ);
        panelMover.setOpaque(false);
        panelMover.setBackground(transparentColor);

        // ---------
        // SPEED
        // ---------
        JPanel panel_speed = GuiUtil.generatePanel();
        panel_speed.setLayout(new BoxLayout(panel_speed, BoxLayout.X_AXIS));
        panel_speed.setOpaque(false);

        _sliderSpeed = new JSlider(1, 10, 1)
        {
            private static final long serialVersionUID = 1L;
            private BufferedImage toDraw;
            int heightKnob;

            {
                heightKnob = (int) (imgSliderKnob.getHeight(null) / 1.5);
                toDraw = ImageUtil.scale(imgSliderKnob, heightKnob * 2, heightKnob);
            }

            @Override
            protected void paintComponent(Graphics g)
            {
                ((Graphics2D) g).drawImage(toDraw, null, (int) ((getValue() - 1) * (getWidth() / getMaximum())),
                        getHeight() / 2 - heightKnob / 2);
            }
        };
        _sliderSpeed.setOpaque(false);
        final JLabel lbl_value = new JLabel(" " + _sliderSpeed.getValue());

        _sliderSpeed.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent changeevent)
            {
                lbl_value.setText("" + _sliderSpeed.getValue());
            }
        });

        panel_speed.add(_sliderSpeed);
        panel_speed.add(Box.createHorizontalGlue());
        panel_speed.add(lbl_value);
        panel_speed.add(Box.createHorizontalGlue());

        // -------------------
        // INVERT CHEBKBOXES
        // -------------------
        _cbInvertX = new InverterCheckBox("Invert X-Axis");
        _cbInvertX.setImages(imgInvertSwitchOn, imgInvertSwitchOff, imgInvertLightOn, imgInvertLightOff);
        _cbInvertX.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                StageMover.setInvertX(_cbInvertX.isSelected());
            }
        });

        _cbInvertY = new InverterCheckBox("Invert Y-Axis");
        _cbInvertY.setImages(imgInvertSwitchOn, imgInvertSwitchOff, imgInvertLightOn, imgInvertLightOff);
        _cbInvertY.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                StageMover.setInvertY(_cbInvertY.isSelected());
            }
        });

        _cbInvertZ = new InverterCheckBox("Invert Z-Axis");
        _cbInvertZ.setImages(imgInvertSwitchOn, imgInvertSwitchOff, imgInvertLightOn, imgInvertLightOff);
        _cbInvertZ.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                StageMover.setInvertZ(_cbInvertZ.isSelected());
            }
        });

        JPanel panelInvert = GuiUtil.generatePanel();
        panelInvert.setOpaque(false);
        panelInvert.setLayout(new GridLayout(3, 1));
        panelInvert.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        panelInvert.add(_cbInvertX);
        panelInvert.add(_cbInvertY);
        panelInvert.add(_cbInvertZ);

        // MEMORY BUTTONS
        MemoryButton btnM1 = new MemoryButton("M1", imgMemBtnOn, imgMemBtnOff);
        MemoryButton btnM2 = new MemoryButton("M2", imgMemBtnOn, imgMemBtnOff);
        MemoryButton btnM3 = new MemoryButton("M3", imgMemBtnOn, imgMemBtnOff);
        MemoryButton btnM4 = new MemoryButton("M4", imgMemBtnOn, imgMemBtnOff);

        JButton btnHelp = new JButton(new IcyIcon(imgMemBtnOff))
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(Graphics g)
            {
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(imgMemBtnOff, 0, 0, w, h, null);
                g2.setFont(new Font("Arial", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString("?", getWidth() / 2 - fm.charWidth('?') / 2, getHeight() / 2 + fm.getHeight() / 3);
            }
        };
        btnHelp.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionevent)
            {
                NetworkUtil.openBrowser(plugin.getDescriptor().getWeb());
            }
        });

        JPanel panelMemoryButtons = new JPanel(new GridLayout(1, 4));
        panelMemoryButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelMemoryButtons.setOpaque(false);
        panelMemoryButtons.setPreferredSize(new Dimension(60, 40));
        panelMemoryButtons.add(btnM1);
        panelMemoryButtons.add(btnM2);
        panelMemoryButtons.add(btnM3);
        panelMemoryButtons.add(btnM4);
        panelMemoryButtons.add(btnHelp);

        // -----------
        // COORDINATES
        // ----------
        Point3D.Double pos;
        try
        {
            pos = StageMover.getXYZ();
        }
        catch (Exception e)
        {
            pos = new Point3D.Double();
        }

        _lblX = new JLabel("X: " + StringUtil.toString(pos.x, 2) + " µm");
        _lblY = new JLabel("Y: " + StringUtil.toString(pos.y, 2) + " µm");
        _lblZ = new JLabel("Z: " + StringUtil.toString(pos.z, 2) + " µm");
        _lblX.setHorizontalAlignment(SwingConstants.CENTER);
        _lblY.setHorizontalAlignment(SwingConstants.CENTER);
        _lblZ.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panelCoords = new JPanel(new GridLayout(3, 1));
        panelCoords.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelCoords.setOpaque(false);
        panelCoords.setBackground(transparentColor);
        panelCoords.add(_lblX);
        panelCoords.add(_lblY);
        panelCoords.add(Box.createVerticalGlue());
        panelCoords.add(_lblZ);

        JPanel panelAll = new JPanel()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g)
            {
                checkInverts();
                g.drawImage(imgRemoteBg, 0, 0, getWidth(), getHeight(), null);
            }
        };
        panelAll.setLayout(new BoxLayout(panelAll, BoxLayout.Y_AXIS));
        panelAll.setBackground(Color.BLACK);
        panelAll.add(panelMover);
        panelAll.add(panel_speed);
        panelAll.add(panelMemoryButtons);
        panelAll.add(panelInvert);
        panelAll.add(Box.createVerticalGlue());
        panelAll.add(panelCoords);

        add(panelAll);

        pack();
        validate();
        center();
    }

    /**
     * Load preferences : speed, invertX and invertY.
     */
    private void loadPreferences()
    {
        Preferences root = Preferences.userNodeForPackage(getClass());
        _prefs = root.node(root.absolutePath() + "/" + REMOTE);
        _sliderSpeed.setValue(_prefs.getInt(SPEED, 1));
        checkInverts();
    }

    @Override
    public void stateChanged()
    {
        super.stateChanged();
        pack();
    }

    @Override
    public void onClosed()
    {
        panelMoverXY.shutdown();
        panelMoverZ.shutdown();
        _prefs.putInt(SPEED, _sliderSpeed.getValue());
        StageMover.removeListener(this);
    }

    private void checkInverts()
    {
        _cbInvertX.setSelected(StageMover.isInvertX());
        _cbInvertY.setSelected(StageMover.isInvertY());
        _cbInvertZ.setSelected(StageMover.isInvertZ());
    }

    @Override
    public void onStagePositionChanged(String s, double z)
    {
        _lblZ.setText("Z: " + StringUtil.toString(z, 2) + " µm");
    }

    @Override
    public void onXYStagePositionChanged(String s, double x, double y)
    {
        _lblX.setText("X: " + StringUtil.toString(x, 2) + " µm");
        _lblY.setText("Y: " + StringUtil.toString(y, 2) + " µm");
    }

    @Override
    public void onXYStagePositionChangedRelative(String s, double d, double d1)
    {
    }

    @Override
    public void onStagePositionChangedRelative(String s, double d)
    {
    }
}