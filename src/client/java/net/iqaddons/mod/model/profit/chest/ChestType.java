package net.iqaddons.mod.model.profit.chest;

public enum ChestType {

    FREE, PAID, UNKNOWN;

    public static ChestType fromString(String str) {
        if (str == null) return UNKNOWN;
        if (str.contains("Free Chest")) return FREE;
        if (str.contains("Paid Chest")) return PAID;

        return UNKNOWN;
    }
}
