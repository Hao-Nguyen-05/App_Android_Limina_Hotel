package com.lumina.hotel.model;

/** Bản ghi bảng rooms — PHÒNG VẬT LÝ, chỉ admin thao tác. */
public class Room {
    private int id;
    private String roomNumber;
    private int floor;
    private int roomTypeId;
    private String roomStatus;
    private String note;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public int getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(int roomTypeId) { this.roomTypeId = roomTypeId; }
    public String getRoomStatus() { return roomStatus; }
    public void setRoomStatus(String roomStatus) { this.roomStatus = roomStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
