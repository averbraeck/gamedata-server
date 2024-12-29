package nl.gamedata.server;

import javax.servlet.http.HttpSession;

import nl.gamedata.common.SqlUtils;

public final class ServerUtils extends SqlUtils {

    public static void loadAttributes(final HttpSession session) {
        ServerData data = SessionUtils.getData(session);
        data.setMenuChoice("");
    }

}
