package jcraft.jblockactivity;

public enum LoggingType {

    bothblocks(-2, false),
    all(-1, false),
    blockbreak(1, true),
    blockplace(2, true),
    inventoryaccess(3, true),
    blockinteract(4, true),
    hangingbreak(5, true),
    hangingplace(6, true),
    hanginginteract(7, true),
    creaturekill(8, true),
    explosions(9, true),
    blockburn(10, true),
    tramplefarmland(11, true),
    worldedit(12, false);

    private final int id;
    private final boolean defaultState;

    private LoggingType(int id, boolean defaultState) {
        this.id = id;
        this.defaultState = defaultState;
    }

    public int getId() {
        return id;
    }

    public boolean getDefaultState() {
        return defaultState;
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
