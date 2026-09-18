package com.lumina.hotel;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        com.lumina.hotel.util.AppExecutors.io(() -> {
            com.lumina.hotel.data.dao.RoomTypeDao typeDao = new com.lumina.hotel.data.dao.RoomTypeDao(this);
            com.lumina.hotel.data.dao.RoomDao roomDao = new com.lumina.hotel.data.dao.RoomDao(this);
            com.lumina.hotel.data.dao.BookingDao bookingDao = new com.lumina.hotel.data.dao.BookingDao(this);
            android.util.Log.d("SEED", "Loại phòng: " + typeDao.countTypes()
                    + " | Phòng: " + roomDao.countAllRooms()
                    + " | Trạng thái: " + roomDao.countByStatus()
                    + " | Đơn: " + bookingDao.countByStatus());
        });
    }
}