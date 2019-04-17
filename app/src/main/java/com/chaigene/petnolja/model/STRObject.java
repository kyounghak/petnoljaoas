package com.chaigene.petnolja.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.io.Serializable;

@IgnoreExtraProperties
public class STRObject implements Serializable {

    @Exclude
    String id;

    // DocumentSnapshot은 굳이 시리얼라이즈 할 필요가 없다. (Pagination cursor를 위해서만 존재)
    // Ref:https://stackoverflow.com/a/24824263/4729203
    @Exclude
    private transient DocumentSnapshot documentSnapshot;

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    @Exclude
    public DocumentSnapshot getDocumentSnapshot() {
        return documentSnapshot;
    }

    @Exclude
    public void setDocumentSnapshot(DocumentSnapshot documentSnapshot) {
        this.documentSnapshot = documentSnapshot;
    }
}