package com.lumina.hotel.model;

/**
 * Kết quả JOIN bookings + room_types + rooms + users.
 * Dùng cho: Đơn của tôi, Quản lý đơn đặt, Đơn mới nhất trên Dashboard.
 */
public class BookingWithDetail {
    private Booking booking;
    private String typeName;
    private String imageUrl;
    private String roomNumber;      // null nếu chưa gán phòng
    private String customerName;
    private String customerPhone;
    private boolean reviewed;       // đã đánh giá chưa (để ẩn nút Đánh giá)

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
}
