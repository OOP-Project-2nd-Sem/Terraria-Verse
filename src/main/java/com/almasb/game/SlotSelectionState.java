package com.almasb.game;

/**
 * Tiny shared object that tracks which slot the player has clicked
 * (to begin a drag/swap operation).
 *
 * One instance is created in GameApp and passed to InventoryUI, HotbarUI,
 * and ArmorUI so they all share the same selection cursor.
 */
public class SlotSelectionState {

    private int    index    = -1;
    private String slotType = "";

    public void select(int index, String slotType) {
        this.index    = index;
        this.slotType = slotType;
    }

    public void clear() {
        this.index    = -1;
        this.slotType = "";
    }

    public boolean isEmpty()        { return index == -1; }
    public int     getIndex()       { return index; }
    public String  getSlotType()    { return slotType; }
}
