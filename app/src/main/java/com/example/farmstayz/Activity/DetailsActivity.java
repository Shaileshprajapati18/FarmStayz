package com.example.farmstayz.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.farmstayz.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    TextView tvTitle, tvLocation, tvDescription, tvGuests, tvBedrooms, tvBathrooms, tvContact, tvAddress, tvPricePerPerson, tvPricePerDay, tvRating, tvToggle, tvImageCounter;
    //ImageView profile;
    ImageView back_arrow;
    Button bookNowBtn;
    private static final String TAG = "DetailsActivity";
    private FirebaseDatabase db;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private ViewPager2 viewPagerImages;
    private ImageSliderAdapter imageSliderAdapter;
    String userId="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_details);

        getWindow().setStatusBarColor(getResources().getColor(android.R.color.white));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        viewPagerImages = findViewById(R.id.viewPagerImages);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        back_arrow = findViewById(R.id.back_arrow);
        tvTitle = findViewById(R.id.tvTitle);
        tvLocation = findViewById(R.id.tvLocation);
        tvDescription = findViewById(R.id.tvDescription);
        tvToggle = findViewById(R.id.tvToggle);
        tvGuests = findViewById(R.id.tvGuests);
        tvBedrooms = findViewById(R.id.tvBedrooms);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvContact = findViewById(R.id.tvContact);
        tvAddress = findViewById(R.id.tvAddress);
        tvRating = findViewById(R.id.tvRating);
        //profile = findViewById(R.id.profile);
        tvPricePerDay = findViewById(R.id.tvPricePerDay);
        tvPricePerPerson = findViewById(R.id.tvPricePerPerson);
        bookNowBtn = findViewById(R.id.bookNowBtn);

        back_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Intent intent = getIntent();

        Long id = intent.getLongExtra("id", -1);
        String userUid = intent.getStringExtra("userUid");
        String name = intent.getStringExtra("name");
        String location = intent.getStringExtra("location");
        String description = intent.getStringExtra("description");
        String contact = intent.getStringExtra("contactNo");
        String address = intent.getStringExtra("address");
        int guests = intent.getIntExtra("maxGuestCapacity", 0);
        int bedrooms = intent.getIntExtra("bedrooms", 0);
        int bathrooms = intent.getIntExtra("bathrooms", 0);
        String perDayPrice = intent.getStringExtra("perDayPrice");
        String perPersonPrice = intent.getStringExtra("perPersonPrice");
        Double rating = intent.getDoubleExtra("rating", 0.0);
        ArrayList<String> images = intent.getStringArrayListExtra("images");

        String fullText = description;
        tvDescription.setText(fullText);
        tvDescription.setMaxLines(2);
        tvDescription.setEllipsize(TextUtils.TruncateAt.END);
        tvToggle.setText("Read more");

        tvToggle.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;

            @Override
            public void onClick(View v) {
                if (expanded) {
                    tvDescription.setMaxLines(2);
                    tvDescription.setEllipsize(TextUtils.TruncateAt.END);
                    tvToggle.setText("Read more");
                } else {
                    tvDescription.setMaxLines(Integer.MAX_VALUE);
                    tvDescription.setEllipsize(null);
                    tvToggle.setText("Read less");
                }
                expanded = !expanded;
            }
        });

        bookNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(DetailsActivity.this,BookNowActivity.class);
                intent.putExtra("id",id);
                intent.putExtra("userUid",userUid);
                intent.putExtra("maxGuestCapacity",guests);
                startActivity(intent);
            }
        });
        tvTitle.setText(name);
        tvLocation.setText(location);
        tvRating.setText(rating + "");
        tvDescription.setText(description);
        tvGuests.setText("Maximum Guests: " + guests);
        tvBedrooms.setText("Bedrooms: " + bedrooms);
        tvBathrooms.setText("Bathrooms: " + bathrooms);
        tvContact.setText("Contact: " + contact);
        tvAddress.setText("Address: " + address);
        tvPricePerDay.setText(perDayPrice);
        tvPricePerPerson.setText(perPersonPrice);

        imageSliderAdapter = new ImageSliderAdapter(this, images != null ? images : new ArrayList<>());
        viewPagerImages.setAdapter(imageSliderAdapter);

        // Update image counter
        updateImageCounter(0, images != null ? images.size() : 0);
        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageCounter(position + 1, images != null ? images.size() : 0);
            }
        });

        if (images == null || images.isEmpty()) {
            viewPagerImages.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
         userId = currentUser != null ? currentUser.getUid() : null;

        if (userId == null) {
           // profile.setImageResource(R.drawable.ic_user);
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
            //updateProfileView(username, email, photoUrl, profileBitmapPath, localBitmap);
        }

       // setupProfileClickListener(username, email, photoUrl, profileBitmapPath);

        configureWindowInsets();
    }

    private void updateImageCounter(int current, int total) {
        tvImageCounter.setText(current + "/" + total);
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

                    //updateProfileView(username, email, photoUrl, firebaseBitmap != null ? saveBitmapToFile(firebaseBitmap, userId) : null, firebaseBitmap);
                } else {
                    Log.d(TAG, "No Firebase data for userId: " + userId);
                  //  profile.setImageResource(R.drawable.ic_user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase error: " + databaseError.getMessage());
               // profile.setImageResource(R.drawable.ic_user);
            }
        });
    }

//    private void updateProfileView(String username, String email, String photoUrl, String profileBitmapPath, Bitmap localBitmap) {
//        Log.d(TAG, "Updating profile view");
//        if (profileBitmapPath != null) {
//            Bitmap bitmap = loadBitmapFromPath(profileBitmapPath);
//            if (bitmap != null) {
//                profile.setImageBitmap(bitmap);
//            }
//        } else if (localBitmap != null) {
//            profile.setImageBitmap(localBitmap);
//        } else if (photoUrl != null && !photoUrl.isEmpty()) {
//            Glide.with(this).load(photoUrl).error(R.drawable.ic_user).into(profile);
//        } else {
//            profile.setImageResource(R.drawable.ic_user);
//        }
//    }

//    private void setupProfileClickListener(String username, String email, String photoUrl, String profileBitmapPath) {
//        profile.setOnClickListener(v -> {
//            Log.d(TAG, "Profile clicked, navigating to ProfileActivity");
//            Intent intent = new Intent(DetailsActivity.this, ProfileActivity.class);
//            intent.putExtra("name", username != null ? username : getIntent().getStringExtra("name"));
//            intent.putExtra("email", email != null ? email : getIntent().getStringExtra("email"));
//            intent.putExtra("photoUrl", photoUrl != null ? photoUrl : getIntent().getStringExtra("photoUrl"));
//            if (profileBitmapPath != null) {
//                intent.putExtra("profileBitmapPath", profileBitmapPath);
//            }
//            startActivity(intent);
//        });
//    }

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
    private static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ViewHolder> {
        private final List<String> images;
        private final DetailsActivity context;

        ImageSliderAdapter(DetailsActivity context, List<String> images) {
            this.context = context;
            this.images = images;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String imageUrl = images.get(position).replace("localhost", "192.168.153.1");;
            Log.d("ImageSliderAdapter", "Loading image: " + imageUrl);
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.img_serene_valley_villa)
                    .error(R.drawable.img_serene_valley_villa)
                    .fitCenter()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}