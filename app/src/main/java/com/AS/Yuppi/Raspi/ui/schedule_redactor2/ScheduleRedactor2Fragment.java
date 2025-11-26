package com.AS.Yuppi.Raspi.ui.schedule_redactor2;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.AS.Yuppi.Raspi.DataWorkers.FileUtil;
import com.AS.Yuppi.Raspi.DataWorkers.JsonConverter;
import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentScheduleRedactor2Binding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.AS.Yuppi.Raspi.DataWorkers.NetworkClient;
import com.AS.Yuppi.Raspi.DataWorkers.ApiService;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiSchedule;
import com.AS.Yuppi.Raspi.DataWorkers.DTOs.ApiDaySchedule;
import com.AS.Yuppi.Raspi.DataWorkers.Day_Schedule; // он уже должен быть
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ScheduleRedactor2Fragment extends Fragment {

    private FragmentScheduleRedactor2Binding binding;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SchedulelController schedulelController;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private int weekOnScreen = 0;

    private List<EditText> listDaysViews;
    private List<EditText> listTimesStartViews;
    private List<EditText> listTimesEndViews;
    
    private final Calendar calendarStartDate = Calendar.getInstance();
    private final Calendar calendarEndDate = Calendar.getInstance();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        schedulelController = MySingleton.getInstance(context.getApplicationContext()).getSchedulelController();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        new ViewModelProvider(this).get(ScheduleRedactor2ViewModel.class);
        binding = FragmentScheduleRedactor2Binding.inflate(inflater, container, false);
        initializeFilePicker();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViewLists();

        binding.layoutDaychanger.setVisibility(View.VISIBLE);
        binding.layoutTimechanger.setVisibility(View.GONE);

        // В setupClickListeners() в ScheduleRedactor2Fragment.java

        // Например, по обычному клику загружаем с сервера
        binding.buttonServerSaveLoad.setOnClickListener(v -> fetchSchedulesFromServer());

        // А по долгому нажатию - отправляем на сервер
        binding.buttonServerSaveLoad.setOnLongClickListener(v -> {
            updateEditableInfo(); // Сначала обновляем данные
            postCurrentScheduleToServer();
            return true; // Возвращаем true, чтобы onClick не сработал
        });


        setupPageSelectSpinner();
        setupCircleModeSpinner();
        setupTimeDaySelectSpinner();
        setupDatePickers();
        setupClickListeners();

        // Загружаем данные в поля при первом создании View
        updateScreen(true);
    }

    private void initializeViewLists() {
        listDaysViews = Arrays.asList(
                binding.editTextDay1, binding.editTextDay2, binding.editTextDay3,
                binding.editTextDay4, binding.editTextDay5, binding.editTextDay6, binding.editTextDay7
        );
        listTimesStartViews = Arrays.asList(
                binding.editTextTimeStart1, binding.editTextTimeStart2, binding.editTextTimeStart3,
                binding.editTextTimeStart4, binding.editTextTimeStart5, binding.editTextTimeStart6,
                binding.editTextTimeStart7, binding.editTextTimeStart8, binding.editTextTimeStart9,
                binding.editTextTimeStart10
        );
        listTimesEndViews = Arrays.asList(
                binding.editTextTimeEnd1, binding.editTextTimeEnd2, binding.editTextTimeEnd3,
                binding.editTextTimeEnd4, binding.editTextTimeEnd5, binding.editTextTimeEnd6,
                binding.editTextTimeEnd7, binding.editTextTimeEnd8, binding.editTextTimeEnd9,
                binding.editTextTimeEnd10
        );
    }

    private void setupPageSelectSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.spinner_pageselect_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPageselect.setAdapter(adapter);
        binding.spinnerPageselect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                binding.layoutDaychanger.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                binding.layoutTimechanger.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupCircleModeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.spinner_circlemode_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCirclemode.setAdapter(adapter);
    }

    private void setupTimeDaySelectSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.spinner_time_dayselect_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeDayselect.setAdapter(adapter);
        binding.spinnerTimeDayselect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                changedTimeList(position + weekOnScreen * 7);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePickers() {
        // Слушатель для даты начала
        DatePickerDialog.OnDateSetListener dateStartListener = (view, year, month, dayOfMonth) -> {
            calendarStartDate.set(Calendar.YEAR, year);
            calendarStartDate.set(Calendar.MONTH, month);
            calendarStartDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateStartLabel();
        };

        // Слушатель для даты окончания
        DatePickerDialog.OnDateSetListener dateEndListener = (view, year, month, dayOfMonth) -> {
            calendarEndDate.set(Calendar.YEAR, year);
            calendarEndDate.set(Calendar.MONTH, month);
            calendarEndDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateEndLabel();
        };

        // Устанавливаем обработчик клика на поле даты начала
        binding.editTextDateStartdate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), dateStartListener,
                    calendarStartDate.get(Calendar.YEAR),
                    calendarStartDate.get(Calendar.MONTH),
                    calendarStartDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Устанавливаем обработчик клика на поле даты окончания
        binding.editTextDateEnddate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), dateEndListener,
                    calendarEndDate.get(Calendar.YEAR),
                    calendarEndDate.get(Calendar.MONTH),
                    calendarEndDate.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateDateStartLabel() {
        String myFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.editTextDateStartdate.setText(sdf.format(calendarStartDate.getTime()));
    }

    private void updateDateEndLabel() {
        String myFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.editTextDateEnddate.setText(sdf.format(calendarEndDate.getTime()));
    }

    private void setupClickListeners() {
        binding.buttonFileloadFilechecker.setOnClickListener(v -> launchFilePicker(false, "*/*"));
        binding.buttonFileloadFilechecker.setOnLongClickListener(v -> {
            saveScheduleToDownloads();
            return true;
        });
        binding.buttonTimeSaveday.setOnClickListener(v -> saveTimeDay());
        binding.buttonTimeSaveday.setOnLongClickListener(v -> {
            saveTimeAllDays();
            return true;
        });
        binding.buttonTimeClear.setOnClickListener(v -> clearTimes());
        binding.buttonTimeClear.setOnLongClickListener(v -> {
            writeTimesFromSaved(-1);
            return true;
        });
        binding.buttonChangeweek.setOnClickListener(v -> changeWeek());
        binding.buttonFirstweekchange.setOnClickListener(v -> changeFirstWeekID());
        binding.buttonSaveLoad.setOnClickListener(v -> {
            updateEditableInfo();
            Schedules schedule = schedulelController.geteditableSchedule();
            if (schedule == null || schedule.getName() == null || schedule.getName().trim().isEmpty()) {
                Toast.makeText(getContext(), "Введите имя расписания", Toast.LENGTH_SHORT).show();
                return;
            }
            // Автор всегда берется из MySingleton/DeviceUtils
            schedule.setAuthor(MySingleton.getHashedDeviceId());
            schedulelController.saveEditableSchedule();
            Toast.makeText(getContext(), "Расписание сохранено локально", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateEditableInfo() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) {
            // Если расписания нет, создаем новое, чтобы избежать NPE
            schedulelController.createCurrentSchedule();
            currentSchedule = schedulelController.geteditableSchedule();
        }

        int offset = weekOnScreen * 7;
        for (int i = 0; i < 7; i++) {
            currentSchedule.setDayLesson(listDaysViews.get(i).getText().toString(), i + offset);
        }

        currentSchedule.setName(binding.autoCompleteTextViewAuthor.getText().toString());
        currentSchedule.setAuthor(MySingleton.getHashedDeviceId());

        LocalDate startDate = getLocalDateFromString(binding.editTextDateStartdate.getText().toString());
        LocalDate endDate = getLocalDateFromString(binding.editTextDateEnddate.getText().toString());

        if (startDate != null) currentSchedule.setStart_Date(startDate);
        if (endDate != null) currentSchedule.setEnd_Date(endDate);
    }

    private void updateScreen(boolean isClearUpdate) {
        if (binding == null || getView() == null) return;

        View focusedView = getView().findFocus();
        if (focusedView != null) focusedView.clearFocus();

        if (isClearUpdate) {
            loadDataToScreen();
        } else {
            showDays();
        }
    }

    private void loadDataToScreen() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) return;

        // Устанавливаем режим цикла в спиннере
        binding.spinnerCirclemode.setSelection(currentSchedule.getCircle_Mode());

        int weekIp = currentSchedule.getFirstWeekId();
        int offset = (currentSchedule.getCircle_Mode() == 1 && weekOnScreen == 1) ? 7 : 0;
        for (int i = 0; i < 7; i++) {
            listDaysViews.get(i).setText(currentSchedule.getDayLesson(i + offset));
        }

        try {
            String tb2Text = getString(R.string.first_week_is_even_template);
            binding.buttonFirstweekchange.setText(String.format(tb2Text, (weekIp == 0 ? "четная" : "нечетная")));
        } catch (Exception e) {
            binding.buttonFirstweekchange.setText("1 неделя " + (weekIp == 0 ? "четная" : "нечетная"));
        }


        binding.autoCompleteTextViewAuthor.setText(currentSchedule.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        if(currentSchedule.getStart_Date() != null) {
            LocalDate startDate = currentSchedule.getStart_Date();
            binding.editTextDateStartdate.setText(startDate.format(formatter));
            // Обновляем календарь для date picker
            calendarStartDate.set(startDate.getYear(), startDate.getMonthValue() - 1, startDate.getDayOfMonth());
        } else {
            // Устанавливаем текущую дату по умолчанию, если поле пустое
            if (binding.editTextDateStartdate.getText().toString().trim().isEmpty()) {
                updateDateStartLabel();
            }
        }
        if(currentSchedule.getEnd_Date() != null) {
            LocalDate endDate = currentSchedule.getEnd_Date();
            binding.editTextDateEnddate.setText(endDate.format(formatter));
            // Обновляем календарь для date picker
            calendarEndDate.set(endDate.getYear(), endDate.getMonthValue() - 1, endDate.getDayOfMonth());
        } else {
            // Устанавливаем текущую дату по умолчанию, если поле пустое
            if (binding.editTextDateEnddate.getText().toString().trim().isEmpty()) {
                updateDateEndLabel();
            }
        }

        // Обновляем список времени
        changedTimeList(-1);
    }

    private void showDays() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null || getActivity() == null || binding == null) return;

        getActivity().runOnUiThread(() -> {
            int offset = weekOnScreen * 7;
            for (int i = 0; i < 7; i++) {
                if (i + offset < currentSchedule.countDays_Schedule()) {
                    listDaysViews.get(i).setText(currentSchedule.getDayLesson(i + offset));
                }
            }
        });
    }

    private void changeWeek() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) return;

        // Сохраняем данные с текущей недели перед переключением
        updateEditableInfo();

        weekOnScreen = (weekOnScreen == 0) ? 1 : 0;

        try {
            binding.buttonChangeweek.setText(getString(R.string.change_week_template, weekOnScreen + 1));
        } catch (Exception e) {
            binding.buttonChangeweek.setText("Неделя " + (weekOnScreen + 1));
        }

        updateScreen(false); // Обновляем экран новыми данными
    }

    private void saveTimeDay() {
        int day = binding.spinnerTimeDayselect.getSelectedItemPosition() + weekOnScreen * 7;
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) return;
        for (int i = 0; i < 10; i++) {
            int startTime = parseNumberOrTime(listTimesStartViews.get(i).getText().toString());
            int endTime = parseNumberOrTime(listTimesEndViews.get(i).getText().toString());
            currentSchedule.setLesson_StartTime(day, i, startTime);
            currentSchedule.setLesson_EndTime(day, i, endTime);
        }
        changedTimeList(-1);
    }

    // ИСПРАВЛЕНО: Завершена логика метода
    private void saveTimeAllDays() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) return;

        List<Integer> startTimes = new ArrayList<>();
        List<Integer> endTimes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            startTimes.add(parseNumberOrTime(listTimesStartViews.get(i).getText().toString()));
            endTimes.add(parseNumberOrTime(listTimesEndViews.get(i).getText().toString()));
        }

        // ДОБАВЛЕН НЕДОСТАЮЩИЙ ЦИКЛ
        for (int dayIndex = 0; dayIndex < currentSchedule.countDays_Schedule(); dayIndex++) {
            for (int lessonIndex = 0; lessonIndex < 10; lessonIndex++) {
                currentSchedule.setLesson_StartTime(dayIndex, lessonIndex, startTimes.get(lessonIndex));
                currentSchedule.setLesson_EndTime(dayIndex, lessonIndex, endTimes.get(lessonIndex));
            }
        }
        changedTimeList(-1); // Обновляем отображение времени
        Toast.makeText(getContext(), "Время сохранено для всех дней", Toast.LENGTH_SHORT).show();
    }

    private void clearTimes() {
        for (int i = 0; i < 10; i++) {
            listTimesStartViews.get(i).setText("");
            listTimesEndViews.get(i).setText("");
        }
    }

    private void writeTimesFromSaved(int day) {
        if (day == -1) day = binding.spinnerTimeDayselect.getSelectedItemPosition() + weekOnScreen * 7;
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null || day >= currentSchedule.countDays_Schedule()) return;

        for (int i = 0; i < listTimesStartViews.size(); i++) {
            listTimesStartViews.get(i).setText(minutesToTime(currentSchedule.getLessons_StartTime(day, i)));
            listTimesEndViews.get(i).setText(minutesToTime(currentSchedule.getLessons_EndTime(day, i)));
        }
    }

    private void changedTimeList(int day) {
        if (binding == null) return;
        if (day == -1) day = binding.spinnerTimeDayselect.getSelectedItemPosition() + weekOnScreen * 7;

        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null || day >= currentSchedule.countDays_Schedule()) {
            binding.TextViewTimelist.setText("");
            return;
        }

        StringBuilder output = new StringBuilder();
        int lessonsCount = currentSchedule.countLessons_StartTime();
        for (int i = 0; i < lessonsCount; i++) {
            output.append(i + 1)
                    .append(") ")
                    .append(minutesToTime(currentSchedule.getLessons_StartTime(day, i)))
                    .append("-")
                    .append(minutesToTime(currentSchedule.getLessons_EndTime(day, i)))
                    .append('\n');
        }
        binding.TextViewTimelist.setText(output.toString());
    }

    private void changeFirstWeekID() {
        Schedules currentSchedule = schedulelController.geteditableSchedule();
        if (currentSchedule == null) return;

        int newWeekId = (currentSchedule.getFirstWeekId() == 0) ? 1 : 0;
        currentSchedule.setFirstWeekId(newWeekId);
        updateScreen(true); // Перерисовываем экран с учетом нового id
    }

    private void processFileRaspi(String content) {
        Schedules loadedSchedule = JsonConverter.fromJson_Uni(content);
        if (loadedSchedule != null) {
            schedulelController.seteditableSchedule(loadedSchedule);
            updateScreen(true);
            Toast.makeText(getContext(), "Расписание из файла загружено", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Ошибка чтения файла расписания", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveScheduleToDownloads() {
        updateEditableInfo(); // Сначала обновляем данные
        Schedules scheduleToSave = schedulelController.geteditableSchedule();

        if (scheduleToSave == null || scheduleToSave.getName() == null || scheduleToSave.getName().isEmpty()) {
            Toast.makeText(getContext(), "Ошибка: введите имя расписания.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String jsonString = JsonConverter.toJson(scheduleToSave);
        final String fileName = scheduleToSave.getName() + ".rasp";

        ioExecutor.execute(() -> {
            try {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File targetDir = new File(downloadsDir, "Raspi");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                File targetFile = new File(targetDir, fileName);

                try (FileOutputStream fos = new FileOutputStream(targetFile);
                     OutputStreamWriter writer = new OutputStreamWriter(fos)) {
                    writer.write(jsonString);
                }

                MediaScannerConnection.scanFile(getContext(), new String[]{targetFile.getAbsolutePath()}, null, null);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Сохранено: " + targetFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                Log.e("ScheduleExport", "Ошибка записи в файл", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Ошибка сохранения файла", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void initializeFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                String content = FileUtil.readTextFromUri(getActivity(), uri);
                                processFileRaspi(content);
                            } catch (IOException e) {
                                Toast.makeText(getContext(), "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void launchFilePicker(boolean allowMultiple, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        filePickerLauncher.launch(intent);
    }

    public static int parseNumberOrTime(String input) {
        if (input == null || input.trim().isEmpty()) return 0;
        String cleaned = input.replaceAll("[^0-9:]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            if (cleaned.contains(":")) {
                String[] parts = cleaned.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return Math.max(0, Math.min(23, hours)) * 60 + Math.max(0, Math.min(59, minutes));
            } else {
                return Integer.parseInt(cleaned);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static String minutesToTime(int totalMinutes) {
        if (totalMinutes < 0) return "00:00";
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private LocalDate getLocalDateFromString(String dateString) {
        if (dateString == null || dateString.isEmpty()) return LocalDate.now();
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    /**
     * Загружает все расписания с сервера.
     * Пока просто выводит имена в лог.
     */
    private void fetchSchedulesFromServer() {
        ApiService apiService = NetworkClient.getApiService();
        Call<List<ApiSchedule>> call = apiService.getSchedules();

        call.enqueue(new Callback<List<ApiSchedule>>() {
            @Override
            public void onResponse(Call<List<ApiSchedule>> call, Response<List<ApiSchedule>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ApiSchedule> schedules = response.body();
                    Log.d("NetworkRequest", "Успешно загружено " + schedules.size() + " расписаний.");
                    for (ApiSchedule schedule : schedules) {
                        Log.d("NetworkRequest", "Имя расписания: " + schedule.getName());
                    }
                    Toast.makeText(getContext(), "Расписания загружены", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("NetworkRequest", "Ошибка загрузки: " + response.code());
                    Toast.makeText(getContext(), "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ApiSchedule>> call, Throwable t) {
                Log.e("NetworkRequest", "Сетевая ошибка: " + t.getMessage());
                Toast.makeText(getContext(), "Сетевая ошибка", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Отправляет текущее редактируемое расписание на сервер.
     */
    private void postCurrentScheduleToServer() {
        Schedules localSchedule = schedulelController.geteditableSchedule();
        if (localSchedule == null) {
            Toast.makeText(getContext(), "Нет расписания для отправки", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Конвертируем ваше локальное расписание в формат для API
        List<ApiDaySchedule> apiDays = new ArrayList<>();
        List<Day_Schedule> localDays = localSchedule.getDays_Schedule();

        for (int i = 0; i < localDays.size(); i++) {
            Day_Schedule localDay = localDays.get(i);
            ApiDaySchedule apiDay = new ApiDaySchedule();
            apiDay.setId(i); // id можно ставить по порядку
            apiDay.setLessons(localDay.get_lesson());
            apiDay.setLessonsStartTime(localDay.get_Lessons_StartTime()); // Используем геттер из Day_Schedule
            apiDay.setLessonsEndTime(localDay.get_Lessons_EndTime());     // Используем геттер из Day_Schedule
            apiDays.add(apiDay);
        }

        ApiSchedule scheduleToSend = new ApiSchedule(localSchedule.getName(), apiDays);

        // 2. Отправляем запрос
        ApiService apiService = NetworkClient.getApiService();
        Call<Void> call = apiService.postSchedule(scheduleToSend);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("NetworkRequest", "Расписание успешно отправлено.");
                    Toast.makeText(getContext(), "Расписание отправлено на сервер!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("NetworkRequest", "Ошибка отправки: " + response.code());
                    Toast.makeText(getContext(), "Ошибка отправки: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("NetworkRequest", "Сетевая ошибка при отправке: " + t.getMessage());
                Toast.makeText(getContext(), "Сетевая ошибка при отправке", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
