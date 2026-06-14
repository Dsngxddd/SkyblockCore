package net.cengiz1.skyblock.proxy;

/**
 * Sunucular arası Redis kanalında dolaşan mesaj türleri.
 */
public enum MessageType {

    /** Bir ada veritabanında güncellendi; diğer sunucular önbelleği DB'den tazelemeli. */
    ISLAND_UPDATE,

    /** Bir ada silindi; diğer sunucular önbellekten kaldırmalı. */
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
