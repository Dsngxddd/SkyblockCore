package net.cengiz1.skyblock.island;

import net.cengiz1.skyblock.config.SettingsManager;

import java.util.concurrent.atomic.AtomicInteger;

public class IslandGrid {

    private final SettingsManager settings;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    public IslandGrid(SettingsManager settings) {
        this.settings = settings;
    }

    public int reserveIndex() {
        return this.nextIndex.getAndIncrement();
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex.set(nextIndex);
    }

    public int getCenterX(int index) {
        return spiral(index)[0] * settings.getIslandDistance();
    }

    public int getCenterZ(int index) {
        return spiral(index)[1] * settings.getIslandDistance();
    }

    private int[] spiral(int index) {
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;

        for (int i = 0; i < index; i++) {
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx;
            z += dz;
        }

        return new int[]{x, z};
    }
}
