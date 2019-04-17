package com.chaigene.petnolja.model;

public class Comment extends FIRComment {

    public Comment(FIRComment firComment) {
        this.key = firComment.getKey();
        this.uid = firComment.getUid();
        this.nickname = firComment.getNickname();
        this.content = firComment.getContent();
        this.timestamp = firComment.getTimestamp(true);
    }

    public Comment() {
    }
}