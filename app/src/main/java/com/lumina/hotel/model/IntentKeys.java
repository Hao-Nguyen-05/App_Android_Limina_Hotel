package com.lumina.hotel.model;

/**
 * Hằng số key Intent dùng chung toàn app (theo BLOCK NAV).
 * Luôn dùng hằng số này khi putExtra/getExtra, không gõ tay chuỗi.
 */
public final class IntentKeys {

    private IntentKeys() { }

    public static final String EXTRA_ROOM_TYPE_ID = "room_type_id";   // int
    public static final String EXTRA_ROOM_ID      = "room_id";        // int
    public static final String EXTRA_BOOKING_ID   = "booking_id";     // int
    public static final String EXTRA_BOOKING_CODE = "booking_code";   // String
    public static final String EXTRA_CHECK_IN     = "check_in";       // long (epoch millis)
    public static final String EXTRA_CHECK_OUT    = "check_out";      // long (epoch millis)
    public static final String EXTRA_NUM_GUESTS   = "num_guests";     // int
    public static final String EXTRA_TAB_INDEX    = "tab_index";      // int
    public static final String EXTRA_KEYWORD      = "keyword";        // String
    public static final String EXTRA_TYPE_FILTER  = "type_filter";    // String
    public static final String EXTRA_PRICE_MIN    = "price_min";     // long
    public static final String EXTRA_PRICE_MAX    = "price_max";     // long
    public static final String EXTRA_AMENITY_IDS  = "amenity_ids";   // int[]
}