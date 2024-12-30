# API for mission event data

The fields for the mission event are as follows. Note that the field name is used in lower case for the key in the form-data GET or POST request, as the key in the JSON payload, and as the tag name in the XML payload. All keys are in lower case and use 'snaking' with underscores as indicated in the table.

| key           | required? | type | explanation         |
| ------------- | --------- | ---- | ------------------- |
| `data`          | required | string | Value should be `mission_event`, lower case with underscore. |
| `session_token` | required | string(45) | Token to identify the game session, game version, game, and organization responsible for playing the game. |
| `game_mission`  | required | string(16) | Code of the game mission for which this is an event. Every game has at least one mission. The mission code will be looked up for the game that is implicitly encoded in the `session_token`. |
| `type`          | optional | string(45) | The type of data being sent. When not included, `string` is assumed. See below for a table with types. |
| `key`           | required | string(45) | The key of maximum 45 characters. Although the key can contain any characters, keys typically do not have spaces and consist of ASCII characters. |
| `value`        | required | text(65535) | The value belonging to the key, of the appropriate type. The value can be a multi-line string. Max 65,535 characters. |
| `timestamp`   | don't use | timestamp | The timestamp is normally inserted by the server at the moment of receiving the data, and stored in UTC time. In case you would want to override the timestamp, it can be provided in the data. In that case, the server will not allocate it's own timestamp. |
| `status`      | optional | string(45) | If the data sent is linked to some status, it can be provided in this field. Max 45 characters. |
| `round`       | optional | string(16) | If the data sent is linked to a round, it can be provided in this field. Rounds can be a number, but also text, e.g., `Practice-1`. It is always stored as a text field in the database. The maximum length is 16 characters. |
| `game_time`   | optional | string(45) | Many games keep their own clock that is different from the wall clock. This can be measured in many different ways, e.g., seconds since the start, or years in an environmental game. The `game_time` can be 45 characters, and it is always stored as a text field in the database. |
| `grouping_code` | optional | string(45) | The optional grouping code of max 45 characters can be used to filter data using different headings. This can help in the data analysis. A `grouping_code` could, for instance, be `error` where session events store exceptions in the execution of the game. |
| `facilitator_initiated` | optional | boolean | Indicates whether the event was initiated by manual intervention of the facilitator (`true`) or an autonomous event by the game (`false`). The default value is `false`. | 



### Values for `type`

| type       | explanation | 
| ---------- | ----------- |
| `string`     | Characters. Single-line. utf-8 encoding is used. |
| `text`       | Characters. Can be multi-line. utf-8 encoding is used. |
| `integer`    | Integer value. Can be positive or negative. A leading `+` sign is not allowed. |
| `float`      | Floating-point value. Use a decimal point (no decimal comma, no thousands separators). Scientific notation is allowed, with an `E` or `e` for the exponent. An example is `6.24E+7` or `6.24E7`. The use of values `NaN`, `Inf` and `-Inf` are allowed. A leading `+` sign is not allowed. |
| `boolean`   | Boolean value. Values that are correctly parsed are `T` or `1` or `true` or `TRUE` for a true value, and `F` or `0` or `false` or `FALSE` for a false value.

Possible extensions for the types could be: `time`, `duration`, `color` and `coordinate`. These might be be implemented at a later stage.

