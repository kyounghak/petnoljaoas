package com.chaigene.petnolja.model;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class FIRQuery extends FIRObject {

    String query;
    int page;

    public FIRQuery(String query, int page) {
        this.query = query;
        this.page = page;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
