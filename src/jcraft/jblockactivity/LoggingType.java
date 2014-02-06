package jcraft.jblockactivity;

public enum LoggingType {
    bothblocks(-2),
    all(-1),
    blockbreak(1),
    blockplace(2),
    inventoryaccess(3),
    blockinteract(4),
    hangingbreak(5),
    hangingplace(6),
    hanginginteract(7),
    creaturekill(8);

    private final int id;

    private LoggingType(int id) {
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
