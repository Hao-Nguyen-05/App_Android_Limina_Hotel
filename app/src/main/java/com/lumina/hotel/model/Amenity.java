package com.lumina.hotel.model;

/** Bản ghi bảng amenities — tiện nghi phòng. */
public class Amenity {
    private int id;
    private String amenityName;
    private String iconKey;

    public Amenity() { }
    public Amenity(int id, String amenityName, String iconKey) {
        this.id = id; this.amenityName = amenityName; this.iconKey = iconKey;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAmenityName() { return amenityName; }
    public void setAmenityName(String amenityName) { this.amenityName = amenityName; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
}
