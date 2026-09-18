package com.lumina.hotel.model;

/** Bản ghi bảng reviews. reviewerName lấy từ JOIN users, không phải cột trong bảng. */
public class Review {
    private int id;
    private int bookingId;
    private int userId;
    private int roomTypeId;
    private int rating;
    private String comment;
    private long createdAt;
    private String reviewerName;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(int roomTypeId) { this.roomTypeId = roomTypeId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getReviewerName() { return reviewerName; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
}
