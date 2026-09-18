package com.lumina.hotel.model;

/** Bản ghi bảng bookings. Mọi số tiền đều kiểu long, đơn vị VNĐ. */
public class Booking {
    private int id;
    private String bookingCode;
    private int userId;
    private int roomTypeId;
    private int roomId;            // 0 = chưa gán phòng vật lý
    private long checkInDate;
    private long checkOutDate;
    private int numNights;
    private int numGuests;
    private long roomSubtotal;
    private long serviceFee;
    private long vatAmount;
    private long discountAmount;
    private long totalAmount;
    private String bookingStatus;
    private String note;
    private long createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(int roomTypeId) { this.roomTypeId = roomTypeId; }
    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public long getCheckInDate() { return checkInDate; }
    public void setCheckInDate(long checkInDate) { this.checkInDate = checkInDate; }
    public long getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(long checkOutDate) { this.checkOutDate = checkOutDate; }
    public int getNumNights() { return numNights; }
    public void setNumNights(int numNights) { this.numNights = numNights; }
    public int getNumGuests() { return numGuests; }
    public void setNumGuests(int numGuests) { this.numGuests = numGuests; }
    public long getRoomSubtotal() { return roomSubtotal; }
    public void setRoomSubtotal(long roomSubtotal) { this.roomSubtotal = roomSubtotal; }
    public long getServiceFee() { return serviceFee; }
    public void setServiceFee(long serviceFee) { this.serviceFee = serviceFee; }
    public long getVatAmount() { return vatAmount; }
    public void setVatAmount(long vatAmount) { this.vatAmount = vatAmount; }
    public long getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(long discountAmount) { this.discountAmount = discountAmount; }
    public long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
