package jcraft.jblockactivity;

import java.util.concurrent.LinkedBlockingQueue;

import jcraft.jblockactivity.utils.QueryParams;

import org.bukkit.command.CommandSender;

public class ActionExecuteThread implements Runnable {

    private boolean running = true;

    private final CommandHandler cmdHandler;
    private final static LinkedBlockingQueue<ActionRequest> queue = new LinkedBlockingQueue<ActionRequest>();

    public ActionExecuteThread(CommandHandler cmdHandler) {
        this.cmdHandler = cmdHandler;
    }

    public void run() {
        synchronized (queue) {
            while (running) {
                ActionRequest request;

                try {
                    request = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                try {
                    cmdHandler.executeCommand(request);
                } catch (IllegalArgumentException e) {
                    request.getSender().sendMessage(e.getMessage());
                    continue;
                }
            }
        }
    }

    public static void addRequest(ActionRequest request) {
        queue.add(request);
    }

    public void terminate() {
        running = false;
    }

    public static class ActionRequest {

        public enum ActionType {
            CMD_LOOKUP, CMD_CLEARLOG, CMD_ROLLBACK, CMD_REDO, CMD_CONFIRM;
        }

        private final ActionType type;
        private final CommandSender sender;
        private String[] args;
        private QueryParams params;
        private final boolean askQuestion;

        public ActionRequest(ActionType type, CommandSender sender, QueryParams params) {
            this.type = type;
            this.sender = sender;
            this.params = params;
            this.askQuestion = false;
        }

        public ActionRequest(ActionType type, CommandSender sender, QueryParams params, boolean askQuestion) {
            this.type = type;
            this.sender = sender;
            this.params = params;
            this.askQuestion = askQuestion;
        }

        public ActionRequest(ActionType type, CommandSender sender, String[] args, boolean askQuestion) {
            this.type = type;
            this.sender = sender;
            this.args = args;
            this.askQuestion = askQuestion;
        }

        public ActionType getType() {
            return type;
        }

        public CommandSender getSender() {
            return sender;
        }

        public String[] getArgs() {
            return args;
        }

        public boolean askQuestion() {
            return askQuestion;
        }

        private void parseParams() throws IllegalArgumentException {
            params = new QueryParams(sender, args, false);
        }

        public QueryParams getParams() throws IllegalArgumentException {
            if (params == null) {
                parseParams();
            }

            return params;
        }

    }

}
