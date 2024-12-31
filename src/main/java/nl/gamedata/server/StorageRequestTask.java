package nl.gamedata.server;

import java.time.LocalDateTime;

/**
 * A StorageRequestTask holds the payload of the event or score and is handled here.
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 * @param requestType containing GET or POST
 * @param contentType the Content-Type string in a POST request
 * @param payload the unaltered payload that has still to be parsed
 * @param timestamp the timestamp of the request
 */
public record StorageRequestTask(String requestType, String contentType, String payload, LocalDateTime timestamp)
{
    public StorageRequestTask(final String requestType, final String contentType, final String payload)
    {
        this(requestType, contentType, payload, LocalDateTime.now());
    }
}
