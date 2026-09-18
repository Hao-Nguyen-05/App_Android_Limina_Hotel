package com.lumina.hotel.model;

/** Bản ghi bảng promotions — mã giảm giá. */
public class Promotion {
    private int id;
    private String code;
    private String title;
    private int discountPercent;
    private long maxDiscount;
    private long startDate;
    private long endDate;
    private boolean active;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(long maxDiscount) { this.maxDiscount = maxDiscount; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
