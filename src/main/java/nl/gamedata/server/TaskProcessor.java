package nl.gamedata.server;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * The TaskProcessor is for now a single-threaded process taking care of processing storage tasks from jobs in the queue.
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TaskProcessor
{
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void startProcessing()
    {
        System.out.println("TaskProcessor.startProcessing");
        executor.submit(() ->
        {
            while (true)
            {
                try
                {
                    StorageRequestTask task = RequestQueueManager.takeTask();
                    processTask(task);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private static void processTask(final StorageRequestTask task)
    {
        try
        {
            // turn the request into a Map of keys and values.
            Map<String, String> requestMap = new HashMap<>();
            if ("GET".equals(task.requestType()))
                convertFormTask(task, requestMap);
            else if ("POST".equals(task.requestType()))
            {
                if (task.contentType().toLowerCase().contains("x-www-form-urlencoded"))
                    convertFormTask(task, requestMap);
                else if (task.contentType().toLowerCase().contains("application/json"))
                    convertJsonTask(task, requestMap);
                else if (task.contentType().toLowerCase().contains("application/xml"))
                    convertXmlTask(task, requestMap);
                else
                {
                    // TODO: log request with unknown request type to log file
                    return;
                }
            }
            else
            {
                // TODO: log non-GET/POST request to log file
                return;
            }

            store(task, requestMap);
        }
        catch (Exception e)
        {
            // TODO: log error to log file
            return;
        }
    }

    static void convertFormTask(final StorageRequestTask task, final Map<String, String> requestMap)
    {
        String[] pairs = task.payload().split("&");
        for (String pair : pairs)
        {
            String[] keyValue = pair.split("=", 2);
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8).toLowerCase().strip();
            String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            // TODO: check if key already exists?
            requestMap.put(key, value);
        }
    }

    static void convertJsonTask(final StorageRequestTask task, final Map<String, String> requestMap)
    {
        // Normalize input for outer { } quotes
        String jsonString = task.payload().trim();
        if (!jsonString.startsWith("{") || !jsonString.endsWith("}"))
            jsonString = "{" + jsonString + "}";

        try
        {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext())
            {
                String key = keys.next().toLowerCase().trim();
                String value = jsonObject.optString(key, ""); // Default value is empty if key has no value
                // TODO: check if key already exists?
                requestMap.put(key, value);
            }
        }
        catch (Exception e)
        {
            // TODO: log error
            return;
        }
    }

    static void convertXmlTask(final StorageRequestTask task, final Map<String, String> requestMap)
    {
        try
        {
            Document document = Jsoup.parse(task.payload().toString(), "", org.jsoup.parser.Parser.xmlParser());

            // Extract all direct child elements (ignore root tag like <gamedata>)
            Element root = document.children().first();
            for (Element child : (root != null ? root.children() : document.children()))
            {
                String key = child.tagName().toLowerCase(); // Tag name is the key
                String value = child.text(); // Text content is the value
                requestMap.put(key, value);
            }
        }
        catch (Exception e)
        {
            // TODO: log error
            return;
        }
    }

    static void store(final StorageRequestTask task, final Map<String, String> requestMap)
    {
        System.out.println(requestMap);

        /*-
        HttpSession session = request.getSession();
        ServerData data = new ServerData();
        session.setAttribute("serverData", data);
        try
        {
            data.setDataSource((DataSource) new InitialContext().lookup("/gamedata-server_datasource"));
        }
        catch (NamingException e)
        {
            throw new ServletException(e);
        }
         */
    }

    public static void stopProcessing()
    {
        executor.shutdownNow();
    }

}
