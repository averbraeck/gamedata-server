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

Characters that need to be escaped are:

| Character | Code | &nbsp; | Character | Code | &nbsp; | Character | Code |
| --------- | ---- | ------ | --------- | ---- | ------ | --------- | ---- |
| SPACE | %20 | | &vbar; | %7C | | ? | %3F |
| < | %3C | | \ | %5C | | : | %3A |
| > | %3E | | ^ | %5E | | @ | %40 |





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


## Sending a minimum player event
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


## Sending a minimum player score
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

