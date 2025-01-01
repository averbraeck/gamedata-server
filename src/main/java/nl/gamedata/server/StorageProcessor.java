package nl.gamedata.server;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.jooq.DSLContext;

import nl.gamedata.common.SqlUtils;
import nl.gamedata.data.Tables;
import nl.gamedata.data.tables.records.GameMissionRecord;
import nl.gamedata.data.tables.records.GameRecord;
import nl.gamedata.data.tables.records.GameSessionRecord;
import nl.gamedata.data.tables.records.GameVersionRecord;
import nl.gamedata.data.tables.records.GroupAttemptRecord;
import nl.gamedata.data.tables.records.GroupEventRecord;
import nl.gamedata.data.tables.records.GroupObjectiveRecord;
import nl.gamedata.data.tables.records.GroupRecord;
import nl.gamedata.data.tables.records.GroupRoleRecord;
import nl.gamedata.data.tables.records.GroupScoreRecord;
import nl.gamedata.data.tables.records.LearningGoalRecord;
import nl.gamedata.data.tables.records.MissionEventRecord;
import nl.gamedata.data.tables.records.OrganizationGameRecord;
import nl.gamedata.data.tables.records.OrganizationRecord;
import nl.gamedata.data.tables.records.PlayerAttemptRecord;
import nl.gamedata.data.tables.records.PlayerEventRecord;
import nl.gamedata.data.tables.records.PlayerObjectiveRecord;
import nl.gamedata.data.tables.records.PlayerRecord;
import nl.gamedata.data.tables.records.PlayerScoreRecord;
import nl.gamedata.data.tables.records.ScaleRecord;

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

    private GameMissionRecord gameMission;

    private PlayerRecord player;

    private PlayerAttemptRecord playerAttempt;

    private GroupRecord group;

    private GroupAttemptRecord groupAttempt;

    private GroupRoleRecord groupRole;

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

        if (!retrieveGameSession())
            return;
        if (!checkTokens())
            return;
        if (!checkMission())
            return;

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
        MissionEventRecord missionEvent = Tables.MISSION_EVENT.newRecord();
        missionEvent.setGameSessionId(this.gameSession.getId());
        missionEvent.setGameMissionId(this.gameMission.getId());
        missionEvent.setType(parseString("type", false, "string"));
        String key = parseString("key", true);
        if (key == null)
            return;
        missionEvent.setKey(key);
        String value = parseString("value", true);
        if (value == null)
            return;
        missionEvent.setValue(value);
        missionEvent.setTimestamp(parseDateTime("timestamp", false, this.task.timestamp()));
        missionEvent.setStatus(parseString("status", false, null));
        missionEvent.setRound(parseString("round", false, null));
        missionEvent.setGameTime(parseString("game_time", false, null));
        missionEvent.setGroupingCode(parseString("grouping_code", false, null));
        missionEvent.setFacilitatorInitiated(parseBoolean("facilitator_initiated", false, (byte) 0));
        missionEvent.store();
    }

    private void handlePlayerEvent()
    {
        if (!retrievePlayer())
            return;
        PlayerEventRecord playerEvent = Tables.PLAYER_EVENT.newRecord();
        playerEvent.setPlayerAttemptId(this.playerAttempt.getId());
        playerEvent.setType(parseString("type", false, "string"));
        String key = parseString("key", true);
        if (key == null)
            return;
        playerEvent.setKey(key);
        String value = parseString("value", true);
        if (value == null)
            return;
        playerEvent.setValue(value);
        playerEvent.setTimestamp(parseDateTime("timestamp", false, this.task.timestamp()));
        playerEvent.setStatus(parseString("status", false, null));
        playerEvent.setRound(parseString("round", false, null));
        playerEvent.setGameTime(parseString("game_time", false, null));
        playerEvent.setGroupingCode(parseString("grouping_code", false, null));
        playerEvent.setPlayerInitiated(parseBoolean("player_initiated", false, (byte) 0));
        playerEvent.store();
    }

    private void handleGroupEvent()
    {
        if (!retrieveGroup())
            return;
        GroupEventRecord groupEvent = Tables.GROUP_EVENT.newRecord();
        groupEvent.setGroupAttemptId(this.groupAttempt.getId());
        groupEvent.setType(parseString("type", false, "string"));
        String key = parseString("key", true);
        if (key == null)
            return;
        groupEvent.setKey(key);
        String value = parseString("value", true);
        if (value == null)
            return;
        groupEvent.setValue(value);
        groupEvent.setTimestamp(parseDateTime("timestamp", false, this.task.timestamp()));
        groupEvent.setStatus(parseString("status", false, null));
        groupEvent.setRound(parseString("round", false, null));
        groupEvent.setGameTime(parseString("game_time", false, null));
        groupEvent.setGroupingCode(parseString("grouping_code", false, null));
        groupEvent.setGroupInitiated(parseBoolean("group_initiated", false, (byte) 0));
        groupEvent.store();
    }

    private void handlePlayerScore()
    {
        if (!retrievePlayer())
            return;
        PlayerScoreRecord playerScore = Tables.PLAYER_SCORE.newRecord();
        RecordId playerObjectiveId = retrievePlayerObjectiveId(false);
        if (!playerObjectiveId.ok())
            return;
        playerScore.setPlayerObjectiveId(playerObjectiveId.id());
        RecordId scaleId = retrieveScaleId(false);
        if (!scaleId.ok())
            return;
        playerScore.setScaleId(scaleId.id());
        playerScore.setPlayerAttemptId(this.playerAttempt.getId());
        String scoreType = parseString("score_type", true);
        if (scoreType == null)
            return;
        playerScore.setScoreType(scoreType);
        Double delta = parseDouble("delta", false, Double.NaN);
        if (delta == null)
            return;
        playerScore.setDelta(delta.isNaN() ? null : delta);
        Double newScoreNumber = parseDouble("new_score_number", false, Double.NaN);
        if (newScoreNumber == null)
            return;
        playerScore.setNewScoreNumber(newScoreNumber.isNaN() ? null : newScoreNumber);
        playerScore.setNewScoreString(parseString("new_score_string", false, null));
        playerScore.setTimestamp(parseDateTime("timestamp", false, this.task.timestamp()));
        playerScore.setFinalScore(parseBoolean("final_score", false, (byte) 0));
        playerScore.setStatus(parseString("status", false, null));
        playerScore.setRound(parseString("round", false, null));
        playerScore.setGameTime(parseString("game_time", false, null));
        playerScore.setGroupingCode(parseString("grouping_code", false, null));
        playerScore.store();
    }

    private void handleGroupScore()
    {
        if (!retrieveGroup())
            return;
        GroupScoreRecord groupScore = Tables.GROUP_SCORE.newRecord();
        RecordId groupObjectiveId = retrieveGroupObjectiveId(false);
        if (!groupObjectiveId.ok())
            return;
        groupScore.setGroupObjectiveId(groupObjectiveId.id());
        RecordId scaleId = retrieveScaleId(false);
        if (!scaleId.ok())
            return;
        groupScore.setScaleId(scaleId.id());
        groupScore.setGroupAttemptId(this.groupAttempt.getId());
        String scoreType = parseString("score_type", true);
        if (scoreType == null)
            return;
        groupScore.setScoreType(scoreType);
        Double delta = parseDouble("delta", false, Double.NaN);
        if (delta == null)
            return;
        groupScore.setDelta(delta.isNaN() ? null : delta);
        Double newScoreNumber = parseDouble("new_score_number", false, Double.NaN);
        if (newScoreNumber == null)
            return;
        groupScore.setNewScoreNumber(newScoreNumber.isNaN() ? null : newScoreNumber);
        groupScore.setNewScoreString(parseString("new_score_string", false, null));
        groupScore.setTimestamp(parseDateTime("timestamp", false, this.task.timestamp()));
        groupScore.setFinalScore(parseBoolean("final_score", false, (byte) 0));
        groupScore.setStatus(parseString("status", false, null));
        groupScore.setRound(parseString("round", false, null));
        groupScore.setGameTime(parseString("game_time", false, null));
        groupScore.setGroupingCode(parseString("grouping_code", false, null));
        groupScore.store();
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
            String gameSessionCode = this.requestMap.get("game_session_code");
            String gameCode = this.requestMap.get("game_code");
            String gameVersionCode = this.requestMap.get("game_version_code");
            String organizationCode = this.requestMap.get("organization_code");
            this.game = dsl.selectFrom(Tables.GAME).where(Tables.GAME.CODE.eq(gameCode)).fetchAny();
            if (this.game == null)
            {
                error("No record found for game " + gameCode);
                return false;
            }
            this.gameVersion = dsl.selectFrom(Tables.GAME_VERSION)
                    .where(Tables.GAME_VERSION.CODE.eq(gameVersionCode).and(Tables.GAME_VERSION.GAME_ID.eq(this.game.getId())))
                    .fetchAny();
            if (this.gameVersion == null)
            {
                error("No record found for gameVersion " + gameVersionCode + " for game " + gameCode);
                return false;
            }
            this.organization =
                    dsl.selectFrom(Tables.ORGANIZATION).where(Tables.ORGANIZATION.CODE.eq(organizationCode)).fetchAny();
            if (this.organization == null)
            {
                error("No record found for organization " + organizationCode);
                return false;
            }
            this.organizationGame =
                    dsl.selectFrom(Tables.ORGANIZATION_GAME).where(Tables.ORGANIZATION_GAME.GAME_ID.eq(this.game.getId())
                            .and(Tables.ORGANIZATION_GAME.ORGANIZATION_ID.eq(this.organization.getId()))).fetchAny();
            if (this.organizationGame == null)
            {
                error("No access record found for organization " + this.organization.getCode() + " for game "
                        + this.game.getCode());
                return false;
            }
            this.gameSession = dsl.selectFrom(Tables.GAME_SESSION)
                    .where(Tables.GAME_SESSION.CODE.eq(gameSessionCode)
                            .and(Tables.GAME_SESSION.GAME_VERSION_ID.eq(this.gameVersion.getId()))
                            .and(Tables.GAME_SESSION.ORGANIZATION_ID.eq(this.organization.getId())))
                    .fetchAny();
            if (this.gameSession != null && this.gameSession.getTokenForced() != 0)
            {
                error("Anonymous access without token for Game Session " + gameSessionCode + " for game " + this.game.getCode()
                        + ", but token is forced");
                return false;
            }
            if (this.gameSession == null && this.organizationGame.getAnonymousSessions() == 0)
            {
                error("Anonymous access without token for Game Session " + gameSessionCode + " for game " + this.game.getCode()
                        + ", but anonymous access is not allowed");
                return false;
            }
            if (this.gameSession == null)
            {
                this.gameSession = Tables.GAME_SESSION.newRecord();
                this.gameSession.setOrganizationId(this.organization.getId());
                this.gameSession.setGameVersionId(this.gameVersion.getId());
                this.gameSession.setCode(gameSessionCode);
                this.gameSession.setName(gameSessionCode);
                this.gameSession.setDescription("Autogenerated");
                this.gameSession.setSessionToken("");
                this.gameSession.setTokenForced((byte) 0);
                this.gameSession.setArchived((byte) 0);
                this.gameSession.setValid((byte) 1);
                this.gameSession.setPlayDate(LocalDate.now());
                this.gameSession.setSessionStatus("");
                this.gameSession.store();
            }
        }

        error("RequestMap contains neither 'session_token', nor 'game_session_code' & "
                + "'game_code' & 'game_version_code' & 'organization_code'");
        return false;
    }

    private boolean checkTokens()
    {
        DSLContext dsl = this.data.getDSL();
        if (this.game.getTokenForced() != 0)
        {
            if (!this.requestMap.containsKey("game_token"))
            {
                error("Field game_token not found for Game Session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode() + ", while game token is forced");
                return false;
            }
            String gameToken = this.requestMap.get("game_token");
            var gt = dsl.selectFrom(Tables.GAME_TOKEN)
                    .where(Tables.GAME_TOKEN.GAME_ID.eq(this.game.getId()).and(Tables.GAME_TOKEN.VALUE.eq(gameToken)))
                    .fetchAny();
            if (gt == null)
            {
                error("Field game_token does not exist in database. Game Session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode() + " (game token is " + gameToken + ")");
                return false;
            }
            if (gt.getWriter() == 0)
            {
                error("Used game_token does not allow write access. Game Session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode() + " (game token is " + gameToken + ")");
                return false;
            }
        }

        if (this.organizationGame.getTokenForced() != 0)
        {
            if (!this.requestMap.containsKey("organization_game_token"))
            {
                error("Field organization_game_token not found for Game Session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode() + ", while organization-game token is forced");
                return false;
            }
            String orgGameToken = this.requestMap.get("organization_game_token");
            var ogt = dsl
                    .selectFrom(Tables.ORGANIZATION_GAME_TOKEN).where(Tables.ORGANIZATION_GAME_TOKEN.ORGANIZATION_GAME_ID
                            .eq(this.organizationGame.getId()).and(Tables.ORGANIZATION_GAME_TOKEN.VALUE.eq(orgGameToken)))
                    .fetchAny();
            if (ogt == null)
            {
                error("Field organization_game_token does not exist in database. Game Session " + this.gameSession.getCode()
                        + " for game " + this.game.getCode() + " (organization_game token is " + orgGameToken + ")");
                return false;
            }
            if (ogt.getWriter() == 0)
            {
                error("organization_Used game_token does not allow write access. Game Session " + this.gameSession.getCode()
                        + " for game " + this.game.getCode() + " (organization_game token is " + orgGameToken + ")");
                return false;
            }
        }
        return true;
    }

    private boolean checkMission()
    {
        DSLContext dsl = this.data.getDSL();
        if (!this.requestMap.containsKey("game_mission"))
        {
            error("No 'game_mission' tag found for game session " + this.gameSession.getCode() + " for game "
                    + this.game.getCode());
            return false;
        }
        String gameMissionCode = this.requestMap.get("game_mission");
        this.gameMission = dsl.selectFrom(Tables.GAME_MISSION).where(Tables.GAME_MISSION.CODE.eq(gameMissionCode)
                .and(Tables.GAME_MISSION.GAME_VERSION_ID.eq(this.gameVersion.getId()))).fetchAny();
        if (this.gameMission == null)
        {
            error("No record found for gameMission " + gameMissionCode + " for game " + this.game.getCode());
            return false;
        }
        return true;
    }

    private boolean retrievePlayer()
    {
        DSLContext dsl = this.data.getDSL();

        // 1. Player
        if (!this.requestMap.containsKey("player_name"))
        {
            error("No 'player_name' tag found for game session " + this.gameSession.getCode() + " for game "
                    + this.game.getCode());
            return false;
        }
        String playerName = this.requestMap.get("player_name");
        this.player = dsl.selectFrom(Tables.PLAYER)
                .where(Tables.PLAYER.NAME.eq(playerName).and(Tables.PLAYER.GAME_SESSION_ID.eq(this.gameSession.getId())))
                .fetchAny();
        if (this.player == null)
        {
            this.player = Tables.PLAYER.newRecord();
            this.player.setName(playerName);
            this.player.setDisplayName(parseString("display_name", false, playerName.substring(0, 45)));
            this.player.setGameSessionId(this.gameSession.getId());
            this.player.store();
        }

        // 2. PlayerAttempt
        Integer playerAttemptNr = parseInt("player_attempt_nr", false, 1);
        this.playerAttempt = dsl.selectFrom(Tables.PLAYER_ATTEMPT)
                .where(Tables.PLAYER_ATTEMPT.PLAYER_ID.eq(this.player.getId())
                        .and(Tables.PLAYER_ATTEMPT.GAME_MISSION_ID.eq(this.gameMission.getId()))
                        .and(Tables.PLAYER_ATTEMPT.ATTEMPT_NR.eq(playerAttemptNr)))
                .fetchAny();
        if (this.playerAttempt == null)
        {
            this.playerAttempt = Tables.PLAYER_ATTEMPT.newRecord();
            this.playerAttempt.setAttemptNr(playerAttemptNr);
            this.playerAttempt.setStatus(parseString("player_attempt_status", false, ""));
            this.playerAttempt.setGameMissionId(this.gameMission.getId());
            this.playerAttempt.setPlayerId(this.player.getId());
            this.playerAttempt.store();
        }

        // 3. Group and GroupRole
        String groupName = parseString("group_name", false, null);
        if (groupName != null)
        {
            String groupRoleName = parseString("group_role", false, "MEMBER");
            this.group = dsl.selectFrom(Tables.GROUP)
                    .where(Tables.GROUP.NAME.eq(groupName).and(Tables.GROUP.GAME_SESSION_ID.eq(this.gameSession.getId())))
                    .fetchAny();
            if (this.group == null)
            {
                this.group = Tables.GROUP.newRecord();
                this.group.setName(groupName);
                this.group.setGameSessionId(this.gameSession.getId());
                this.group.store();
            }
            this.groupRole = dsl.selectFrom(Tables.GROUP_ROLE)
                    .where(Tables.GROUP_ROLE.NAME.eq(groupRoleName).and(Tables.GROUP_ROLE.PLAYER_ID.eq(this.player.getId()))
                            .and(Tables.GROUP_ROLE.GROUP_ID.eq(this.group.getId())))
                    .fetchAny();
            if (this.groupRole == null)
            {
                this.groupRole = Tables.GROUP_ROLE.newRecord();
                this.groupRole.setName(groupRoleName);
                this.groupRole.setPlayerId(this.player.getId());
                this.groupRole.setGroupId(this.group.getId());
                this.groupRole.store();
            }
        }
        return true;
    }

    private boolean retrieveGroup()
    {
        DSLContext dsl = this.data.getDSL();

        // 1. Group
        if (!this.requestMap.containsKey("group_name"))
        {
            error("No 'group_name' tag found for game session " + this.gameSession.getCode() + " for game "
                    + this.game.getCode());
            return false;
        }
        String groupName = this.requestMap.get("group_name");
        this.group = dsl.selectFrom(Tables.GROUP)
                .where(Tables.GROUP.NAME.eq(groupName).and(Tables.GROUP.GAME_SESSION_ID.eq(this.gameSession.getId())))
                .fetchAny();
        if (this.group == null)
        {
            this.group = Tables.GROUP.newRecord();
            this.group.setName(groupName);
            this.group.setGameSessionId(this.gameSession.getId());
            this.group.store();
        }

        // 2. GroupAttempt
        Integer groupAttemptNr = parseInt("group_attempt_nr", false, 1);
        this.groupAttempt = dsl.selectFrom(Tables.GROUP_ATTEMPT)
                .where(Tables.GROUP_ATTEMPT.GROUP_ID.eq(this.group.getId())
                        .and(Tables.GROUP_ATTEMPT.GAME_MISSION_ID.eq(this.gameMission.getId()))
                        .and(Tables.GROUP_ATTEMPT.ATTEMPT_NR.eq(groupAttemptNr)))
                .fetchAny();
        if (this.groupAttempt == null)
        {
            this.groupAttempt = Tables.GROUP_ATTEMPT.newRecord();
            this.groupAttempt.setAttemptNr(groupAttemptNr);
            this.groupAttempt.setStatus(parseString("group_attempt_status", false, ""));
            this.groupAttempt.setGameMissionId(this.gameMission.getId());
            this.groupAttempt.setGroupId(this.group.getId());
            this.groupAttempt.store();
        }
        return true;
    }

    private RecordId retrievePlayerObjectiveId(final boolean required)
    {
        DSLContext dsl = this.data.getDSL();

        // Step 1. check that conditions are fulfilled
        boolean bpo = this.requestMap.containsKey("player_objective");
        boolean blg = this.requestMap.containsKey("learning_goal");
        if (!required && !bpo && !blg)
            return new RecordId(null, true);
        if (required && (!bpo || !blg))
        {
            error("No player_objective or learning_goal found game session " + this.gameSession.getCode() + " for game "
                    + this.game.getCode());
            return new RecordId(null, false);
        }
        if (bpo && !blg)
        {
            error("if player_objective is specified, learning_goal should be specified as well. Game session "
                    + this.gameSession.getCode() + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }
        if (!bpo && blg)
        {
            error("if learning_goal is specified, player_objective should be specified as well. Game session "
                    + this.gameSession.getCode() + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }

        // Step 2. Retrieve learning goal for the game mission
        String lg = this.requestMap.get("learning_goal");
        LearningGoalRecord learningGoal = dsl.selectFrom(Tables.LEARNING_GOAL)
                .where(Tables.LEARNING_GOAL.GAME_MISSION_ID.eq(this.gameMission.getId()).and(Tables.LEARNING_GOAL.CODE.eq(lg)))
                .fetchAny();
        if (learningGoal == null)
        {
            error("No record found for learning goal " + lg + " that belongs to game mission " + this.gameMission.getCode()
                    + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }

        // Step 3. Retrieve the player objective for learning goal
        String po = this.requestMap.get("player_objective");
        PlayerObjectiveRecord playerObjective = dsl.selectFrom(Tables.PLAYER_OBJECTIVE).where(
                Tables.PLAYER_OBJECTIVE.LEARNING_GOAL_ID.eq(learningGoal.getId()).and(Tables.PLAYER_OBJECTIVE.CODE.eq(po)))
                .fetchAny();
        if (playerObjective == null)
        {
            error("No record found for player objective " + po + " that belongs to learning goal " + lg + " for game "
                    + this.game.getCode());
            return new RecordId(null, false);
        }

        return new RecordId(playerObjective.getId(), true);
    }

    private RecordId retrieveGroupObjectiveId(final boolean required)
    {
        DSLContext dsl = this.data.getDSL();

        // Step 1. check that conditions are fulfilled
        boolean bgo = this.requestMap.containsKey("group_objective");
        boolean blg = this.requestMap.containsKey("learning_goal");
        if (!required && !bgo && !blg)
            return new RecordId(null, true);
        if (required && (!bgo || !blg))
        {
            error("No group_objective or learning_goal found for game session " + this.gameSession.getCode() + " for game "
                    + this.game.getCode());
            return new RecordId(null, false);
        }
        if (bgo && !blg)
        {
            error("if group_objective is specified, learning_goal should be specified as well. Game session "
                    + this.gameSession.getCode() + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }
        if (!bgo && blg)
        {
            error("if learning_goal is specified, group_objective should be specified as well. Game session "
                    + this.gameSession.getCode() + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }

        // Step 2. Retrieve learning goal for the game mission
        String lg = this.requestMap.get("learning_goal");
        LearningGoalRecord learningGoal = dsl.selectFrom(Tables.LEARNING_GOAL)
                .where(Tables.LEARNING_GOAL.GAME_MISSION_ID.eq(this.gameMission.getId()).and(Tables.LEARNING_GOAL.CODE.eq(lg)))
                .fetchAny();
        if (learningGoal == null)
        {
            error("No record found for learning goal " + lg + " that belongs to game mission " + this.gameMission.getCode()
                    + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }

        // Step 3. Retrieve the group objective for learning goal
        String go = this.requestMap.get("group_objective");
        GroupObjectiveRecord groupObjective = dsl.selectFrom(Tables.GROUP_OBJECTIVE)
                .where(Tables.GROUP_OBJECTIVE.LEARNING_GOAL_ID.eq(learningGoal.getId()).and(Tables.GROUP_OBJECTIVE.CODE.eq(go)))
                .fetchAny();
        if (groupObjective == null)
        {
            error("No record found for group objective " + go + " that belongs to learning goal " + lg + " for game "
                    + this.game.getCode());
            return new RecordId(null, false);
        }

        return new RecordId(groupObjective.getId(), true);
    }

    private RecordId retrieveScaleId(final boolean required)
    {
        DSLContext dsl = this.data.getDSL();

        // Step 1. check that conditions are fulfilled
        boolean bs = this.requestMap.containsKey("scale_type");
        if (!required && !bs)
            return new RecordId(null, true);
        if (required && !bs)
        {
            error("No scale_type found for game session " + this.gameSession.getCode() + " for game " + this.game.getCode());
            return new RecordId(null, false);
        }

        // Step 2. Retrieve scale for the game
        String scaleType = this.requestMap.get("scale_type");
        ScaleRecord scale = dsl.selectFrom(Tables.SCALE)
                .where(Tables.SCALE.GAME_ID.eq(this.game.getId()).and(Tables.SCALE.TYPE.eq(scaleType))).fetchAny();
        if (scale == null)
        {
            error("No record found for scale " + scaleType + " that belongs to game " + this.game.getCode());
            return new RecordId(null, false);
        }

        return new RecordId(scale.getId(), true);
    }

    private String parseString(final String key, final boolean required)
    {
        return parseString(key, required, null);
    }

    private String parseString(final String key, final boolean required, final String defaultValue)
    {
        if (!this.requestMap.containsKey(key))
        {
            if (required)
            {
                error("No tag " + key + " found for game session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode());
                return null;
            }
            return defaultValue;
        }
        return this.requestMap.get(key);
    }

    private LocalDateTime parseDateTime(final String key, final boolean required, final LocalDateTime defaultValue)
    {
        if (!this.requestMap.containsKey(key))
        {
            if (required)
            {
                error("No tag " + key + " found for game session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode());
                return null;
            }
            return defaultValue;
        }
        String dtString = this.requestMap.get(key);
        try
        {
            LocalDateTime dt = LocalDateTime.parse(dtString);
            return dt;
        }
        catch (Exception e)
        {
            error("DateTime " + dtString + " for key " + key + " not valid for game session " + this.gameSession.getCode()
                    + " for game " + this.game.getCode() + " -- format is ISO-8601, e.g., 2024-12-03T10:15:30");
            return defaultValue;
        }
    }

    private Byte parseBoolean(final String key, final boolean required, final Byte defaultValue)
    {
        if (!this.requestMap.containsKey(key))
        {
            if (required)
            {
                error("No tag " + key + " found for game session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode());
                return null;
            }
            return defaultValue;
        }
        String bString = this.requestMap.get(key);
        try
        {
            Integer b = Integer.parseInt(bString);
            return b == 0 ? (byte) 0 : (byte) 1;
        }
        catch (Exception e)
        {
            error("Byte " + bString + " for key " + key + " not valid for game session " + this.gameSession.getCode()
                    + " for game " + this.game.getCode() + " -- format is ISO-8601, e.g., 2024-12-03T10:15:30");
            return defaultValue;
        }
    }

    private Integer parseInt(final String key, final boolean required, final Integer defaultValue)
    {
        if (!this.requestMap.containsKey(key))
        {
            if (required)
            {
                error("No tag " + key + " found for game session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode());
                return null;
            }
            return defaultValue;
        }
        String iString = this.requestMap.get(key);
        try
        {
            return Integer.parseInt(iString);
        }
        catch (Exception e)
        {
            error("Integer " + iString + " for key " + key + " not valid for game session " + this.gameSession.getCode()
                    + " for game " + this.game.getCode());
            return defaultValue;
        }
    }

    private Double parseDouble(final String key, final boolean required, final Double defaultValue)
    {
        if (!this.requestMap.containsKey(key))
        {
            if (required)
            {
                error("No tag " + key + " found for game session " + this.gameSession.getCode() + " for game "
                        + this.game.getCode());
                return null;
            }
            return defaultValue;
        }
        String dString = this.requestMap.get(key);
        try
        {
            return Double.parseDouble(dString);
        }
        catch (Exception e)
        {
            error("Double " + dString + " for key " + key + " not valid for game session " + this.gameSession.getCode()
                    + " for game " + this.game.getCode());
            return defaultValue;
        }
    }

    private void error(final String message)
    {
        ErrorHandler.storeError(this.data, this.task, this.requestMap, message);
    }

    record RecordId(Integer id, boolean ok)
    {
    }
}
