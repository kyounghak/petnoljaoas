package com.chaigene.petnolja.ui.helper;

import com.chaigene.petnolja.ui.fragment.ChildFragment;

import java.io.Serializable;

public interface InitialFragmentHelper extends Serializable {
    ChildFragment onInit();
}