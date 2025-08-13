package com.example.farmstayz.Actvity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.farmstayz.Adapter.FarmhouseAdapter;
import com.example.farmstayz.Model.Farmhouse;
import com.example.farmstayz.Model.FarmhouseListResponse;
import com.example.farmstayz.R;
import com.example.farmstayz.Networks.RetrofitClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvailableFarmHouseActivity extends AppCompatActivity {

    private static final String TAG = "AvailableFarmHouse";
    private ImageView profile;
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private SharedPreferences prefs;
    private RecyclerView recyclerView;
    private FarmhouseAdapter adapter;
    private List<Farmhouse> farmhouseList;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_avalable_farm_house);

        etSearch = findViewById(R.id.etSearch);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        profile = findViewById(R.id.profile);
        recyclerView = findViewById(R.id.recyclerView);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.getFilter().filter(s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        farmhouseList = new ArrayList<>();
        adapter = new FarmhouseAdapter(AvailableFarmHouseActivity.this, farmhouseList,farmhouseList);
        recyclerView.setAdapter(adapter);

        getAllFarmhouses();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = currentUser != null ? currentUser.getUid() : null;

        if (userId == null) {
            profile.setImageResource(R.drawable.ic_user);
            Log.d(TAG, "No user logged in, setting default profile image");
            return;
        }

        String username = prefs.getString("username_" + userId, null);
        String email = prefs.getString("email_" + userId, null);
        String photoUrl = prefs.getString("photoUrl_" + userId, null);
        String profileBitmapPath = getIntent().getStringExtra("profileBitmapPath");
        Bitmap localBitmap = loadBitmapFromInternalStorage(userId);

        if (isLocalDataEmpty(username, email, photoUrl, localBitmap, profileBitmapPath)) {
            loadFromFirebase(userId);
        } else {
            updateProfileView(username, email, photoUrl, profileBitmapPath, localBitmap);
        }

        setupProfileClickListener(username, email, photoUrl, profileBitmapPath);

        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        configureWindowInsets();
    }

    private void getAllFarmhouses() {
        Log.d(TAG, "Initiating GET /api/farmhouses request to " + RetrofitClient.getApiService().toString());
        Call<FarmhouseListResponse> call = RetrofitClient.getApiService().getAllFarmhouses();
        call.enqueue(new Callback<FarmhouseListResponse>() {
            @Override
            public void onResponse(Call<FarmhouseListResponse> call, Response<FarmhouseListResponse> response) {
                Log.d(TAG, "Response received: HTTP " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    FarmhouseListResponse farmhouseListResponse = response.body();
                    // Log raw JSON response
                    try {
                        String rawJson = new Gson().toJson(farmhouseListResponse);
                        Log.d(TAG, "Raw JSON response: " + rawJson);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to log raw JSON: " + e.getMessage());
                    }

                    if (farmhouseListResponse.getStatusCode() == 0 &&
                            "success".equalsIgnoreCase(farmhouseListResponse.getStatusMessage())) {
                        List<Farmhouse> farmhouses = farmhouseListResponse.getMessageBody();
                        if (farmhouses != null && !farmhouses.isEmpty()) {
                            farmhouseList.clear();
                            farmhouseList.addAll(farmhouses);
                            adapter = new FarmhouseAdapter(AvailableFarmHouseActivity.this, farmhouseList,farmhouseList);
                            recyclerView.setAdapter(adapter);
                            Log.d(TAG, "Success: Fetched " + farmhouses.size() + " farmhouses");
                            Toast.makeText(AvailableFarmHouseActivity.this, "Fetched " + farmhouses.size() + " farmhouses", Toast.LENGTH_SHORT).show();
                        } else {
                            farmhouseList.clear();
                            adapter.notifyDataSetChanged();
                            Log.w(TAG, "MessageBody is null or empty");
                            Toast.makeText(AvailableFarmHouseActivity.this, "No farmhouses found", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String errorMessage = farmhouseListResponse.getStatusMessage() != null
                                ? farmhouseListResponse.getStatusMessage()
                                : "Invalid status code or message";
                        Log.e(TAG, "API error: " + errorMessage);
                        Toast.makeText(AvailableFarmHouseActivity.this, "Fetch failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                } else {
                    String errorMessage = "HTTP " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += ": " + response.errorBody().string();
                        } catch (IOException e) {
                            errorMessage += ": Failed to read error body - " + e.getMessage();
                        }
                    } else {
                        errorMessage += ": No error body";
                    }
                    Log.e(TAG, "Response unsuccessful or null body: " + errorMessage);
                    Toast.makeText(AvailableFarmHouseActivity.this, "Fetch failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<FarmhouseListResponse> call, Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage(), t);
                Toast.makeText(AvailableFarmHouseActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isLocalDataEmpty(String username, String email, String photoUrl, Bitmap localBitmap, String profileBitmapPath) {
        return username == null && email == null && photoUrl == null && localBitmap == null && profileBitmapPath == null;
    }

    private void loadFromFirebase(String userId) {
        Log.d(TAG, "Loading user data from Firebase for userId: " + userId);
        db.getReference("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String photoUrl = dataSnapshot.child("photoUrl").getValue(String.class);
                    String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    SharedPreferences.Editor editor = prefs.edit();
                    if (username != null) editor.putString("username_" + userId, username);
                    if (email != null) editor.putString("email_" + userId, email);
                    if (photoUrl != null) editor.putString("photoUrl_" + userId, photoUrl);
                    editor.apply();

                    Bitmap firebaseBitmap = profileImageUrl != null ? base64ToBitmap(profileImageUrl) : null;
                    if (firebaseBitmap != null) {
                        saveBitmapToFile(firebaseBitmap, userId);
                    }

                    updateProfileView(username, email, photoUrl, firebaseBitmap != null ? saveBitmapToFile(firebaseBitmap, userId) : null, firebaseBitmap);
                } else {
                    Log.d(TAG, "No Firebase data for userId: " + userId);
                    profile.setImageResource(R.drawable.ic_user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());
                profile.setImageResource(R.drawable.ic_user);
            }
        });
    }

    private void updateProfileView(String username, String email, String photoUrl, String profileBitmapPath, Bitmap localBitmap) {
        Log.d(TAG, "Updating profile view");
        if (profileBitmapPath != null) {
            Bitmap bitmap = loadBitmapFromPath(profileBitmapPath);
            if (bitmap != null) {
                profile.setImageBitmap(bitmap);
            }
        } else if (localBitmap != null) {
            profile.setImageBitmap(localBitmap);
        } else if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).error(R.drawable.ic_user).into(profile);
        } else {
            profile.setImageResource(R.drawable.ic_user);
        }
    }

    private void setupProfileClickListener(String username, String email, String photoUrl, String profileBitmapPath) {
        profile.setOnClickListener(v -> {
            Log.d(TAG, "Profile clicked, navigating to ProfileActivity");
            Intent intent = new Intent(AvailableFarmHouseActivity.this, ProfileActivity.class);
            intent.putExtra("name", username != null ? username : getIntent().getStringExtra("name"));
            intent.putExtra("email", email != null ? email : getIntent().getStringExtra("email"));
            intent.putExtra("photoUrl", photoUrl != null ? photoUrl : getIntent().getStringExtra("photoUrl"));
            if (profileBitmapPath != null) {
                intent.putExtra("profileBitmapPath", profileBitmapPath);
            }
            startActivity(intent);
        });
    }

    private void configureWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private Bitmap loadBitmapFromInternalStorage(String userId) {
        try {
            FileInputStream fis = openFileInput(userId + "_profile.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            Log.d(TAG, "Loaded bitmap from internal storage for userId: " + userId);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap: " + e.getMessage());
            return null;
        }
    }

    private Bitmap loadBitmapFromPath(String filePath) {
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            Log.d(TAG, "Loaded bitmap from path: " + filePath);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap from path: " + e.getMessage());
            return null;
        }
    }

    private String saveBitmapToFile(Bitmap bitmap, String userId) {
        try {
            File file = new File(getFilesDir(), userId + "_profile.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.close();
            Log.d(TAG, "Saved bitmap to file: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save bitmap: " + e.getMessage());
            return null;
        }
    }

    private Bitmap base64ToBitmap(String base64Str) {
        try {
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decode base64: " + e.getMessage());
            return null;
        }
    }
}