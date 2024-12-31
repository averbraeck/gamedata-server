package nl.gamedata.server;

import java.util.Map;

/**
 * ErrorHandler.java.
 * <p>
 * Copyright (c) 2024-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://github.com/averbraeck/gamedata-server/LICENSE">GameData project License</a>.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class ErrorHandler
{
    public static void storeError(final ServerData data, final StorageRequestTask task, final Map<String, String> requestMap,
            final String message)
    {
        System.err.println("ERROR: " + message);
        System.err.println(" task: " + task);
        System.err.println("  map: " + requestMap);

        // TODO: try to store in database...

    }
}
