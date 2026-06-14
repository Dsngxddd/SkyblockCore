package net.cengiz1.skyblock.proxy;

public enum MessageType {

    ISLAND_UPDATE,

    ISLAND_DELETE;

    public static MessageType fromString(String value) {
        if (value == null)
            return null;
        for (MessageType type : values())
            if (type.name().equalsIgnoreCase(value))
                return type;
        return null;
    }
}
