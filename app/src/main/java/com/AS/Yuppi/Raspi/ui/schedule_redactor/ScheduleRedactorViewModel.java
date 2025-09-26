package com.AS.Yuppi.Raspi.ui.schedule_redactor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ScheduleRedactorViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public ScheduleRedactorViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is gallery fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}