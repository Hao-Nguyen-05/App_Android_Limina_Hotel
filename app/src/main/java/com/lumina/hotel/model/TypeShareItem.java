package com.lumina.hotel.model;

public class TypeShareItem {
    private final String typeName;
    private final int percent;
    private final int count;
    public TypeShareItem(String typeName, int percent, int count) {
        this.typeName = typeName; this.percent = percent; this.count = count;
    }
    public String getTypeName() { return typeName; }
    public int getPercent() { return percent; }
    /** Số đơn đặt của loại phòng này trong khoảng ngày đang lọc. */
    public int getCount() { return count; }
}