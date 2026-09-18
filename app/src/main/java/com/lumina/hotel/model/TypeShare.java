package com.lumina.hotel.model;

/** Một dòng trong biểu đồ "Tỷ trọng loại phòng đặt" của Dashboard. */
public class TypeShare {
    private String typeName;
    private int count;
    private int percent;

    public TypeShare(String typeName, int count, int percent) {
        this.typeName = typeName; this.count = count; this.percent = percent;
    }
    public String getTypeName() { return typeName; }
    public int getCount() { return count; }
    public int getPercent() { return percent; }
}
