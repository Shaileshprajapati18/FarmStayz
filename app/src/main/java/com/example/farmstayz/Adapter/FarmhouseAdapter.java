package com.example.farmstayz.Adapter;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.farmstayz.Activity.DetailsActivity;
import com.example.farmstayz.Model.Farmhouse;
import com.example.farmstayz.R;

import java.util.ArrayList;
import java.util.List;

public class FarmhouseAdapter extends RecyclerView.Adapter<FarmhouseAdapter.FarmhouseViewHolder> {

    private final Activity activity;
    private List<Farmhouse> farmhouseList;
    private List<Farmhouse> farmhouseListFull;

    public FarmhouseAdapter( Activity activity, List<Farmhouse> farmhouseList,List<Farmhouse> farmhouseListFull) {
        this.activity = activity;
        this.farmhouseList =new ArrayList<>(farmhouseList);
        this.farmhouseListFull = new ArrayList<>(farmhouseListFull);
    }

    public Filter getFilter() {
        return FarmhouseFilter;
    }

    private final Filter FarmhouseFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Farmhouse> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(farmhouseListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Farmhouse farmhouse : farmhouseListFull) {
                    if (farmhouse.getName().toLowerCase().contains(filterPattern)
                            || farmhouse.getAddress().toLowerCase().contains(filterPattern)) {
                        filteredList.add(farmhouse);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            farmhouseList.clear();
            farmhouseList.addAll((List<Farmhouse>) results.values);
            notifyDataSetChanged();
        }
    };

    @NonNull
    @Override
    public FarmhouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.farmhouse_item, parent, false);

        return new FarmhouseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FarmhouseViewHolder holder, int position) {
        Farmhouse farmhouse = farmhouseList.get(position);
        holder.tvName.setText(farmhouse.getName());
        holder.tvLocation.setText(farmhouse.getLocation());
        holder.tvGuests.setText(farmhouse.getMaxGuestCapacity() + " Guests");
        holder.tvBedrooms.setText(farmhouse.getBedrooms() + " Bedrooms");
        holder.tvRating.setText(farmhouse.getRating()+"");
        holder.tvPricePerDay.setText(String.format("₹%.0f", farmhouse.getPerDayPrice()));
        holder.tvDescription.setText(farmhouse.getDescription() != null ? farmhouse.getDescription() : "No description");

        String fullText = farmhouse.getDescription();
        holder.tvDescription.setText(fullText);
        holder.tvDescription.setMaxLines(1);
        holder.tvDescription.setEllipsize(TextUtils.TruncateAt.END);
        holder.tvToggle.setText("Read more");

        holder.tvToggle.setOnClickListener(new View.OnClickListener() {
            boolean expanded = false;

            @Override
            public void onClick(View v) {
                if (expanded) {
                    holder.tvDescription.setMaxLines(1);
                    holder.tvDescription.setEllipsize(TextUtils.TruncateAt.END);
                    holder.tvToggle.setText("Read more");
                } else {
                    holder.tvDescription.setMaxLines(Integer.MAX_VALUE);
                    holder.tvDescription.setEllipsize(null);
                    holder.tvToggle.setText("Read less");
                }
                expanded = !expanded;
            }
        });
        List<String> images = farmhouse.getImages();
        if (images != null && !images.isEmpty()) {
            String imageUrl = images.get(0).replace("localhost", "192.168.153.1");
            Log.d("FarmhouseAdapter", "Loading image: " + imageUrl);
            Glide.with(activity)
                    .load(imageUrl)
                    .placeholder(R.drawable.img_serene_valley_villa)
                    .error(R.drawable.img_serene_valley_villa)
                    .fitCenter()
                    .into(holder.ivImage);
        } else {
            Log.w("FarmhouseAdapter", "No images for: " + farmhouse.getName());
            holder.ivImage.setImageResource(R.drawable.img_serene_valley_villa);
        }

        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(activity, DetailsActivity.class);

            intent.putExtra("id", farmhouse.getId());
            intent.putExtra("userUid", farmhouse.getUserUid());
            intent.putExtra("name", farmhouse.getName());
            intent.putExtra("maxGuestCapacity", farmhouse.getMaxGuestCapacity());
            intent.putExtra("address", farmhouse.getAddress());
            intent.putExtra("bedrooms", farmhouse.getBedrooms());
            intent.putExtra("bathrooms", farmhouse.getBathrooms());
            intent.putExtra("description", farmhouse.getDescription());
            intent.putExtra("contactNo", farmhouse.getContactNo());
            intent.putExtra("googleMapLink", farmhouse.getGoogleMapLink());
            intent.putExtra("location", farmhouse.getLocation());
            intent.putExtra("perDayPrice", String.format("₹%.0f", farmhouse.getPerDayPrice()));
            intent.putExtra("perPersonPrice", String.format("₹%.0f",farmhouse.getPerPersonPrice()));
            intent.putExtra("rating", farmhouse.getRating());
            intent.putStringArrayListExtra("images", new ArrayList<>(images));

            activity.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return farmhouseList.size();
    }

    static class FarmhouseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvLocation, tvGuests, tvBedrooms, tvDescription,tvPricePerDay,tvRating,tvToggle;
        CardView cardView;

        FarmhouseViewHolder(@NonNull View itemView) {
            super(itemView);

            ivImage = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvGuests = itemView.findViewById(R.id.tvGuests);
            tvBedrooms = itemView.findViewById(R.id.tvBedrooms);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            cardView = itemView.findViewById(R.id.cardView);
            tvPricePerDay = itemView.findViewById(R.id.tvPricePerDay);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvToggle = itemView.findViewById(R.id.tvToggle);
        }
    }
}