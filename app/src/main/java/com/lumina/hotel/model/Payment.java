package com.lumina.hotel.model;

/** Bản ghi bảng payments. */
public class Payment {
    private int id;
    private int bookingId;
    private String method;
    private long amount;
    private String paymentStatus;
    private long paidAt;
    private String txnRef;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public long getPaidAt() { return paidAt; }
    public void setPaidAt(long paidAt) { this.paidAt = paidAt; }
    public String getTxnRef() { return txnRef; }
    public void setTxnRef(String txnRef) { this.txnRef = txnRef; }
}
