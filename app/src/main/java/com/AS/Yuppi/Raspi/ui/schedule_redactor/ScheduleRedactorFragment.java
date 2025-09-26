package com.AS.Yuppi.Raspi.ui.schedule_redactor;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.AS.Yuppi.Raspi.DataWorkers.DeviceUtils;
import com.AS.Yuppi.Raspi.DataWorkers.FileUtil;
import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentScheduleRedactorBinding;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ScheduleRedactorFragment extends Fragment {

    private FragmentScheduleRedactorBinding binding;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SchedulelController SchedulelController;//= MySingleton.getInstance().getSchedulelController();
    private int WeekOnScreen = 0;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        SchedulelController = MySingleton.getInstance(context.getApplicationContext())
                .getSchedulelController();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ScheduleRedactorViewModel galleryViewModel =
                new ViewModelProvider(this).get(ScheduleRedactorViewModel.class);

        binding = FragmentScheduleRedactorBinding.inflate(inflater, container, false);

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        List<Uri> uris = new ArrayList<>();

                        Intent data = result.getData();
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                uris.add(uri);
                            }
                        } else {
                            Uri uri = data.getData();
                            if (uri != null) {
                                uris.add(uri);
                            }
                        }
                        try {
                            String Content = FileUtil.readTextFromUri(getActivity(), uris.get(0));
                            processFileRaspi(Content);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    } else {

                        //onFilePickCancelled();
                    }
                });

        View root = binding.getRoot();
        return root;
    }
    private Spinner spinner_pageselect,
            spinner_circlemode,
            spinner_time_dayselect;
    private List<EditText> ListDays= new ArrayList<>();
    private List<EditText> ListTimesStart= new ArrayList<>();
    private List<EditText> ListTimesEnd= new ArrayList<>();
    private EditText EditText_StartTime, EditText_EndTime;
    private AutoCompleteTextView autoCompleteTextView_author;
    private TextView TextView_timelist;
    private Button button_fileload_filechecker, button_changeweek, button_firstweekchange, button_save_load,
            button_time_saveday,button_time_clear;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //layout_daychanger layout_timechanger
        LinearLayout layout_daychanger= view.findViewById(R.id.layout_daychanger);
        LinearLayout layout_timechanger= view.findViewById(R.id.layout_timechanger);
        layout_daychanger.setVisibility(View.VISIBLE);
        layout_timechanger.setVisibility(View.GONE);


        spinner_pageselect= view.findViewById(R.id.spinner_pageselect);
        {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.spinner_pageselect_items, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_pageselect.setAdapter(adapter);
        }
        spinner_pageselect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    default:
                    case 0:
                        layout_daychanger.setVisibility(View.VISIBLE);
                        layout_timechanger.setVisibility(View.GONE);
                        break;
                    case 1:
                        layout_daychanger.setVisibility(View.GONE);
                        layout_timechanger.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        layout_daychanger.setVisibility(View.GONE);
                        layout_timechanger.setVisibility(View.GONE);
                        break;

                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner_circlemode= view.findViewById(R.id.spinner_circlemode);
        {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.spinner_circlemode_items, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_circlemode.setAdapter(adapter);
        }

        spinner_time_dayselect= view.findViewById(R.id.spinner_time_dayselect);
        {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.spinner_time_dayselect_items, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_time_dayselect.setAdapter(adapter);
        }
        spinner_time_dayselect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Object selectedItem = parent.getItemAtPosition(position);
                ChangedTimeList(position+WeekOnScreen*7);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ListDays.add(view.findViewById(R.id.editText_day1));
        ListDays.add(view.findViewById(R.id.editText_day2));
        ListDays.add(view.findViewById(R.id.editText_day3));
        ListDays.add(view.findViewById(R.id.editText_day4));
        ListDays.add(view.findViewById(R.id.editText_day5));
        ListDays.add(view.findViewById(R.id.editText_day6));
        ListDays.add(view.findViewById(R.id.editText_day7));

        ListTimesStart.add(view.findViewById(R.id.editText_time_start_1));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_2));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_3));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_4));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_5));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_6));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_7));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_8));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_9));
        ListTimesStart.add(view.findViewById(R.id.editText_time_start_10));

        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_1));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_2));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_3));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_4));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_5));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_6));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_7));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_8));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_9));
        ListTimesEnd.add(view.findViewById(R.id.editText_time_end_10));

        EditText_StartTime=view.findViewById(R.id.editTextDate_startdate);
        EditText_EndTime=view.findViewById(R.id.editTextDate_enddate);

        button_changeweek=view.findViewById(R.id.button_changeweek);
        button_firstweekchange=view.findViewById(R.id.button_firstweekchange);
        button_save_load=view.findViewById(R.id.button_save_load);
        autoCompleteTextView_author=view.findViewById(R.id.autoCompleteTextView_author);

        button_fileload_filechecker=view.findViewById(R.id.button_fileload_filechecker);
        button_fileload_filechecker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFilePicker(false, "*/*");
            }
        });

        TextView_timelist = view.findViewById(R.id.TextView_timelist);
        button_time_saveday=view.findViewById(R.id.button_time_saveday);
        button_time_saveday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveTimeDay();
            }
        });
        button_time_saveday.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SaveTimeAllDays();
                return true;
            }
        });
        button_time_clear=view.findViewById(R.id.button_time_clear);
        button_time_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClearTimes();
            }
        });
        button_time_clear.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WriteTimesFromSaved(-1);
                return true;
            }
        });
        button_changeweek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeWeek();
            }
        });
        button_firstweekchange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeFirstWeekID();
            }
        });
        button_save_load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
                if(CurrentSchedule==null) return;

                CurrentSchedule.setName(autoCompleteTextView_author.getText().toString());
                CurrentSchedule.setAuthor(MySingleton.getHashedDeviceId());

                LocalDate StartDate, EndDate;
                StartDate= getLocalDateFromString(EditText_StartTime.getText().toString());
                EndDate= getLocalDateFromString(EditText_EndTime.getText().toString());

                CurrentSchedule.setStart_Date(StartDate);
                CurrentSchedule.setEnd_Date(EndDate);

                SchedulelController.saveEditableSchedule();
            }
        });

        /*
        // Обработка обычного нажатия
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        // Обработка удержания (долгого нажатия)
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true; //вернуть true, если событие обработано и не нужно вызывать onClick после удержания
            }
        });
        */
        //TV_CurDay.setText("TV_CurDay");
        /*
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button click
            }
        });
         */
    }
    private LocalDate getLocalDateFromString(String Date){
        LocalDate localDate=null;
        try {
            localDate = LocalDate.parse(Date, formatter);
        } catch (java.time.format.DateTimeParseException e) {
            System.err.println("Ошибка при парсинге даты: " + e.getMessage());
        }
        return localDate;
    }
    private void ChangeFirstWeekID(){
        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;

        String tb2Text = button_firstweekchange.getText().toString();

        if(CurrentSchedule.getFirstWeekId() ==0){
            CurrentSchedule.setFirstWeekId(1);
            button_firstweekchange.setText(tb2Text.replace("1 неделя четная", "1 неделя нечетная"));
        } else {
            CurrentSchedule.setFirstWeekId(0);
            button_firstweekchange.setText(tb2Text.replace("1 неделя нечетная", "1 неделя четная"));
        }

    }
    private void launchFilePicker(boolean allowMultiple, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType != null ? mimeType : "*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);

        filePickerLauncher.launch(intent);
    }
    void processFileRaspi(String content){
        int weekip;
        String FileContent=content;
                /*try {
                    FileContent = FileUtil.readFile(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }*/
        List<String> export = new Gson().fromJson(FileContent, new TypeToken<ArrayList<String>>(){}.getType());

        if (export == null || export.size() < 2) {
            return;
        }

        List<String> Vremechko_T = new Gson().fromJson(export.get(0), new TypeToken<ArrayList<String>>(){}.getType());
        List<String> Raspisa = new Gson().fromJson(export.get(1), new TypeToken<ArrayList<String>>(){}.getType());

        HashMap<String, Object> specialDays = null;
        if (export.size() > 2) {
            specialDays = new Gson().fromJson(export.get(2), new TypeToken<HashMap<String, Object>>(){}.getType());
        }

        weekip = Integer.parseInt(Vremechko_T.get(0));
        if (weekip == 1) {
            for (int i = 0; i < 7; i++)
                ListDays.get(i).setText(Raspisa.get(i + 1));  // берём с 1 по 7 индексы
        } else {
            for (int i = 0; i < 7; i++)
                ListDays.get(i).setText(Raspisa.get(i + 8));  // берём с 8 по 14 индексы
        }

        String tb2Text = button_firstweekchange.getText().toString();
        if (weekip == 0) {
            button_firstweekchange.setText(tb2Text.replace("1 неделя нечетная", "1 неделя четная"));
        } else {
            button_firstweekchange.setText(tb2Text.replace("1 неделя четная", "1 неделя нечетная"));
        }
        {
            int CurId=0;
            for (int i2 = 1; i2 < (Vremechko_T.size()-1)/14; i2+=2)
            {
                ListTimesStart.get(CurId).setText(Vremechko_T.get(i2));
                ListTimesEnd.get(CurId).setText(Vremechko_T.get(i2+1));
                CurId++;
            }
        }

        SchedulelController.createCurrentSchedule();
        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        CurrentSchedule.setCircle_Mode(1);//14
        CurrentSchedule.setFirstWeekId(weekip);
        for (int i = 0; i < 14; i++) CurrentSchedule.setDayLesson(Raspisa.get(i + 1),i);
        {
            int oldlistsize = (Vremechko_T.size()-1)/14;
            int oldlist_count=1;

            for (int i1 = 0; i1 < 14; i1++){
                int CurId=0;
                for (int i2 = 1; i2 < oldlistsize; i2+=2)
                {
                    CurrentSchedule.setLesson_StartTime(i1, CurId, Integer.parseInt(Vremechko_T.get(oldlist_count)));
                    CurrentSchedule.setLesson_EndTime(i1, CurId, Integer.parseInt(Vremechko_T.get(oldlist_count+1)));
                    CurId++;
                    oldlist_count+=2;
                }
            }
        }
    }
    private void UpdateScreen(){
        ShowDays();
    }
    private void ChangeWeek(){
        int oldmean=WeekOnScreen;
        switch (WeekOnScreen){
            default:
            case 0:
                WeekOnScreen=1;
                break;
            case 1:
                WeekOnScreen=0;
                break;
        }

        String ButtonText=button_changeweek.getText().toString();
        ButtonText = ButtonText.replace(String.valueOf(oldmean+1),String.valueOf(WeekOnScreen+1));
        button_changeweek.setText(ButtonText);

        UpdateScreen();
    }
    private void ShowDays(){
        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;

        for(int i=0;i<7;i++)
            ListDays.get(i).setText(CurrentSchedule.getDayLesson(i+WeekOnScreen*7));
    }
    private void WriteTimesFromSaved(int Day){
        if(Day==-1) Day=spinner_time_dayselect.getSelectedItemPosition()+WeekOnScreen*7;

        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;

        for (int i=0; i<ListTimesStart.size();i++){
            ListTimesStart.get(i).setText(minutesToTime(CurrentSchedule.getLessons_StartTime(Day, i)));
            ListTimesEnd.get(i).setText(minutesToTime(CurrentSchedule.getLessons_EndTime(Day, i)));
        }
    }
    private void ClearTimes(){
        for (int i=0; i<ListTimesStart.size();i++){
            ListTimesStart.get(i).setText("");
            ListTimesEnd.get(i).setText("");
        }
    }
    private void SaveTimeAllDays(){
        int Day=spinner_time_dayselect.getSelectedItemPosition()+WeekOnScreen*7;

        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;

        List<Integer>StartTimes=new ArrayList<>();
        List<Integer>EndTimes=new ArrayList<>();
        for (int i=0; i<10;i++){
            StartTimes.add(parseNumberOrTime(ListTimesStart.get(i).getText().toString()));
            EndTimes.add(parseNumberOrTime(ListTimesEnd.get(i).getText().toString()));
        }
        for(int i1=0;i1<CurrentSchedule.countDays_Schedule();i1++)
            for(int i2=0;i2<CurrentSchedule.countLessons_StartTime();i2++)
            {
                CurrentSchedule.setLesson_StartTime(i1,i2,StartTimes.get(i2));
                CurrentSchedule.setLesson_EndTime(i1,i2,EndTimes.get(i2));
            }
        ChangedTimeList(-1);
    }
    private void SaveTimeDay(){//(int Day){
        //button_time_saveday
        //if(Day==-1)
        int Day=spinner_time_dayselect.getSelectedItemPosition()+WeekOnScreen*7;

        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;
        for (int i=0; i<10;i++){
            int StartTime=parseNumberOrTime(ListTimesStart.get(i).getText().toString());
            int EndTime=parseNumberOrTime(ListTimesEnd.get(i).getText().toString());

            CurrentSchedule.setLesson_StartTime(Day,i,StartTime);
            CurrentSchedule.setLesson_EndTime(Day,i,EndTime);
        }
        ChangedTimeList(-1);
    }
    public static int parseNumberOrTime(String input) {
        if (input == null) return 0;

        // Удаляем все символы, кроме цифр и двоеточий
        String cleaned = input.replaceAll("[^0-9:]", "");
        if (cleaned.isEmpty()) return 0;

        try {
            if (cleaned.contains(":")) {
                // Парсим как время HH:mm
                String[] parts = cleaned.split(":");
                if (parts.length != 2) return 0;

                String hh = parts[0];
                String mm = parts[1];

                // Проверяем, что части не пустые и состоят только из цифр
                if (!hh.matches("\\d+") || !mm.matches("\\d+")) return 0;

                int hours = Integer.parseInt(hh);
                int minutes = Integer.parseInt(mm);

                //if (hours < 0 || hours > 23) return -1;
                //if (minutes < 0 || minutes > 59) return -1;
                if (hours>23)
                    hours=23;
                else if (hours<0)
                    hours=0;

                if (minutes>59)
                    minutes=59;
                else if (minutes<0)
                    minutes=0;

                return hours * 60 + minutes;
            } else {
                // Парсим как число
                if (!cleaned.matches("\\d+")) return 0;
                return Integer.parseInt(cleaned);
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private void ChangedTimeList(int Day){
        if(Day==-1) Day=spinner_time_dayselect.getSelectedItemPosition()+WeekOnScreen*7;

        Schedules CurrentSchedule = SchedulelController.geteditableSchedule();
        if(CurrentSchedule==null) return;
        String Output="";
        int SchedulesCount = CurrentSchedule.countLessons_StartTime();
        for (int i=0; i<SchedulesCount; i++)
            Output+=minutesToTime(CurrentSchedule.getLessons_StartTime(Day, i)) +"-"+minutesToTime(CurrentSchedule.getLessons_EndTime(Day, i))+'\n';
        TextView_timelist.setText(Output);
    }
    public static String minutesToTime(int totalMinutes) {
        int hours = (totalMinutes / 60) % 24;  // Часы по модулю 24
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}