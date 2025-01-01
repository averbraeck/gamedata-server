# API for group event data

The fields for the group event are as follows. Note that the field name is used in lower case for the key in the form-data GET or POST request, as the key in the JSON payload, and as the tag name in the XML payload. All keys are in lower case and use 'snaking' with underscores as indicated in the table.

| key           | required? | type | explanation         |
| ------------- | --------- | ---- | ------------------- |
| `data`          | required | string | Value should be `group_event`, lower case with underscore. |
| `session_token` | required | string(45) | Token to identify the game session, game version, game, and organization responsible for playing the game. See below for anonymous sessions that do not use a token, and where a session might be created on-the-fly. |
| `organization_game_token` | depends | string(255) | Token that might be used by the organization to limit access in writing data to the database for the game. This value will only be checked if `organization_game.token_forced` is equal to `true` (`1`). When the token is forced, and it cannot be found for the provided game and organization, the record is not written to the database. When the token is not forced, it is not checked, and it does not need to be provided. |
| `game_token` | depends | string(255) | Token that might be used by the game admin to limit access in writing data to the database for the game. This value will only be checked if `game.token_forced` is equal to `true` (`1`). When the token is forced, and it cannot be found for the provided game, the record is not written to the database. When the token is not forced, it is not checked, and it does not need to be provided. |
| `game_mission`  | required | string(16) | Code of the game mission for which this is an event. Every game has at least one mission. The mission code will be looked up for the game that is implicitly encoded in the `session_token`. |
| `group_attempt_nr`  | optional | int | The attempt number in case the group can do multiple attempts at the game mission. If the `group_attempt_nr` is not provided, it will automatically get a value of 1. |
| `group_attempt_status`  | optional | string(45) | Optional description of the attempt in case the group can do multiple attempts at the game mission. An example is a "REGULAR" attempt (attempt nr 1) and a "RETRY" (attempt nr 2). If the `group_attempt_status` is missing, it will remain blank. |
| `group_name` | required | string(45) | The name of the group. This could be a generated group name (fixed groups in the game) or a self-chosen name by a group of players. Most important is that it is **unique** within the game session. Results per group will be clustered using the `group_name`. |
| `type`          | optional | string(45) | The type of data being sent. When not included, `string` is assumed. See below for a table with types. |
| `key`           | required | string(45) | The key of maximum 45 characters. Although the key can contain any characters, keys typically do not have spaces and consist of ASCII characters. |
| `value`        | required | text(65535) | The value belonging to the key, of the appropriate type. The value can be a multi-line string. Max 65,535 characters. |
| `timestamp`   | don't use | timestamp | The timestamp is normally inserted by the server at the moment of receiving the data, and stored in UTC time. In case you would want to override the timestamp, it can be provided in the data. In that case, the server will not allocate it's own timestamp. |
| `status`      | optional | string(45) | If the data sent is linked to some status, it can be provided in this field. Max 45 characters. |
| `round`       | optional | string(16) | If the data sent is linked to a round, it can be provided in this field. Rounds can be a number, but also text, e.g., `Practice-1`. It is always stored as a text field in the database. The maximum length is 16 characters. |
| `game_time`   | optional | string(45) | Many games keep their own clock that is different from the wall clock. This can be measured in many different ways, e.g., seconds since the start, or years in an environmental game. The `game_time` can be 45 characters, and it is always stored as a text field in the database. |
| `grouping_code` | optional | string(45) | The optional grouping code of max 45 characters can be used to filter data using different headings. This can help in the data analysis. A `grouping_code` could, for instance, be `error` where session events store exceptions in the execution of the game. |
| `group_initiated` | optional | boolean | Indicates whether the event was initiated by an action of the group (`true`) or an autonomous event by the game (`false`). The default value is `false`. | 



### Anonymous game session data
If a particular Game Session does not exists yet, and the organization allows for the game to have anonymous sessions (`organization_game.anonymous_sessions` is equal to `1`), data can be logged in an 'anonymous' session. Instead of the session_token, the following data needs to be provided:

| key           | required? | type | explanation         |
| ------------- | --------- | ---- | ------------------- |
| `data`          | required | string | Value should be `group_event`, lower case with underscore. |
| `game_session_code` | required | string(16) | Code to identify the game session. Make sure it is unique for the game - organization combination. |
| `game_code` | required | string(20) | Code to identify the game. This record needs to exist in the database. |
| `game_version_code` | required | string(16) | Code to identify the game-version within the game. This record needs to exist in the database. |
| `organization_code` | required | string(16) | Code to identify the organization. This record needs to exist in the database. |

