package com.example.farmstayz.Actvity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.farmstayz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth, Realtime Database, and SharedPreferences
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Add 2-second delay before checking authentication
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                fetchUserDataAndNavigate(currentUser);
            } else {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
               // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 2000); // 2-second delay

    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        Intent intent = new Intent(SplashActivity.this, AvailableFarmHouseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Load data from SharedPreferences
        String username = prefs.getString("username_" + user.getUid(), user.getDisplayName() != null ? user.getDisplayName() : "User");
        String email = prefs.getString("email_" + user.getUid(), user.getEmail() != null ? user.getEmail() : "");
        String photoUrl = prefs.getString("photoUrl_" + user.getUid(), user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

        intent.putExtra("name", username);
        intent.putExtra("email", email);
        intent.putExtra("photoUrl", photoUrl);

        // Load saved Bitmap for email/password users
        Bitmap savedBitmap = loadBitmapFromInternalStorage(user.getUid());
        if (savedBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            savedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            intent.putExtra("profileBitmap", baos.toByteArray());
        }

        // Start AvailableFarmHouseActivity with SharedPreferences data
        startActivity(intent);
        finish();

        // Sync with Firebase in the background
        db.getReference("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String firebaseUsername = dataSnapshot.child("username").getValue(String.class);
                    String firebaseEmail = dataSnapshot.child("email").getValue(String.class);
                    String firebasePhotoUrl = dataSnapshot.child("photoUrl").getValue(String.class);

                    // Update SharedPreferences if Firebase data differs
                    SharedPreferences.Editor editor = prefs.edit();
                    if (firebaseUsername != null && !firebaseUsername.equals(username)) {
                        editor.putString("username_" + user.getUid(), firebaseUsername);
                    }
                    if (firebaseEmail != null && !firebaseEmail.equals(email)) {
                        editor.putString("email_" + user.getUid(), firebaseEmail);
                    }
                    if (firebasePhotoUrl != null && !firebasePhotoUrl.equals(photoUrl)) {
                        editor.putString("photoUrl_" + user.getUid(), firebasePhotoUrl);
                    }
                    editor.apply();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private Bitmap loadBitmapFromInternalStorage(String userId) {
        try {
            FileInputStream fis = openFileInput(userId + "_profile.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}