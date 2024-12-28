# API for sending game data

## Example setup
Suppose we have a setup with the following game, version, missions and session:

- Game called AbcGame
- Game version v1
- Game missions M1 and M2
- Game session with code Test and session token tk_5t4YP
- No Game token, no organization-game token


## Sending a minimum mission event
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

