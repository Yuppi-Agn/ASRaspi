package com.AS.Yuppi.Raspi.DataWorkers;

import androidx.annotation.Nullable;
import androidx.lifecycle.viewmodel.CreationExtras;

import java.util.ArrayList;
import java.util.List;

public class SchedulelController{
    private List<String> SchedulesList= new ArrayList<String>();
    private Schedules CurrentSchedule, editableSchedule;
    public SchedulelController(){}
    public Schedules geteditableSchedule(){
        if(editableSchedule== null)
            editableSchedule=new Schedules();
        return editableSchedule;
    }
    public Schedules createeditableSchedule(){
        return editableSchedule=new Schedules();
    }
    public void saveEditableSchedule(){
        if(editableSchedule==null) return;
        CurrentSchedule=editableSchedule;
        editableSchedule=null;

        notifyAction("Update");
    }
    public Schedules getCurrentSchedule(){
        return CurrentSchedule;
    }
    public void setCurrentSchedule(Schedules data){
         CurrentSchedule=data;
    }
    public Schedules createCurrentSchedule(){
        return CurrentSchedule=new Schedules();
    }
    public interface OnActionListener {
        void onAction(String data);
    }
    private final List<OnActionListener> listeners = new ArrayList<>();
    public void addOnActionListener(OnActionListener listener) {
        listeners.add(listener);
    }
    public void removeOnActionListener(OnActionListener listener) {
        listeners.remove(listener);
    }
    private void notifyAction(String data) {
        for (OnActionListener listener : listeners) {
            listener.onAction(data);
        }
    }
}
