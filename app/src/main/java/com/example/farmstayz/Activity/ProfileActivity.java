package com.example.farmstayz.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.farmstayz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView arrowBack, userProfileImage;
    private TextView userUsernameText, userEmailText;
    private Button signOutButton;
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth, Realtime Database, and SharedPreferences
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Initialize UI components
        arrowBack = findViewById(R.id.arrowBack);
        userProfileImage = findViewById(R.id.user_profile_image);
        userUsernameText = findViewById(R.id.user_username_text);
        userEmailText = findViewById(R.id.user_email_text);
        signOutButton = findViewById(R.id.sign_out_button);

        // Set status bar color
        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Load data from SharedPreferences
            String username = prefs.getString("username_" + currentUser.getUid(), currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
            String email = prefs.getString("email_" + currentUser.getUid(), currentUser.getEmail() != null ? currentUser.getEmail() : "No email");
            String photoUrl = prefs.getString("photoUrl_" + currentUser.getUid(), currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "");

            // Set profile image
            Bitmap savedBitmap = loadBitmapFromInternalStorage(currentUser.getUid());
            Intent intent = getIntent();
            byte[] bitmapData = intent.getByteArrayExtra("profileBitmap");

            if (bitmapData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                userProfileImage.setImageBitmap(bitmap);
            } else if (savedBitmap != null) {
                userProfileImage.setImageBitmap(savedBitmap);
            } else if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).error(R.drawable.ic_user).into(userProfileImage);
            } else {
                userProfileImage.setImageResource(R.drawable.ic_user);
            }

            // Set text fields with SharedPreferences data
            userUsernameText.setText(username);
            userEmailText.setText(email);

            // Sync with Firebase in the background
            db.getReference("users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String firebaseUsername = dataSnapshot.child("username").getValue(String.class);
                        String firebaseEmail = dataSnapshot.child("email").getValue(String.class);
                        String firebasePhotoUrl = dataSnapshot.child("photoUrl").getValue(String.class);

                        // Update SharedPreferences and UI if Firebase data differs
                        SharedPreferences.Editor editor = prefs.edit();
                        if (firebaseUsername != null && !firebaseUsername.equals(username)) {
                            editor.putString("username_" + currentUser.getUid(), firebaseUsername);
                            userUsernameText.setText(firebaseUsername);
                        }
                        if (firebaseEmail != null && !firebaseEmail.equals(email)) {
                            editor.putString("email_" + currentUser.getUid(), firebaseEmail);
                            userEmailText.setText(firebaseEmail);
                        }
                        if (firebasePhotoUrl != null && !firebasePhotoUrl.equals(photoUrl)) {
                            editor.putString("photoUrl_" + currentUser.getUid(), firebasePhotoUrl);
                        }
                        editor.apply();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            // No user logged in, use Intent data or defaults
            Intent intent = getIntent();
            String username = intent.getStringExtra("name") != null ? intent.getStringExtra("name") : "User";
            String email = intent.getStringExtra("email") != null ? intent.getStringExtra("email") : "No email";
            String photoUrl = intent.getStringExtra("photoUrl") != null ? intent.getStringExtra("photoUrl") : "";
            byte[] bitmapData = intent.getByteArrayExtra("profileBitmap");

            if (bitmapData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                userProfileImage.setImageBitmap(bitmap);
            } else if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).error(R.drawable.ic_user).into(userProfileImage);
            } else {
                userProfileImage.setImageResource(R.drawable.ic_user);
            }

            userUsernameText.setText(username);
            userEmailText.setText(email);
        }

        // Sign-out button listener
        signOutButton.setOnClickListener(v -> {
            if (currentUser != null) {
                // Clear SharedPreferences for this user
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("username_" + currentUser.getUid());
                editor.remove("email_" + currentUser.getUid());
                editor.remove("photoUrl_" + currentUser.getUid());
                editor.apply();
            }
            mAuth.signOut();
            Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        });

        // Back button listener
        arrowBack.setOnClickListener(v -> finish());

        // Handle window insets
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
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}