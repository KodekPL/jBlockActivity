package jcraft.jblockactivity;

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

import org.bukkit.Bukkit;

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
                    Bukkit.getLogger().info("[jBA-Queue] Queue is overloaded! Size: " + getQueueSize());
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
                                Bukkit.getLogger().warning(
                                        "[jBA-Executor] Failed to add new player: " + log.getPlayerName() + " (" + log.getUUID() + ")");
                                continue;
                            }
                        }
                        try {
                            log.executeStatements(connection);
                        } catch (final SQLException ex) {
                            Bukkit.getLogger().log(Level.SEVERE, "[jBA-Executor] SQL Exception on executing: ", ex);
                            break;
                        }
                        count++;
                    }
                    connection.commit();
                } catch (SQLException e) {
                    Bukkit.getLogger().log(Level.SEVERE, "[jBA-Executor] SQL Exception on connection:", e);
                } finally {
                    try {
                        state.close();
                        connection.close();
                    } catch (final SQLException ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "[jBA-Executor] SQL Exception on close:", ex);
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

        final ResultSet result1 = state.executeQuery("SELECT playerid FROM `ba-players` WHERE uuid = '" + stripUUID + "'");
        if (result1.next()) {
            BlockActivity.playerIds.put(stripUUID, result1.getInt(1));
        } else {
            final ResultSet result2 = state.executeQuery("SELECT `playername`, `uuid` FROM `ba-players` WHERE `playername` LIKE CONCAT('"
                    + playerName + "%', '%') ORDER BY `playername` ASC");
            String takenName = playerName;
            int nameCount = 0;
            while (result2.next()) {
                if (result2.getString(1).equalsIgnoreCase(takenName)) {
                    nameCount++;
                    takenName = playerName + nameCount;
                }
            }
            state.execute("INSERT INTO `ba-players` (playername, uuid) SELECT '" + takenName + "', '" + stripUUID
                    + "' FROM DUAL WHERE NOT EXISTS (SELECT * FROM `ba-players` WHERE uuid = '" + stripUUID + "') LIMIT 1;");

            final ResultSet result3 = state.executeQuery("SELECT playerid FROM `ba-players` WHERE uuid = '" + stripUUID + "'");
            if (result3.next()) {
                BlockActivity.playerIds.put(stripUUID, result3.getInt(1));
            }
            result2.close();
            result3.close();
        }
        result1.close();
        return BlockActivity.playerIds.containsKey(stripUUID);
    }

}
