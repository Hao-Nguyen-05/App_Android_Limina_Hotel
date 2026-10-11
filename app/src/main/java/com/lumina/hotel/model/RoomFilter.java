package com.lumina.hotel.model;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;
import java.util.TreeSet;

/**
 * Điều kiện lọc nâng cao cho màn Quản lý phòng (Admin).
 * Lưu bằng chuỗi ghép thủ công dạng "types=1,3;floors=2,4" (không dùng Gson).
 * Tập rỗng = không lọc theo tiêu chí đó.
 */
public class RoomFilter {

    private static final String PREFS_NAME = "lumina_filters";
    private static final String KEY = "filter_admin_room";

    private final Set<Integer> typeIds = new TreeSet<>();
    private final Set<Integer> floors = new TreeSet<>();

    public Set<Integer> getTypeIds() { return typeIds; }
    public Set<Integer> getFloors() { return floors; }

    /** Có ít nhất một điều kiện đang áp dụng không. */
    public boolean isActive() {
        return !typeIds.isEmpty() || !floors.isEmpty();
    }

    /** Xoá toàn bộ điều kiện (dùng cho nút "Đặt lại"). */
    public void reset() {
        typeIds.clear();
        floors.clear();
    }

    /** Bản sao độc lập, để bottom sheet chỉnh thử mà chưa ảnh hưởng filter đang áp dụng. */
    public RoomFilter copy() {
        RoomFilter f = new RoomFilter();
        f.typeIds.addAll(typeIds);
        f.floors.addAll(floors);
        return f;
    }

    // ---------------- CHUỖI GHÉP THỦ CÔNG ----------------

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        if (!typeIds.isEmpty()) sb.append("types=").append(join(typeIds));
        if (!floors.isEmpty()) {
            if (sb.length() > 0) sb.append(';');
            sb.append("floors=").append(join(floors));
        }
        return sb.toString();
    }

    public static RoomFilter parse(String raw) {
        RoomFilter f = new RoomFilter();
        if (raw == null || raw.isEmpty()) return f;
        for (String part : raw.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            if ("types".equals(kv[0])) fillInts(f.typeIds, kv[1]);
            else if ("floors".equals(kv[0])) fillInts(f.floors, kv[1]);
        }
        return f;
    }

    private static void fillInts(Set<Integer> target, String csv) {
        for (String s : csv.split(",")) {
            try {
                target.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
                // bỏ qua giá trị hỏng, không làm crash app
            }
        }
    }

    private static String join(Set<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (Integer v : values) {
            if (sb.length() > 0) sb.append(',');
            sb.append(v);
        }
        return sb.toString();
    }

    // ---------------- LƯU / ĐỌC SharedPreferences ----------------

    public static RoomFilter load(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return parse(sp.getString(KEY, ""));
    }

    public void save(Context context) {
        SharedPreferences.Editor ed =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (isActive()) ed.putString(KEY, serialize());
        else ed.remove(KEY);
        ed.apply();
    }
}