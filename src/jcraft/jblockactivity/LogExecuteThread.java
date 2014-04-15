package jcraft.jblockactivity;

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlog.ActionLog;

public class LogExecuteThread implements Runnable {

    public final LinkedBlockingQueue<ActionLog> queue = new LinkedBlockingQueue<ActionLog>();
    public final int maxTimePerRun, timeBetweenRuns, minLogsToProcess, queueWarningSize;
    public final Lock lock = new ReentrantLock();

    public LogExecuteThread(int maxTimePerRun, int timeBetweenRuns, int minLogsToProcess, int queueWarningSize) {
        this.maxTimePerRun = maxTimePerRun;
        this.timeBetweenRuns = timeBetweenRuns;
        this.minLogsToProcess = minLogsToProcess;
        this.queueWarningSize = queueWarningSize;
    }

    private boolean running = true;

    public void terminate() {
        running = false;
    }

    public void notifyLock() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void run() {
        synchronized (lock) {
            while (running) {
                if (queue.isEmpty() || !lock.tryLock()) {
                    continue;
                }

                if (queueWarningSize > 0 && queue.size() >= queueWarningSize) {
                    getLogger().info("[Queue] Queue is overloaded! Size: " + getQueueSize());
                }

                Connection connection = BlockActivity.getBlockActivity().getConnection();
                Statement state = null;
                try {
                    if (connection == null) {
                        return;
                    }
                    connection.setAutoCommit(false);
                    state = connection.createStatement();
                    final long start = System.currentTimeMillis();
                    int count = 0;
                    while (!queue.isEmpty() && (System.currentTimeMillis() - start < maxTimePerRun || count < minLogsToProcess)) {
                        final ActionLog log = queue.poll();
                        if (log == null) {
                            continue;
                        }
                        if (!BlockActivity.playerIds.containsKey(log.getIdentifier())) {
                            if (!addPlayer(state, log.getPlayerName(), log.getUUID())) {
                                getLogger().warning("[Executor] Failed to add new player: " + log.getPlayerName() + " (" + log.getUUID() + ")");
                                continue;
                            }
                        }
                        try {
                            log.executeStatements(connection);
                        } catch (final SQLException ex) {
                            getLogger().log(Level.SEVERE, "[Executor] SQL Exception on executing: ", ex);
                            break;
                        }
                        count++;
                    }
                    connection.commit();
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "[Executor] SQL Exception on connection:", e);
                } finally {
                    try {
                        state.close();
                        connection.close();
                    } catch (final SQLException ex) {
                        getLogger().log(Level.SEVERE, "[Executor] SQL Exception on close:", ex);
                    }
                    lock.unlock();
                }

                if (!running) {
                    break;
                }

                try {
                    lock.wait(timeBetweenRuns * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private boolean addPlayer(Statement state, String playerName, UUID uuid) throws SQLException {
        final String stripUUID;
        if (uuid != null) {
            stripUUID = uuid.toString().replace("-", "");
        } else {
            stripUUID = playerName;
        }

        state.execute("INSERT INTO `ba-players` (playername, uuid) SELECT '" + playerName + "', '" + stripUUID
                + "' FROM DUAL WHERE NOT EXISTS (SELECT * FROM `ba-players` WHERE uuid = '" + stripUUID + "') LIMIT 1;");
        final ResultSet rs = state.executeQuery("SELECT playerid FROM `ba-players` WHERE uuid = '" + stripUUID + "'");
        if (rs.next()) {
            BlockActivity.playerIds.put(stripUUID, rs.getInt(1));
        }
        rs.close();
        return BlockActivity.playerIds.containsKey(stripUUID);
    }

}
