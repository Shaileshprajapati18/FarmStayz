package com.example.farmstayz.Model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FarmhouseListResponse {

        @SerializedName("statusCode")
        private int statusCode;

        @SerializedName("statusMessage")
        private String statusMessage;

        @SerializedName("messageBody")
        private List<Farmhouse> messageBody;

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public List<Farmhouse> getMessageBody() {
            return messageBody;
        }

}
