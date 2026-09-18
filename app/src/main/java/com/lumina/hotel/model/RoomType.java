package com.lumina.hotel.model;

/** Bản ghi bảng room_types — LOẠI phòng mà khách xem và đặt. */
public class RoomType {
    private int id;
    private String typeName;
    private String description;
    private int areaM2;
    private int maxGuests;
    private long basePrice;
    private String imageUrl;
    private int totalRooms;
    private boolean active;
    private float avgRating;
    private int reviewCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getAreaM2() { return areaM2; }
    public void setAreaM2(int areaM2) { this.areaM2 = areaM2; }
    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }
    public long getBasePrice() { return basePrice; }
    public void setBasePrice(long basePrice) { this.basePrice = basePrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public float getAvgRating() { return avgRating; }
    public void setAvgRating(float avgRating) { this.avgRating = avgRating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
}
