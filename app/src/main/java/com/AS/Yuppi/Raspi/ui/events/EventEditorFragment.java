package com.AS.Yuppi.Raspi.ui.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.UserController;
import com.AS.Yuppi.Raspi.DataWorkers.UserEvents;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentEventEditorBinding;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class EventEditorFragment extends Fragment {

    private FragmentEventEditorBinding binding;
    private UserController userController;
    private int eventId = -1;
    
    private final Calendar calendarDate = Calendar.getInstance();
    private final Calendar calendarTime = Calendar.getInstance();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        userController = MySingleton.getInstance(context.getApplicationContext()).getUserController();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getInt("eventId", -1);
        }

        setupCircleModeSpinner();
        setupDatePicker();
        setupTimePicker();
        setupUiFromEvent();

        binding.btnSaveEvent.setOnClickListener(v -> saveEvent());
    }

    private void setupCircleModeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.spinner_event_circlemode_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerEventCirclemode.setAdapter(adapter);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendarDate.set(Calendar.YEAR, year);
            calendarDate.set(Calendar.MONTH, month);
            calendarDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateLabel();
        };

        binding.etEventDate.setOnClickListener(v -> {
            // Обновляем календарь до текущей даты, если поле пустое или не установлено
            String dateText = binding.etEventDate.getText().toString().trim();
            if (dateText.isEmpty()) {
                calendarDate.setTimeInMillis(System.currentTimeMillis());
            }
            new DatePickerDialog(requireContext(), dateSetListener,
                    calendarDate.get(Calendar.YEAR),
                    calendarDate.get(Calendar.MONTH),
                    calendarDate.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTimePicker() {
        TimePickerDialog.OnTimeSetListener timeSetListener = (view, hourOfDay, minute) -> {
            calendarTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendarTime.set(Calendar.MINUTE, minute);
            updateTimeLabel();
        };

        binding.etEventTime.setOnClickListener(v -> {
            // Обновляем календарь до текущего времени, если поле пустое или не установлено
            String timeText = binding.etEventTime.getText().toString().trim();
            if (timeText.isEmpty()) {
                calendarTime.setTimeInMillis(System.currentTimeMillis());
            }
            new TimePickerDialog(requireContext(), timeSetListener,
                    calendarTime.get(Calendar.HOUR_OF_DAY),
                    calendarTime.get(Calendar.MINUTE),
                    true).show();
        });
    }

    private void updateDateLabel() {
        String myFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etEventDate.setText(sdf.format(calendarDate.getTime()));
    }

    private void updateTimeLabel() {
        String myFormat = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etEventTime.setText(sdf.format(calendarTime.getTime()));
    }

    private void setupUiFromEvent() {
        if (eventId > 0) {
            UserEvents event = userController.getEventById(eventId);
            if (event != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                if (event.getDate() != null) {
                    LocalDate date = event.getDate();
                    binding.etEventDate.setText(date.format(formatter));
                    // Обновляем календарь для date picker
                    calendarDate.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
                }

                int time = event.getTime();
                int hours = time / 60;
                int minutes = time % 60;
                binding.etEventTime.setText(String.format("%02d:%02d", hours, minutes));
                // Обновляем календарь для time picker
                calendarTime.set(Calendar.HOUR_OF_DAY, hours);
                calendarTime.set(Calendar.MINUTE, minutes);
                
                binding.etEventName.setText(event.getName());
                binding.etEventInfo.setText(event.getInfo());
                binding.switchEventEnabled.setChecked(event.isEnable());

                binding.spinnerEventCirclemode.setSelection(event.getCircle_Mode());
                if (event.getCircle_Mode() == 3) {
                    binding.etEventCircleDays.setText(String.valueOf(event.getCircle_Days()));
                }
            }
        } else {
            // Для нового события инициализируем текущей датой и временем
            if (binding.etEventDate.getText().toString().trim().isEmpty()) {
                updateDateLabel();
            }
            if (binding.etEventTime.getText().toString().trim().isEmpty()) {
                updateTimeLabel();
            }
        }
    }

    private void saveEvent() {
        String dateStr = binding.etEventDate.getText().toString().trim();
        String timeStr = binding.etEventTime.getText().toString().trim();
        String name = binding.etEventName.getText().toString().trim();
        String info = binding.etEventInfo.getText().toString().trim();

        if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(timeStr) || TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Заполните дату, время и название", Toast.LENGTH_SHORT).show();
            return;
        }

        // Используем календарь для получения даты
        LocalDate date = LocalDate.of(
                calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH) + 1,
                calendarDate.get(Calendar.DAY_OF_MONTH)
        );

        // Используем календарь для получения времени
        int minutes = calendarTime.get(Calendar.HOUR_OF_DAY) * 60 + calendarTime.get(Calendar.MINUTE);

        int circleMode = binding.spinnerEventCirclemode.getSelectedItemPosition();
        int circleDays = 0;
        if (circleMode == 3) {
            String cdStr = binding.etEventCircleDays.getText().toString().trim();
            try {
                circleDays = Integer.parseInt(cdStr);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Введите количество дней повторения", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        boolean enabled = binding.switchEventEnabled.isChecked();

        UserEvents event = new UserEvents(date, minutes, circleMode, circleDays, name, info);
        if (eventId > 0) {
            event.setId(eventId);
        }
        event.setEnable(enabled);

        userController.saveOrUpdateEvent(event);
        Toast.makeText(getContext(), "Событие сохранено", Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


