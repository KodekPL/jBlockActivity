package jcraft.jblockactivity.session;

import java.util.HashMap;
import java.util.Map;

import jcraft.jblockactivity.utils.QueryParams;
import jcraft.jblockactivity.utils.question.QuestionData;

import org.bukkit.command.CommandSender;

public class ActiveSession {

    private static final Map<String, ActiveSession> sessions = new HashMap<String, ActiveSession>();

    private QueryParams lastParams = null;
    private LookupCache[] lookupCache = null;
    private QuestionData question = null;

    public QueryParams getLastQueryParams() {
        return lastParams;
    }

    public void setLastQueryParams(QueryParams params) {
        this.lastParams = params;
    }

    public LookupCache[] getLastLookupCache() {
        return lookupCache;
    }

    public void setLastLookupCache(LookupCache[] lookupCache) {
        this.lookupCache = lookupCache;
    }

    public QuestionData getQuestion() {
        return question;
    }

    public void setQuestion(QuestionData question) {
        this.question = question;
    }

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
