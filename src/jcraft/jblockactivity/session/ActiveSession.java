package jcraft.jblockactivity.session;

import java.util.HashMap;
import java.util.Map;

import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.question.QuestionData;

import org.bukkit.command.CommandSender;

public class ActiveSession {
    private static final Map<String, ActiveSession> sessions = new HashMap<String, ActiveSession>();

    public QueryParams lastParams = null;
    public LookupCache[] lookupCache = null;

    public QuestionData question = null;

    public static boolean hasSession(CommandSender sender) {
        return sessions.containsKey(sender.getName());
    }

    public static boolean hasSession(String playerName) {
        return sessions.containsKey(playerName);
    }

    public static ActiveSession getSession(CommandSender sender) {
        return getSession(sender.getName());
    }

    public static ActiveSession getSession(String playerName) {
        ActiveSession session = sessions.get(playerName);
        if (session == null) {
            session = new ActiveSession();
            sessions.put(playerName, session);
        }
        return session;
    }
}
