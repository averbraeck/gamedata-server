package nl.gamedata.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * GameDataServer.java.
 * <p>
 * Copyright (c) 2024-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://github.com/averbraeck/gamedata-server/LICENSE">GameData project License</a>.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
@WebServlet("/store")
public class GameDataServer extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException
    {
        System.out.println("init()");
        super.init();
        System.getProperties().setProperty("org.jooq.no-logo", "true");

        // retrieve the username and password for the database
        String homeFolder = System.getProperty("user.home");
        if (homeFolder == null)
        {
            throw new ServletException("Home folder to retrieve database credentials not found");
        }
        String configDir = homeFolder + File.separator + "gamedata";
        File configFile = new File(configDir, "gamedata.properties");
        Properties gamedataProperties = new Properties();
        try
        {
            InputStream stream = new FileInputStream(configFile);
            gamedataProperties.load(stream);
        }
        catch (FileNotFoundException fnfe)
        {
            throw new ServletException(
                    "File with database credentials not found at " + configDir + "/" + "gamedata.properties");
        }
        catch (IOException ioe)
        {
            throw new ServletException("Error when reading database credentials at " + configDir + "/" + "gamedata.properties");
        }
        String dbUser = gamedataProperties.getProperty("dbUser");
        String dbPassword = gamedataProperties.getProperty("dbPassword");
        if (dbUser == null || dbPassword == null)
        {
            throw new ServletException(
                    "Properties dbUser or dbPassword not found in " + configDir + "/" + "gamedata.properties");
        }

        // determine the connection pool, and create one if it does not yet exist (first use after server restart)
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            throw new ServletException(e);
        }

        try
        {
            Context ctx = new InitialContext();
            try
            {
                ctx.lookup("/gamedata-server_datasource");
            }
            catch (NamingException ne)
            {
                final HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:mysql://localhost:3306/gamedata");
                config.setUsername(dbUser);
                config.setPassword(dbPassword);
                config.setMaximumPoolSize(2);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                DataSource dataSource = new HikariDataSource(config);
                ctx.bind("/gamedata-server_datasource", dataSource);
            }
        }
        catch (NamingException e)
        {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = request.getReader())
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                payload.append(line);
            }
        }

        String contentType = request.getContentType();
        // TODO: handle not-supported content types

        // Add task to queue
        StorageRequestTask task = new StorageRequestTask("POST", contentType.toLowerCase(), payload.toString());
        RequestQueueManager.addTask(task);

        // Respond to client
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.getWriter().write("Task submitted successfully");
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
    {
        String queryString = request.getQueryString();
        if (queryString == null)
        {
            // TODO: empty request -- ignore
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing query string in GET request");
            return;
        }

        // Add task to queue
        StorageRequestTask task = new StorageRequestTask("GET", "x-www-form-urlencoded", queryString);
        RequestQueueManager.addTask(task);

        // Respond to client
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.getWriter().write("Task submitted successfully");
    }

}
