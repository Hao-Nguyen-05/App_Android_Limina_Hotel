package com.lumina.hotel.model;

public class BookingBrief {
    private int id;
    private String guestName, roomTypeName, checkInLabel, status;
    private int numNights;
    private long totalAmount;
    // getters/setters
    public int getId() { return id; } public void setId(int v) { id = v; }
    public String getGuestName() { return guestName; } public void setGuestName(String v) { guestName = v; }
    public String getRoomTypeName() { return roomTypeName; } public void setRoomTypeName(String v) { roomTypeName = v; }
    public String getCheckInLabel() { return checkInLabel; } public void setCheckInLabel(String v) { checkInLabel = v; }
    /** booking_status: pending | confirmed | checked_in | completed | cancelled */
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public int getNumNights() { return numNights; } public void setNumNights(int v) { numNights = v; }
    public long getTotalAmount() { return totalAmount; } public void setTotalAmount(long v) { totalAmount = v; }
}