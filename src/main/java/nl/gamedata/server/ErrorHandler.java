package nl.gamedata.server;

import java.util.Map;

import nl.gamedata.data.Tables;
import nl.gamedata.data.tables.records.ErrorRecord;

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
        storeError(data, task, requestMap, message, "ERROR", false);
    }

    public static void storeWarning(final ServerData data, final StorageRequestTask task, final Map<String, String> requestMap,
            final String message)
    {
        storeError(data, task, requestMap, message, "WARNING", true);
    }

    public static void storeError(final ServerData data, final StorageRequestTask task, final Map<String, String> requestMap,
            final String message, final String errorType, final boolean recordStored)
    {
        System.err.println("ERROR: " + message);
        System.err.println(" task: " + task);
        System.err.println("  map: " + requestMap);

        // store in database
        ErrorRecord error = Tables.ERROR.newRecord();
        error.setTimestamp(task.timestamp());
        error.setErrorType(errorType);
        error.setRecordStored(recordStored ? (byte) 1 : (byte) 0);
        error.setMessage(message);
        error.setContent(task.payload());
        if (requestMap.containsKey("data"))
            error.setDataType(requestMap.get("data"));
        if (requestMap.containsKey("session_token"))
            error.setSessionToken(requestMap.get("session_token"));
        if (requestMap.containsKey("game_session_code"))
            error.setGameSessionCode(requestMap.get("game_session_code"));
        if (requestMap.containsKey("game_version_code"))
            error.setGameVersionCode(requestMap.get("game_version_code"));
        if (requestMap.containsKey("organization_code"))
            error.setOrganizationCode(requestMap.get("organization_code"));
        error.store();
    }
}
