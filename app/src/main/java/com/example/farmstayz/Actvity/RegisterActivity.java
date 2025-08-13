package com.example.farmstayz.Actvity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.farmstayz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private TextView signInText;
    private ImageView profileImageView;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private SharedPreferences prefs;
    private Uri profileImageUri;
    private Bitmap profileBitmap;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        initializeComponents();
        setupListeners();
        configureWindowInsets();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        usernameEditText = findViewById(R.id.username_edit_text);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        registerButton = findViewById(R.id.register_button);
        signInText = findViewById(R.id.sign_in_text);
        profileImageView = findViewById(R.id.profile_image_view);
        progressBar = findViewById(R.id.progress_bar);

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(null);
                ivTogglePassword.setImageResource(R.drawable.visibility_off);
            } else {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.visibility);
            }
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        profileImageUri = result.getData().getData();
                        try {
                            profileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
                            profileImageView.setImageBitmap(profileBitmap);
                        } catch (Exception e) {
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        registerButton.setOnClickListener(v -> registerUser());
        signInText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void configureWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (isInputInvalid(username, email, password)) return;

        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            saveUserData(user, username, email);
                                            Intent intent = createNavigationIntent(user, username, email);
                                            startActivity(intent);
                                            finish();
                                            progressBar.setVisibility(View.GONE);
                                        } else {
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private boolean isInputInvalid(String username, String email, String password) {
        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return true;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return true;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return true;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return true;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return true;
        }
        return false;
    }

    private void saveUserData(FirebaseUser user, String username, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username_" + user.getUid(), username);
        editor.putString("email_" + user.getUid(), email);
        editor.apply();

        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", email);
        if (profileBitmap != null) {
            String filePath = saveBitmapToFile(profileBitmap, user.getUid());
            String profileImageUrl = bitmapToBase64(profileBitmap);
            userData.put("profileImageUrl", profileImageUrl);
        }

        db.child("users").child(user.getUid()).setValue(userData)
                .addOnSuccessListener(aVoid -> {})
                .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private Intent createNavigationIntent(FirebaseUser user, String username, String email) {
        Intent intent = new Intent(RegisterActivity.this, AvailableFarmHouseActivity.class);
        intent.putExtra("name", username);
        intent.putExtra("email", email);
        intent.putExtra("photoUrl", "");
        if (profileBitmap != null) {
            String filePath = saveBitmapToFile(profileBitmap, user.getUid());
            intent.putExtra("profileBitmapPath", filePath);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    private String saveBitmapToFile(Bitmap bitmap, String userId) {
        try {
            File file = new File(getFilesDir(), userId + "_profile.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}