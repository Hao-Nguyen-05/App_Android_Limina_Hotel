// model/DashboardStats.java
package com.lumina.hotel.model;

public class DashboardStats {
    public long revenue, revenueChangePercent;
    public int newBookings, newBookingsChangePercent;
    public float occupancyRate;
    public int vacantRooms;
    public long prevPeriodRevenue; // dùng để tính banner "tăng 18%"
    // getters/setters chuẩn hoặc để public field nếu project đang dùng kiểu này ở POJO khác — báo mình quy ước đang dùng nếu khác
}