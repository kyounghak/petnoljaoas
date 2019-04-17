package com.chaigene.petnolja.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Quota extends STRObject {
    public static final String FIELD_SHOP_ID = "shopId";
    public static final String FIELD_YEAR = "year";
    public static final String FIELD_MONTH = "month";
    public static final String FIELD_DATE = "date";

    private String shopId;
    private int year;
    private int month;
    private int date;
    private int occupied;

    public Quota() {
    }

    public Quota(String shopId, int year, int month, int date, int occupied) {
        this.shopId = shopId;
        this.year = year;
        this.month = month;
        this.date = date;
        this.occupied = occupied;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getOccupied() {
        return occupied;
    }

    public void setOccupied(int occupied) {
        this.occupied = occupied;
    }

    @Override
    public String toString() {
        return "Quota{" +
                "id='" + id + '\'' +
                ", shopId='" + shopId + '\'' +
                ", year=" + year +
                ", month=" + month +
                ", date=" + date +
                ", occupied=" + occupied +
                '}';
    }
}