When a record with the `session_code` already exists (with the correct `game_version` and `organization`), the data will be stored. When the `game_session` does not exist yet, a number of checks are carried out:

- does the game with `game_code` exist?
- does the game version identified by `game_version_code` exist?
- does the organization with `organization_code` exist?
- does the organization have access to the game? I.e., does the record `organization_game` exist for the game-organization combination?
- is the value of `organization_game.anonymous_sessions` equal to `1` (true)?

When all checks pass, the system will create a `game_session` record in the database, linking it to the `organization` and `game_version` that were provided. The `name` of the session will be equal to the `code` (this can be edited later). The value of `valid` will be set to `true`. The value of `play_date` will be set to the current date. 



### Allowed values for `type`

| type       | explanation | 
| ---------- | ----------- |
| `string`     | Characters. Single-line. utf-8 encoding is used. |
| `text`       | Characters. Can be multi-line. utf-8 encoding is used. |
| `integer`    | Integer value. Can be positive or negative. A leading `+` sign is not allowed. |
| `float`      | Floating-point value. Use a decimal point (no decimal comma, no thousands separators). Scientific notation is allowed, with an `E` or `e` for the exponent. An example is `6.24E+7` or `6.24E7`. The use of values `NaN`, `Inf` and `-Inf` are allowed. A leading `+` sign is not allowed. |
| `boolean`   | Boolean value. Values that are correctly parsed are `T` or `1` or `true` or `TRUE` for a true value, and `F` or `0` or `false` or `FALSE` for a false value.

Possible extensions for the types could be: `time`, `duration`, `color` and `coordinate`. These might be be implemented at a later stage.


### Dealing with wrong or missing data

| error      | behavior    | 
| ---------- | ----------- |
| `data` is not valid  | In other words, it is missing or not one of `mission_event`, `player_event`, `player_score`, `group_event` or `group_score`. In that case, data cannot be stored since we do not know where to store it. The data is written to disk into a log file instead. |
| `session_token` not found  | When the `session_token` does not exist in the database, we do not know the session where to store the data, nor the `game` and the `organization`. Therefore, the data cannot be stored since we do not know to what game (version) and organization to link it. The data is written to disk into a log file instead. |
| `organization_game_token` invalid | When the `organization_game_token` is forced, but not valid, the data will not be written to the database. The data is written to disk into a log file instead. |
| `game_token` is not valid | When the `game_token` is forced, but not valid, the data will not be written to the database. The data is written to disk into a log file instead. |
| `game_mission` does not exist | When the `game_mission` does not exist, it is impossible to link the `mission_data` to a mission. Therefore, the data will not be written to the database. The data is written to disk into a log file instead. |
| `group_attempt_nr` not a number | When `group_attempt_nr` is not a an integer value, the data is still written to the database. A value of `1` is used for the attempt number. A warning is logged to the log file. |
| `type` is invalid | When the `type` is not one of the permissable types (see above), the data is still written to the database. A warning is logged to the log file. |
| `value` not valid | When the `value` does not correspond to the `type` (e.g., text for an integer type), the data will still be written to the database. A warning is logged to the log file. |
| `timestamp` is invalid | If the `timestamp` is provided, but it does not contain a legible date plus time, data is still written to the database. The given timestamp is replaced by the timestamp of the server. A warning is logged to the log file. |
| `group_initiated` invalid | If the `group_initiated` does not contain a boolean (`T` or `1` or `true` or `TRUE` for a true value, and `F` or `0` or `false` or `FALSE` for a false value), the value is assumed to be `false`. The data is still written to the database. A warning is logged to the log file. |
| `game_code` not found | (for anonymous data). When the `game_code` does not exist in the database, we do not know for which game to store the data. Therefore, the data cannot be stored. The data is written to disk into a log file instead. |
| `game_version_code` not found | (for anonymous data). When the `game_version_code` does not exist in the database, we do not know for which game version to store the data. Therefore, the data cannot be stored. The data is written to disk into a log file instead. |
| `organization_code` not found | (for anonymous data). When the `organization_code` does not exist in the database, we do not know for which organization to store the data, nor whether the organization is allowed to store data for the game. Therefore, the data cannot be stored. The data is written to disk into a log file instead. |
| string too long | When one of the strings is longer than the allowed number of characters, it is truncated. The (truncated) data is stored, if valid. A warning is issued to the log file. |

