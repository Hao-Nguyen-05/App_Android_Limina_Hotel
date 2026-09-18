package com.lumina.hotel.model;

/** Kết quả JOIN rooms + room_types — dùng cho màn Quản lý phòng của admin. */
public class RoomWithType {
    private Room room;
    private String typeName;
    private long basePrice;

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public long getBasePrice() { return basePrice; }
    public void setBasePrice(long basePrice) { this.basePrice = basePrice; }
}
