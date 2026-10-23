package com.afk.client.util;

/** Result of looking for an item in the hotbar. */
public record FindItemResult(int slot, int count) {
    public boolean found() {
        return slot >= 0;
    }
}
