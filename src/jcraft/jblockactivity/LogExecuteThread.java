package jcraft.jblockactivity;

import static org.bukkit.Bukkit.getLogger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import jcraft.jblockactivity.actionlogs.ActionLog;
import jcraft.jblockactivity.sql.SQLConnection;

public class LogExecuteThread implements Runnable {

    public final LinkedBlockingQueue<ActionLog> queue = new LinkedBlockingQueue<ActionLog>();
    public final int maxTimePerRun, timeBetweenRuns, minLogsToProcess;
    public final Lock lock = new ReentrantLock();

    public LogExecuteThread(int maxTimePerRun, int timeBetweenRuns, int minLogsToProcess) {
        this.maxTimePerRun = maxTimePerRun;
        this.timeBetweenRuns = timeBetweenRuns;
        this.minLogsToProcess = minLogsToProcess;
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
                final SQLConnection sqlConnection = new SQLConnection(BlockActivity.sqlProfile);
                Statement state = null;
                try {
                    sqlConnection.open();
                    sqlConnection.getConnection().setAutoCommit(false);
                    state = sqlConnection.getConnection().createStatement();
                    final long start = System.currentTimeMillis();
                    int count = 0;
                    while (!queue.isEmpty() && (System.currentTimeMillis() - start < maxTimePerRun || count < minLogsToProcess)) {
                        final ActionLog log = queue.poll();
                        if (log == null) {
                            continue;
                        }
                        if (!BlockActivity.playerIds.containsKey(log.getPlayerName())) {
                            if (!addPlayer(state, log.getPlayerName())) {
                                getLogger().warning("[Executor] Failed to add new player: " + log.getPlayerName());
                                continue;
                            }
                        }
                        try {
                            log.executeStatements(sqlConnection.getConnection());
                        } catch (final SQLException ex) {
                            getLogger().log(Level.SEVERE, "[Executor] SQL Exception on executing: ", ex);
                            break;
                        }
                        count++;
                    }
                    sqlConnection.getConnection().commit();
                } catch (SQLException e) {
                    getLogger().log(Level.SEVERE, "[Executor] SQL Exception on connection:", e);
                } finally {
                    try {
                        state.close();
                        sqlConnection.closeConnection();
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

    private boolean addPlayer(Statement state, String playerName) throws SQLException {
        state.execute("INSERT INTO `ba-players` (playername) SELECT '" + playerName
                + "' FROM DUAL WHERE NOT EXISTS (SELECT * FROM `ba-players` WHERE `playername` = '" + playerName + "') LIMIT 1;");
        final ResultSet rs = state.executeQuery("SELECT playerid FROM `ba-players` WHERE playername = '" + playerName + "'");
        if (rs.next()) {
            BlockActivity.playerIds.put(playerName, rs.getInt(1));
        }
        rs.close();
        return BlockActivity.playerIds.containsKey(playerName);
    }
}
