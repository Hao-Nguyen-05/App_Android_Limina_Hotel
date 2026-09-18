package com.lumina.hotel.model;

/** Kết quả GROUP BY users + bookings — dùng cho màn Quản lý khách hàng. */
public class CustomerStat {
    private User user;
    private int totalBookings;
    private long totalSpent;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }
    public long getTotalSpent() { return totalSpent; }
    public void setTotalSpent(long totalSpent) { this.totalSpent = totalSpent; }
}
