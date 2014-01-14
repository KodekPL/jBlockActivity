package jcraft.jblockactivity;

public enum LoggingType {
    all(-1), blockbreak(1), blockplace(2);

    private final int id;

    LoggingType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static LoggingType getTypeById(int id) {
        for (LoggingType type : LoggingType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }
}
