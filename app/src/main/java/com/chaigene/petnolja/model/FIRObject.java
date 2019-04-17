package com.chaigene.petnolja.model;

import androidx.annotation.Nullable;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class FIRObject implements Serializable {

    @Exclude
    @Nullable
    String key;

    @Exclude
    @Nullable
    public String getKey() {
        return key;
    }

    @Exclude
    public void setKey(@Nullable String key) {
        this.key = key;
    }
}