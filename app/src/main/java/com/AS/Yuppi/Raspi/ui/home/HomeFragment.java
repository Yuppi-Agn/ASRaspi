package com.AS.Yuppi.Raspi.ui.home;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.AS.Yuppi.Raspi.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.Calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private TextView TV_CurDay, TV_NextDay,
            TV_Time, TV_Times_1, TV_Times_2, TV_Schedules;
    private List<TextView> TextView_Weeks=new ArrayList<>();
    private TextView TV_Schedules_1;
    private SchedulelController SchedulelController;// = MySingleton.getInstance().getSchedulelController();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    //private Schedules controller_Schedules = new Schedules(1);
    private HorizontalScrollView HorizontalScrollView_Schedules;
    private String TimesToday="", TimesNextDay="";
    private Button button_load_schedule;
    private Spinner spinner_select_schedule;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        SchedulelController = MySingleton.getInstance(context.getApplicationContext())
                .getSchedulelController();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        //final TextView textView = binding.textHome;
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TV_CurDay = view.findViewById(R.id.TV_CurDay);
        TV_NextDay = view.findViewById(R.id.TV_NextDay);

        TV_Time = view.findViewById(R.id.TV_Time);
        TV_Times_1 = view.findViewById(R.id.TV_Times_1);
        TV_Times_2 = view.findViewById(R.id.TV_Times_2);
        TV_Schedules = view.findViewById(R.id.TV_Schedules);

        HorizontalScrollView_Schedules= view.findViewById(R.id.HorizontalScrollView_Schedules);
        TV_Schedules_1= view.findViewById(R.id.TV_Schedules_1);

        TextView_Weeks.clear();
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_1));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_2));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_3));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_4));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_5));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_6));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_2_7));

        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_1));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_2));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_3));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_4));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_5));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_6));
        TextView_Weeks.add(view.findViewById(R.id.TV_Schedules_3_7));

        SchedulelController.addOnActionListener(new SchedulelController.OnActionListener() {
            @Override
            public void onAction(String data) {
                System.out.println("Событие произошло, данные: " + data);
                UpdateData();
            }
        });

        TV_Times_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowTimesData(false, false);
            }
        });
        TV_Schedules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowHideWeeksData(false);
            }
        });

        button_load_schedule=view.findViewById(R.id.button_load_schedule);
        spinner_select_schedule=view.findViewById(R.id.spinner_select_schedule);

        button_load_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoadSchedule();
            }
        });
        button_load_schedule.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                LoadDBContbext();
                return true;
            }
        });

        runnable = new Runnable() {
            @Override
            public void run() {
                ShowTimeLost();
                // Запланировать повторный запуск через 1000 мс (1 сек)
                handler.postDelayed(this, 1000);
            }
        };
        ShowTimeLost();
        handler.postDelayed(runnable, getMillisUntilNextMinute());

        UpdateData();
    }
    private void LoadDBContbext(){
        List<String> Data =SchedulelController.fillSchedulesListFromDB();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Data
        );
        adapter.setDropDownViewResource(android.R.layout.select_dialog_item);
        spinner_select_schedule.setAdapter(adapter);
    }
    private void LoadSchedule(){
        SchedulelController.loadCurrentSchedule(spinner_select_schedule.getSelectedItem().toString());
        UpdateData();
    }
    private void UpdateData(){
        Schedules CurrentSchedule = SchedulelController.getCurrentSchedule();
        if(CurrentSchedule==null) return;

        int ThisDay=0, NextDay=0;
        if(CurrentSchedule.getCircle_Mode() ==1){
            if(CurrentSchedule.getFirstWeekId() == 1 && isCurrentWeekEven()){
                ThisDay =getCurrentDayOfWeek();
                if(ThisDay<7)
                    NextDay=ThisDay+1;
                else
                    NextDay=0;
            } else{
                ThisDay =getCurrentDayOfWeek();
                if(ThisDay<7)
                    NextDay=ThisDay+8;
                else
                    NextDay=7;
            }
        } else {
            ThisDay =getCurrentDayOfWeek();
            if(ThisDay<7)
                NextDay=ThisDay+1;
            else
                NextDay=0;
        }
        TV_CurDay.setText(CurrentSchedule.getDayLesson(ThisDay));
        TV_NextDay.setText(CurrentSchedule.getDayLesson(NextDay));

        ShowTimesData(true, true);
        ShowHideWeeksData(true);
        LoadDBContbext();
    }
    private void ShowHideWeeksData(boolean IsUpdateOnly){
        Schedules CurrentSchedule = SchedulelController.getCurrentSchedule();
        if(CurrentSchedule==null) return;

        if(HorizontalScrollView_Schedules.getVisibility() == View.GONE | IsUpdateOnly){
            if(CurrentSchedule.getFirstWeekId() == CurrentWeekEven()){
                TV_Schedules_1.setText("Сегодня первая неделя");
            }
            else{
                TV_Schedules_1.setText("Сегодня вторая неделя");
            }
            if(CurrentSchedule.getCircle_Mode() == 1){
                for(int i=0; i<CurrentSchedule.countDays_Schedule();i++)
                    TextView_Weeks.get(i).setText(CurrentSchedule.getDayLesson(i));
            }
            if(!IsUpdateOnly) HorizontalScrollView_Schedules.setVisibility(View.VISIBLE);
        }
        else
        if(!IsUpdateOnly) HorizontalScrollView_Schedules.setVisibility(View.GONE);
    }
    private void ShowTimesData(boolean IsNeedClear, boolean IsUpdateOnly){
        Schedules CurrentSchedule = SchedulelController.getCurrentSchedule();
        if(CurrentSchedule==null) return;

        if(IsNeedClear || Objects.equals(TimesToday, "") || Objects.equals(TimesNextDay, "")){
            int Day =getCurrentDayOfWeek(), NextDay;
            if(!(CurrentSchedule.getFirstWeekId() == CurrentWeekEven()) && CurrentSchedule.getCircle_Mode()==1) Day+=7;
            NextDay=Day+1;
            if(Day>14)
                Day=13;
            if(NextDay>14)
                NextDay=0;

            if(CurrentSchedule.getCircle_Mode()==1 &&Day>7)
                Day=6;
            if(CurrentSchedule.getCircle_Mode()==1 &&NextDay>7)
                NextDay=0;


            TimesToday=SetTimesData(Day);
            TimesNextDay=SetTimesData(NextDay);
        }
        if(TV_Times_2.getVisibility() == View.GONE || IsUpdateOnly){
            TV_Times_1.setText(TimesToday);
            TV_Times_2.setText(TimesNextDay);
            if(!IsUpdateOnly) TV_Times_2.setVisibility(View.VISIBLE);
        }
        else {
            TV_Times_1.setText("Расписание занятий?");
            if(!IsUpdateOnly) TV_Times_2.setVisibility(View.GONE);
        }
    }
    private String SetTimesData(int Day){
        Schedules CurrentSchedule = SchedulelController.getCurrentSchedule();
        if(CurrentSchedule==null) return "";

        String Data="";
        for(int i=0; i<CurrentSchedule.countLessons_StartTime(); i++)
        {
            int StartValue, EndValue;
            StartValue=CurrentSchedule.getLessons_StartTime(Day, i);
            EndValue=CurrentSchedule.getLessons_EndTime(Day, i);

            if(StartValue ==0 || EndValue==0 || EndValue<StartValue)
                break;

            Data+=minutesToTime(StartValue) +"-"+minutesToTime(EndValue)+
            "("+String.valueOf(EndValue-StartValue);

            if(i>0)
                Data+="|"+String.valueOf(StartValue-CurrentSchedule.getLessons_EndTime(Day, i-1));
            Data+=")\n";
        }
        return Data;
    }
    private void ShowTimeLost(){
        Schedules CurrentSchedule = SchedulelController.getCurrentSchedule();
        if(CurrentSchedule==null) return;

        String Data="";
        int Day =getCurrentDayOfWeek();
        if(!(CurrentSchedule.getFirstWeekId() == CurrentWeekEven()) && CurrentSchedule.getCircle_Mode()==1) Day+=7;
        if(Day>14)
            Day=13;
        if(CurrentSchedule.getCircle_Mode()==1 &&Day>7)
            Day=6;

        int CurTime=getMinutesSinceMidnight(),
        TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 0);
        if(CurTime>TimeToCheck){
            TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 0);
            if(CurTime>TimeToCheck){
                TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 1);
                if(CurTime>TimeToCheck){
                    TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 1);
                    if(CurTime>TimeToCheck){
                        TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 2);
                        if(CurTime>TimeToCheck){
                            TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 2);
                            if(CurTime>TimeToCheck){
                                TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 3);
                                if(CurTime>TimeToCheck){
                                    TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 3);
                                    if(CurTime>TimeToCheck){
                                        TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 4);
                                        if(CurTime>TimeToCheck){
                                            TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 4);
                                            if(CurTime>TimeToCheck){
                                                TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 5);
                                                if(CurTime>TimeToCheck){
                                                    TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 5);
                                                    if(CurTime>TimeToCheck){
                                                        TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 6);
                                                        if(CurTime>TimeToCheck){
                                                            TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 6);
                                                            if(CurTime>TimeToCheck){
                                                                TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 7);
                                                                if(CurTime>TimeToCheck){
                                                                    TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 7);
                                                                    if(CurTime>TimeToCheck){
                                                                        TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 8);
                                                                        if(CurTime>TimeToCheck){
                                                                            TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 8);
                                                                            if(CurTime>TimeToCheck){
                                                                                TimeToCheck=CurrentSchedule.getLessons_StartTime(Day, 9);
                                                                                if(CurTime>TimeToCheck){
                                                                                    TimeToCheck=CurrentSchedule.getLessons_EndTime(Day, 9);
                                                                                    if(CurTime>TimeToCheck){
                                                                                        Data="На сегодня занятия окончились";
                                                                                    }
                                                                                    else
                                                                                        Data="До конца десятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                                                }
                                                                                else
                                                                                    Data="До начала десятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                                            }
                                                                            else
                                                                                Data="До конца девятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                                        }
                                                                        else
                                                                            Data="До начала девятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                                    }
                                                                    else
                                                                        Data="До конца восьмой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                                }
                                                                else
                                                                    Data="До начала восьмой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                            }
                                                            else
                                                                Data="До конца седьмой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                        }
                                                        else
                                                            Data="До начала седьмой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                    }
                                                    else
                                                        Data="До конца шестой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                                }
                                                else
                                                    Data="До начала шейстой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                            }
                                            else
                                                Data="До конца пятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                        }
                                        else
                                            Data="До начала пятой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                    }
                                    else
                                        Data="До конца четвертой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                                }
                                else
                                    Data="До начала четвертой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                            }
                            else
                                Data="До конца третьей пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                        }
                        else
                            Data="До начала третьей пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                    }
                    else
                        Data="До конца второй пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
                }
                else
                    Data="До начала второй пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
            }
            else
                Data="До конца первой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
        } else
            Data="До начала первой пары "+String.valueOf(TimeToCheck-CurTime)+" мин.";
        TV_Time.setText(Data);
    }
    public long getMillisUntilNextMinute() {
        Calendar now = Calendar.getInstance();

        int seconds = now.get(Calendar.SECOND);
        int milliseconds = now.get(Calendar.MILLISECOND);

        // Вычисляем сколько миллисекунд прошло с начала текущей минуты
        long passedMillis = seconds * 1000 + milliseconds;

        // Милисекунд в минуте
        long millisInMinute = 60 * 1000;

        // Возвращаем остаток до начала следующей минуты
        return millisInMinute - passedMillis;
    }
    public int getMinutesSinceMidnight() {
        Calendar calendar = Calendar.getInstance(); // текущие дата и время
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // часы (0-23)
        int minute = calendar.get(Calendar.MINUTE);    // минуты (0-59)
        return hour * 60 + minute;
    }
    public static String minutesToTime(int totalMinutes) {
        int hours = (totalMinutes / 60) % 24;  // Часы по модулю 24
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
    public static int getCurrentDayOfWeek() {
        LocalDate today = LocalDate.now();
        return today.getDayOfWeek().getValue()-1; // 1 = понедельник, 7 = воскресенье
    }
    public static boolean isCurrentWeekEven() {
        //Возвращает true, если текущая неделя года — четная, false — если нечетная.
        Calendar calendar = Calendar.getInstance();
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        return (weekOfYear % 2) == 0;
    }
    public static int CurrentWeekEven() {
        //Возвращает true, если текущая неделя года — четная, false — если нечетная.
        Calendar calendar = Calendar.getInstance();
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        return (weekOfYear % 2);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}