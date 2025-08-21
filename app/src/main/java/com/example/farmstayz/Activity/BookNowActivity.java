package com.example.farmstayz.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.example.farmstayz.Model.BookingRequest;
import com.example.farmstayz.Networks.RetrofitClient;
import com.example.farmstayz.Model.FarmhouseListResponse;
import com.example.farmstayz.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookNowActivity extends AppCompatActivity {

    private static final String TAG = "FarmStayZ";
    private EditText etCustomerPhone, etDocDate, etNumberOfGuests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_now);

        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etDocDate = findViewById(R.id.etDocDate);
        etNumberOfGuests = findViewById(R.id.etNumberOfGuests);
        Button bookNowBtn = findViewById(R.id.bookNowBtn);
        ImageView backArrow = findViewById(R.id.back_arrow);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", getResources().getConfiguration().locale);
        Calendar calendar = Calendar.getInstance();
        etDocDate.setKeyListener(null);
        etDocDate.setText(sdf.format(calendar.getTime()));

        // Handle date selection
        etDocDate.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(BookNowActivity.this);
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_calendar, null);
            builder.setView(dialogView);

            CalendarView calendarView = dialogView.findViewById(R.id.customCalendarView);
            Button btnOk = dialogView.findViewById(R.id.btnOk);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            String currentDateText = etDocDate.getText().toString();
            if (!TextUtils.isEmpty(currentDateText)) {
                try {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.setTime(sdf.parse(currentDateText));
                    calendarView.setDate(selectedCalendar.getTimeInMillis());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing current date: " + e.getMessage());
                }
            }

            final Calendar selectedCalendar = Calendar.getInstance();
            calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
                selectedCalendar.set(year, month, dayOfMonth);
            });

            AlertDialog dialog = builder.create();

            btnOk.setOnClickListener(view12 -> {
                String formattedDate = sdf.format(selectedCalendar.getTime());
                etDocDate.setText(formattedDate);
                Log.d(TAG, "etDocDate updated to: " + formattedDate);
                dialog.dismiss();
            });

            btnCancel.setOnClickListener(view13 -> dialog.dismiss());

            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
        });

        backArrow.setOnClickListener(v -> onBackPressed());

        bookNowBtn.setOnClickListener(v -> {
            if (validateInputs()) {
                sendBookingRequest();
            }
        });

        getWindow().setStatusBarColor(getResources().getColor(R.color.white));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etCustomerPhone.getText().toString().trim())) {
            etCustomerPhone.setError("Phone number is required");
            return false;
        }
        if (TextUtils.isEmpty(etDocDate.getText().toString().trim())) {
            etDocDate.setError("Date is required");
            return false;
        }
        if (TextUtils.isEmpty(etNumberOfGuests.getText().toString().trim())) {
            etNumberOfGuests.setError("Number of guests is required");
            return false;
        }

        try {
            int guests = Integer.parseInt(etNumberOfGuests.getText().toString().trim());
            int maxGuestCapacity = getIntent().getIntExtra("maxGuestCapacity", -1);
            if (guests <= 0) {
                etNumberOfGuests.setError("Number of guests must be positive");
                return false;
            }

            if (guests > maxGuestCapacity) {
                etNumberOfGuests.setError("Number of guests exceeds the maximum capacity");
                return false;
            }
        } catch (NumberFormatException e) {
            etNumberOfGuests.setError("Invalid number of guests");
            return false;
        }
        return true;
    }

    private void sendBookingRequest() {
        long farmhouseId = getIntent().getLongExtra("id", -1);
        String userUid = getIntent().getStringExtra("userUid");
        if (farmhouseId == -1) {
            Toast.makeText(this, "Invalid farmhouse ID", Toast.LENGTH_SHORT).show();
            return;
        }

        BookingRequest bookingRequest = new BookingRequest();
        BookingRequest.Farmhouse farmhouse = new BookingRequest.Farmhouse();
        farmhouse.setId(farmhouseId);
        bookingRequest.setHostUid(userUid);
        bookingRequest.setFarmhouse(farmhouse);
        bookingRequest.setCustomerPhone(etCustomerPhone.getText().toString().trim());
        bookingRequest.setDate(etDocDate.getText().toString().trim());
        bookingRequest.setNumberOfGuests(Integer.parseInt(etNumberOfGuests.getText().toString().trim()));

        Call<FarmhouseListResponse> call = RetrofitClient.getApiService().createBooking(bookingRequest);
        call.enqueue(new Callback<FarmhouseListResponse>() {
            @Override
            public void onResponse(Call<FarmhouseListResponse> call, Response<FarmhouseListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().getStatusCode() == 0 && "success".equalsIgnoreCase(response.body().getStatusMessage())) {
                        Log.d(TAG, "Booking created successfully");

                        etCustomerPhone.setText("");
                        etNumberOfGuests.setText("");

                        LayoutInflater inflater = LayoutInflater.from(BookNowActivity.this);
                        View dialogView = inflater.inflate(R.layout.dialog_booking_success, null);

                        AlertDialog.Builder builder = new AlertDialog.Builder(BookNowActivity.this);
                        builder.setView(dialogView);
                        AlertDialog successDialog = builder.create();

                        if (successDialog.getWindow() != null) {
                            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        }

                        Button btnOk = dialogView.findViewById(R.id.btnOk);
                        LottieAnimationView lottieSuccess=dialogView.findViewById(R.id.lottieSuccess);
                        lottieSuccess.setAnimation("success_animation.json");
                        lottieSuccess.playAnimation();
                        btnOk.setOnClickListener(v -> successDialog.dismiss());

                        successDialog.show();
                    }
                    else {
                        String errorMessage = response.body().getStatusMessage() != null ? response.body().getStatusMessage() : "Unknown error";
                        Log.e(TAG, "Failed to create booking: " + errorMessage);
                        Toast.makeText(BookNowActivity.this, "Failed to book: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMessage = "HTTP " + response.code() + (response.errorBody() != null ? ": " + response.message() : "");
                    Log.e(TAG, "POST failed: " + errorMessage);
                    Toast.makeText(BookNowActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<FarmhouseListResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(BookNowActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}