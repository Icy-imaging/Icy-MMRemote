package plugins.tprovoost.Microscopy.MicroscopeRemote;

import icy.gui.frame.IcyFrameAdapter;
import icy.gui.frame.IcyFrameEvent;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;
import plugins.tprovoost.Microscopy.gui.RemoteFrame;

/**
 * @author Irsath Nguyen
 */
public class MicroscopeRemotePlugin extends MicroscopePlugin
{
    // static instance
    static RemoteFrame instance = null;

    public MicroscopeRemotePlugin()
    {
        if (instance == null)
        {
            // Creation of the frame.
            instance = new RemoteFrame(this);

            // Add a listener on the frame : when the frame is closed
            // the plugin is removed from the GUI plugin
            instance.addFrameListener(new IcyFrameAdapter()
            {
                @Override
                public void icyFrameClosed(IcyFrameEvent e)
                {
                    shutdown();
                }
            });
        }
    }

    @Override
    public void start()
    {
        if (instance != null)
            instance.toFront();
    }

    @Override
    public void shutdown()
    {
        super.shutdown();

        if (instance != null)
            instance.dispose();
        instance = null;
    }
}
