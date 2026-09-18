package com.lumina.hotel.model;

/** Một điểm trên biểu đồ doanh thu của Dashboard. */
public class RevenuePoint {
    private String label;   // "20/09"
    private long revenue;

    public RevenuePoint(String label, long revenue) {
        this.label = label; this.revenue = revenue;
    }
    public String getLabel() { return label; }
    public long getRevenue() { return revenue; }
}
