package com.example.farmstayz.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.farmstayz.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button signInButton;
    LinearLayout loginwithgoogle_layout;
    private TextView forgotPasswordText, signUpText;
    private ProgressBar progressBar;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseDatabase db;
    private SharedPreferences prefs;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        signInButton = findViewById(R.id.sign_in_button);
        loginwithgoogle_layout = findViewById(R.id.loginwithgoogle_layout);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
        signUpText = findViewById(R.id.sign_up_text);
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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(v -> signInWithEmail());
        loginwithgoogle_layout.setOnClickListener(v -> signInWithGoogle());
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
        forgotPasswordText.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Enter your email to reset password");
                emailEditText.requestFocus();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> progressBar.setVisibility(View.GONE));
        });

        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void signInWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserDataAndNavigate(user);
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void signInWithGoogle() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                progressBar.setVisibility(View.VISIBLE);
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            } catch (ApiException e) {
                Log.e("LoginActivity", "Google Sign-In failed: " + e.getStatusCode());
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("username_" + user.getUid(), account.getDisplayName() != null ? account.getDisplayName() : "User");
                            editor.putString("email_" + user.getUid(), account.getEmail() != null ? account.getEmail() : "");
                            String photoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "";
                            editor.putString("photoUrl_" + user.getUid(), photoUrl);
                            editor.apply();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("username", account.getDisplayName() != null ? account.getDisplayName() : "User");
                            userData.put("email", account.getEmail() != null ? account.getEmail() : "");
                            userData.put("photoUrl", photoUrl);
                            db.getReference("users").child(user.getUid()).setValue(userData)
                                    .addOnSuccessListener(aVoid -> fetchUserDataAndNavigate(user))
                                    .addOnFailureListener(e -> {
                                        fetchUserDataAndNavigate(user);
                                        progressBar.setVisibility(View.GONE);
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e("LoginActivity", "Firebase auth with Google failed: " + task.getException().getMessage());
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchUserDataAndNavigate(FirebaseUser user) {
        Intent intent = new Intent(LoginActivity.this, AvailableFarmHouseActivity.class);
    //    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        String username = prefs.getString("username_" + user.getUid(), user.getDisplayName() != null ? user.getDisplayName() : "User");
        String email = prefs.getString("email_" + user.getUid(), user.getEmail() != null ? user.getEmail() : "");
        String photoUrl = prefs.getString("photoUrl_" + user.getUid(), user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

        intent.putExtra("name", username);
        intent.putExtra("email", email);
        intent.putExtra("photoUrl", photoUrl);

        Bitmap savedBitmap = loadBitmapFromInternalStorage(user.getUid());
        if (savedBitmap != null) {
            String filePath = saveBitmapToFile(savedBitmap, user.getUid());
            intent.putExtra("profileBitmapPath", filePath);
            String profileImageUrl = bitmapToBase64(savedBitmap);
            Map<String, Object> userData = new HashMap<>();
            userData.put("profileImageUrl", profileImageUrl);
            db.getReference("users").child(user.getUid()).updateChildren(userData);
        } else {
            db.getReference("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        if (profileImageUrl != null) {
                            Bitmap bitmap = base64ToBitmap(profileImageUrl);
                            String filePath = saveBitmapToFile(bitmap, user.getUid());
                            intent.putExtra("profileBitmapPath", filePath);
                        }
                    }
                    startActivity(intent);
                    finish();
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    startActivity(intent);
                    finish();
                    progressBar.setVisibility(View.GONE);
                }
            });
        }

        startActivity(intent);
        finish();
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