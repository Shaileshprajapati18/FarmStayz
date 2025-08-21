package com.example.farmstayz.Model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Farmhouse implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("userUid")
    private String userUid;

    @SerializedName("name")
    private String name;

    @SerializedName("maxGuestCapacity")
    private Integer maxGuestCapacity;

    @SerializedName("location")
    private String location;

    @SerializedName("googleMapLink")
    private String googleMapLink;

    @SerializedName("description")
    private String description;

    @SerializedName("contactNo")
    private String contactNo;

    @SerializedName("address")
    private String address;

    @SerializedName("bedrooms")
    private Integer bedrooms;

    @SerializedName("bathrooms")
    private Integer bathrooms;

    @SerializedName("rating")
    private Double rating;

    @SerializedName("perDayPrice")
    private Double perDayPrice;

    @SerializedName("perPersonPrice")
    private Double perPersonPrice;

    @SerializedName("images")
    private List<String> images;

    public Long getId() {
        return id;
    }

    public String getUserUid() {
        return userUid;
    }
    public Integer getBathrooms() {
        return bathrooms;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxGuestCapacity() {
        return maxGuestCapacity;
    }

    public String getLocation() {
        return location;
    }

    public String getGoogleMapLink() {
        return googleMapLink;
    }

    public String getDescription() {
        return description;
    }

    public String getContactNo() {
        return contactNo;
    }

    public String getAddress() {
        return address;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public Double getRating() {
        return rating;
    }

    public Double getPerDayPrice() {
        return perDayPrice;
    }

    public Double getPerPersonPrice() {
        return perPersonPrice;
    }

    public List<String> getImages() {
        return images;
    }
}