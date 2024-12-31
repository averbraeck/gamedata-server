package nl.gamedata.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * The AppLifecycleListener is responsible for starting and stopping the TaskProcessor when the ServletContext is initialized or
 * destroyed.
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener
{

    @Override
    public void contextInitialized(final ServletContextEvent sce)
    {
        TaskProcessor.startProcessing();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce)
    {
        TaskProcessor.stopProcessing();
    }
}
