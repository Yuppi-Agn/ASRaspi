package com.AS.Yuppi.Raspi.ui.hometaskredactor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HometaskRedactorViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HometaskRedactorViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Это фрагмент редактора домашних заданий");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
