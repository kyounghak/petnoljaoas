package com.chaigene.petnolja.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import com.kakao.usermgmt.LoginButton;
import com.chaigene.petnolja.R.layout;

public class KakaoLoginButton extends LoginButton {

    public KakaoLoginButton(Context context) {
        super(context);
    }

    public KakaoLoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KakaoLoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        inflate(this.getContext(), layout.kakao_login_layout, this);
    }
}