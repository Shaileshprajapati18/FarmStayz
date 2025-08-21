package com.example.farmstayz.Model;

import com.google.gson.annotations.SerializedName;

public class BookingRequest {
    @SerializedName("farmhouse")
    private Farmhouse farmhouse;

    @SerializedName("customerPhone")
    private String customerPhone;

    @SerializedName("hostUid")
    private String hostUid;

    @SerializedName("date")
    private String date;

    @SerializedName("numberOfGuests")
    private int numberOfGuests;

    public static class Farmhouse {
        @SerializedName("id")
        private long id;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }
    }

    public String getHostUid() {

        return hostUid;
    }

    public void setHostUid(String hostUid) {
        this.hostUid = hostUid;
    }

    public Farmhouse getFarmhouse() {
        return farmhouse;
    }

    public void setFarmhouse(Farmhouse farmhouse) {
        this.farmhouse = farmhouse;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }
}