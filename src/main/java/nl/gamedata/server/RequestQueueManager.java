package nl.gamedata.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * RequestQueueManager.java.
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */

public class RequestQueueManager
{
    private static final BlockingQueue<StorageRequestTask> queue = new LinkedBlockingQueue<>();

    public static void addTask(final StorageRequestTask task)
    {
        queue.offer(task);
    }

    public static StorageRequestTask takeTask() throws InterruptedException
    {
        return queue.take();
    }

    public static boolean isEmpty()
    {
        return queue.isEmpty();
    }

    public static int numberOfTasks()
    {
        return queue.size();
    }
}
