# API for sending game data

## 1. Technical requirements

The data is sent using an http(s) request to the gamedata server. The data can be sent as a GET request or as a POST request. The content is encoded using simple key-value pairs without any nesting. For now, one message can be sent per request. The server reads the payload of the request, checks superficially (and quickly) for its correctness. When the message is correct, it is passed to a message queue for processing, and the response will be `200 OK`. In case the request is incorrect and cannot be processed as a result, the response sent will be `400 Bad Request`. 

The following message types are accepted:

### GET request
When sending a GET request, all data is encoded in the http(s) address, using the well-known `?` and `&` construct. This is a very low-effort implementation that can be used in many applications without extensive programming. Note that many types of characters need to be escaped (see below). An example is:

```
https://gamedata.nl/gamedata-server/store?data=mission_event&session_token=tk_5t4YP
  &game_mission=M1&type=string&key=task1&value=started
```

Characters that need to be escaped are (from https://en.wikipedia.org/wiki/Percent-encoding):

| Character | Code | &nbsp; | Character | Code | &nbsp; | Character | Code | &nbsp; | Character | Code |
| --------- | ---- | ------ | --------- | ---- | ------ | --------- | ---- | ------ | --------- | ---- |
| SPACE | %20 | | ( | %28 | | < | %3C | | ^ | %5E |
| ! | %21 | | ) | %29 | | = | %3D | | ` | %60 |
| " | %22 | | * | %2A | | > | %3E | | { | %7B |
| # | %23 | | + | %2B | | ? | %3F | | &vert; | %7C |
| $ | %24 | | , | %2C | | @ | %40 | | } | %7D |
| % | %25 | | / | %2F | | [ | %5B | | ~ | %7E |
| & | %26 | | : | %3A | | \ | %5C | | &pound;  | %C2%A3 |
| ' | %27 | | ; | %3B | | ] | %5E | | &euro;  | %E2%82%AC |


### POST request, form-encoded
The same data format that is sent with a GET request can also be sent with a POST request. It needs the following `Content-Type`:

```
Content-Type: x-www-form-urlencoded
```

The `Content-Type` may be followed by a character set:

```
Content-Type: x-www-form-urlencoded; charset=UTF-8
```

Note that neither the content-type, nor the character set is case sensitive. They will be transformed to lower case before processing. The payload in the message is sent as a single-line string:

```
data=mission_event&session_token=tk_5t4YP&game_mission=M1&type=string&key=task1&value=started
```

It uses the same escape characters as shown in the GET information above.


### POST request, json encoded
The gamedata server application accepts JSON formatted data, which is convenient for Javascript and web-based applications. It needs the following `Content-Type`:

```
Content-Type: application/json
```

The `Content-Type` may be followed by a character set:

```
Content-Type: application/json; charset=UTF-8
```

Note that neither the content-type, nor the character set is case sensitive. They will be transformed to lower case before processing. The payload in the message is either sent as a single-line string (line break inserted for display purposes):

```json
{"data":"mission_event","session_token":"tk_5t4YP","game_mission":"M1",
   "type":"string","key":"task1","value":"started"}
```

or as a multi-line string:

```json
{
  "data":"mission_event",
  "session_token":"tk_5t4YP",
  "game_mission":"M1",
  "type":"string",
  "key":"task1",
  "value":"started"
}
```

Escaping characters in the JSON string, such as a double quote, follows the JSON standards. Most used are:

| Character | Code |
| --------- | ---- |
| newline   | \n   |
| tab       | \t   |
| "         | \\"  |
| \         | \\\\ |

It is best to use an up-to-date library to 'stringify' the JSON strings.


### POST request, XML encoded
The gamedata server application accepts XML formatted data, which can be convenient in certain programming environments. It needs the following `Content-Type`:

```
Content-Type: application/xml
```

The `Content-Type` may be followed by a character set:

```
Content-Type: application/xml; charset=UTF-8
```

Note that neither the content-type, nor the character set is case sensitive. They will be transformed to lower case before processing. The payload in the message is typically sent as a multi-line string:

```xml
  <data>mission_event</data>
  <session_token>tk_5t4YP</session_token>
  <game_mission>M1</game_mission>
  <type>string</type>
  <key>task1</key>
  <value>started</value>
```

It is allowed, but not compulsory, to package the data into a `<gamedata>` tag:

```xml
<gamedata>
  <data>mission_event</data>
  <session_token>tk_5t4YP</session_token>
  <game_mission>M1</game_mission>
  <type>string</type>
  <key>task1</key>
  <value>started</value>
</gamedata>
```

Escaping characters in the XML string, such as a less-than or bigger-than sign, follows the XML standards. Most used are:

| Character | Code       |
| --------- | ---------- |
| '         | &amp;apos; |
| "         | &amp;quot; |
| &         | &amp;amp;  |
| <         | &amp;lt;   |
| >         | &amp;gt;   |

It is best to use an up-to-date library to encode the XML strings.



## 2. Examples

### Setup 
Suppose we have a setup with the following game, version, missions and session:

- Game called AbcGame
- Game version v1
- Game missions M1 and M2
- Game session with code Test and session token tk_5t4YP
- No Game token, no organization-game token


### Sending a mission event
Suppose the mission just opened task2 for players in mission 1. The data is sent as a json record to the game data server, either as a POST or as a GET request. 

The POST request could send, e.g., the following data to `https://gamedata.nl/gamedata-server/store`:

```json
{
  "data": "mission_event"
  "session_token": "tk_5t4YP",
  "game_mission": "M1",
  "type": "string",
  "key": "task2",
  "value": "open"
}
```

The GET request could be, e.g.:

```
https://gamedata.nl/gamedata-server/store?data="mission_event"&session_token="tk_5t4YP"
  &game_mission="M1"&type="string"&key="task2"&value="open"
```

The server-side code will add the `timestamp`, and fill the compulsory field `player_initiated` with `true`.


### Sending a player event
Suppose a player with code "p24" and nickname "John" just finished task1 in mission 1. The data is sent as a json record to the game data server, either as a POST or as a GET request. 

The POST request could send, e.g., the following data to `https://gamedata.nl/gamedata-server/store`:

```json
{
  "data": "player_event"
  "session_token": "tk_5t4YP",
  "game_mission": "M1",
  "player_name": "p24",
  "player_display_name": "John",
  "type": "string",
  "key": "task1",
  "value": "finished"
}
```

The GET request could be, e.g.:

```
https://gamedata.nl/gamedata-server/store?data="player_event"&session_token="tk_5t4YP"
  &game_mission="M1"&player_name="p24"&player_display_name="John"
  &type="string"&key="task1"&value="finished"
```

The server-side code will add the `timestamp`, and fill the compulsory field `player_initiated` with `true`. Furthermore, if no attempt is made yet, the data will be stored under "attempt #1". It is sufficient to fill either the name, or the display name. Many games will fill both, though.


### Sending a player score
Suppose the player with code "p24" and nickname "John" just scored 5 extra points for 'health' in mission M1, bringing the total 'health' score to 59. The data is sent as a json record to the game data server, either as a POST or as a GET request. 

The POST request could send, e.g., the following data to `https://gamedata.nl/gamedata-server/store`:

```json
{
  "data": "player_score"
  "session_token": "tk_5t4YP",
  "game_mission": "M1",
  "player_name": "p24",
  "player_display_name": "John",
  "score_type": "health",
  "delta": 5,
  "new_score_number": 59
}
```

The GET request could be, e.g.:

```
https://gamedata.nl/gamedata-server/store?data="player_score"&session_token="tk_5t4YP"
  &game_mission="M1"&player_name="p24"&player_display_name="John"
  &score_type="health"&delta=5&new_score_number=59
```

Note that it is convenient, but not necessary, to send both the `delta` and the `new_score_number`. It is also possible to send either of them, or neither. It is sufficient to fill either the name, or the display name. Many games will fill both, though. The server-side code will add the `timestamp`, and fill the compulsory field `player_initiated` with `true`.

