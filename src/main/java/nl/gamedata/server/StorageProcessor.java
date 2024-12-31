package nl.gamedata.server;

import java.util.Map;

import org.jooq.DSLContext;

import nl.gamedata.common.SqlUtils;
import nl.gamedata.data.Tables;
import nl.gamedata.data.tables.records.GameRecord;
import nl.gamedata.data.tables.records.GameSessionRecord;
import nl.gamedata.data.tables.records.GameVersionRecord;
import nl.gamedata.data.tables.records.OrganizationGameRecord;
import nl.gamedata.data.tables.records.OrganizationRecord;

/**
 * StorageProcessor takes care of storing the data.
 * <p>
 * Copyright (c) 2024-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://github.com/averbraeck/gamedata-server/LICENSE">GameData project License</a>.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class StorageProcessor
{
    private final ServerData data;

    private final StorageRequestTask task;

    private final Map<String, String> requestMap;

    private GameSessionRecord gameSession;

    private GameVersionRecord gameVersion;

    private GameRecord game;

    private OrganizationRecord organization;

    private OrganizationGameRecord organizationGame;

    public StorageProcessor(final ServerData data, final StorageRequestTask task, final Map<String, String> requestMap)
    {
        super();
        this.data = data;
        this.task = task;
        this.requestMap = requestMap;
    }

    public void store()
    {
        // try to find the data field
        String messageData = this.requestMap.get("data");
        if (messageData == null)
            error("No data element in the request");

        switch (messageData)
        {
            case "mission_event" -> handleMissionEvent();
            case "player_event" -> handlePlayerEvent();
            case "group_event" -> handleGroupEvent();
            case "player_score" -> handlePlayerScore();
            case "group_score" -> handleGroupScore();
            default -> error("Unknown message data type: " + messageData);
        }

    }

    private void handleMissionEvent()
    {
        DSLContext dsl = this.data.getDSL();
        if (!retrieveGameSession())
            return;
    }

    private void handlePlayerEvent()
    {
        DSLContext dsl = this.data.getDSL();
        if (!retrieveGameSession())
            return;
    }

    private void handleGroupEvent()
    {
        DSLContext dsl = this.data.getDSL();
        if (!retrieveGameSession())
            return;
    }

    private void handlePlayerScore()
    {
        DSLContext dsl = this.data.getDSL();
        if (!retrieveGameSession())
            return;
    }

    private void handleGroupScore()
    {
        DSLContext dsl = this.data.getDSL();
        if (!retrieveGameSession())
            return;
    }

    private boolean retrieveGameSession()
    {
        DSLContext dsl = this.data.getDSL();
        if (this.requestMap.containsKey("session_token"))
        {
            String sessionToken = this.requestMap.get("session_token");
            this.gameSession =
                    dsl.selectFrom(Tables.GAME_SESSION).where(Tables.GAME_SESSION.SESSION_TOKEN.eq(sessionToken)).fetchAny();
            if (this.gameSession == null)
            {
                error("session_token in data not found in database: " + sessionToken);
                return false;
            }
            this.gameVersion = SqlUtils.readRecordFromId(this.data, Tables.GAME_VERSION, this.gameSession.getGameVersionId());
            this.game = SqlUtils.readRecordFromId(this.data, Tables.GAME, this.gameVersion.getGameId());
            this.organization = SqlUtils.readRecordFromId(this.data, Tables.ORGANIZATION, this.gameSession.getOrganizationId());
            this.organizationGame =
                    dsl.selectFrom(Tables.ORGANIZATION_GAME).where(Tables.ORGANIZATION_GAME.GAME_ID.eq(this.game.getId())
                            .and(Tables.ORGANIZATION_GAME.ORGANIZATION_ID.eq(this.organization.getId()))).fetchAny();
            if (this.organizationGame == null)
            {
                error("No access record found for organization " + this.organization.getCode() + " for game "
                        + this.game.getCode());
                return false;
            }
            return true;
        }

        if (this.requestMap.containsKey("game_session_code") && this.requestMap.containsKey("game_code")
                && this.requestMap.containsKey("game_version_code") && this.requestMap.containsKey("organization_code"))
        {

        }

        error("RequestMap contains neither 'session_token', nor 'game_session_code' & "
                + "'game_code' & 'game_version_code' & 'organization_code'");
        return false;
    }

    private void error(final String message)
    {
        ErrorHandler.storeError(this.data, this.task, this.requestMap, message);
    }

}
