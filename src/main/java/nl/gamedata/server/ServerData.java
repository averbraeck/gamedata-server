package nl.gamedata.server;

import nl.gamedata.common.CommonData;
import nl.gamedata.data.tables.records.UserRecord;

public class ServerData extends CommonData
{
    /** The name of the user logged in to this session. If null, no user is logged in. */
    private String username;

    /** the User record (static during session). */
    private UserRecord user;

    /* ================================================ */
    /* PERSISTENT DATA ABOUT CHOICES MADE ON THE SCREEN */
    /* ================================================ */

    public String getUsername()
    {
        return this.username;
    }

    public void setUsername(final String username)
    {
        this.username = username;
    }

    public UserRecord getUser()
    {
        return this.user;
    }

    public void setUser(final UserRecord user)
    {
        this.user = user;
    }

}
