package com.AS.Yuppi.Raspi.ui.addhometask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AddHometaskViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AddHometaskViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Это фрагмент добавления задания");
    }

    public LiveData<String> getText() {
        return mText;
    }

}
