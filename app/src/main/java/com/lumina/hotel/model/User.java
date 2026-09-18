package com.lumina.hotel.model;

/** Bản ghi bảng users. POJO thuần, không annotation. */
public class User {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private long dob;
    private String avatarUrl;
    private String role;
    private boolean vip;
    private long createdAt;
    private String authProvider;
    private String googleId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public long getDob() { return dob; }
    public void setDob(long dob) { this.dob = dob; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isVip() { return vip; }
    public void setVip(boolean vip) { this.vip = vip; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    /** Chữ cái đầu dùng làm avatar khi chưa có ảnh. */
    public String getInitial() {
        if (fullName == null || fullName.trim().isEmpty()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1].substring(0, 1).toUpperCase();
    }
}
