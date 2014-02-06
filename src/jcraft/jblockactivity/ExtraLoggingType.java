package jcraft.jblockactivity;

public enum ExtraLoggingType {
    commandblock(1), flowerpot(2), mobspawner(3), signtext(4), skull(5), inventory(6);

    private final int id;

    private ExtraLoggingType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ExtraLoggingType getTypeById(int id) {
        for (ExtraLoggingType type : ExtraLoggingType.values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        return null;
    }
}