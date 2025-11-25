package com.AS.Yuppi.Raspi.ui.home;

import com.AS.Yuppi.Raspi.DataWorkers.Day_Schedule;
import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.R;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AS.Yuppi.Raspi.databinding.FragmentHomeBinding;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeFragment extends Fragment {
    // Сделаем binding nullable, чтобы явно показать, что он может быть null
    @Nullable
    private FragmentHomeBinding binding;

    private SchedulelController schedulelController;
    private HomeScheduleAdapter scheduleAdapter;
    private int currentDayOffset = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        schedulelController = MySingleton.getInstance(context.getApplicationContext())
                .getSchedulelController();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализируем UI
        setupRecyclerView();
        setupClickListeners();

        // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
        // Используем LifecycleOwner для безопасной подписки на события.
        // Это гарантирует, что лямбда будет выполняться только когда View существует.
        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        schedulelController.addOnActionListener(data -> {
            // Проверяем, что View все еще активно (состояние RESUMED, STARTED и т.д.)
            if (viewLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                // Теперь вызов updateScheduleData абсолютно безопасен
                updateScheduleData();
            }
        });

        // Первоначальное обновление данных
        updateScheduleData();
    }

    private void setupRecyclerView() {
        if (binding == null) return;
        RecyclerView scheduleRecyclerView = binding.scheduleList;
        scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        scheduleRecyclerView.setNestedScrollingEnabled(false);
        scheduleAdapter = new HomeScheduleAdapter(new ArrayList<>());
        scheduleRecyclerView.setAdapter(scheduleAdapter);
    }

    private void setupClickListeners() {
        if (binding == null) return;
        binding.btnPrevDay.setOnClickListener(v -> {
            currentDayOffset--;
            updateScheduleData();
        });

        binding.btnNextDay.setOnClickListener(v -> {
            currentDayOffset++;
            updateScheduleData();
        });

        binding.btnAddHometask.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_nav_home_to_addHometaskFragment));

        binding.btnAddLectureNote.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_nav_home_to_addLectureFragment));
    }

    private void updateScheduleData() {
        // Проверка на существование View
        if (binding == null || scheduleAdapter == null) {
            return;
        }

        updateDateHeader();

        Schedules currentSchedule = schedulelController.getCurrentSchedule();
        if (currentSchedule == null) {
            Log.w("HomeFragment", "Текущее расписание не загружено.");
            scheduleAdapter.updateLessons(new ArrayList<>()); // Очищаем список
            return;
        }

        // Получаем расписание на выбранный день
        Day_Schedule daySchedule = currentSchedule.getScheduleForDayOffset(currentDayOffset);
        if (daySchedule == null) {
            Log.w("HomeFragment", "Расписание на день со смещением " + currentDayOffset + " не найдено.");
            scheduleAdapter.updateLessons(new ArrayList<>()); // Очищаем список
            return;
        }

        // ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД-ПАРСЕР
        List<ScheduleLesson> lessons = parseDaySchedule(daySchedule, currentSchedule);

        // Обновляем RecyclerView
        scheduleAdapter.updateLessons(lessons);
    }

    private void updateDateHeader() {
        if (binding == null) return;
        LocalDate targetDate = LocalDate.now().plusDays(currentDayOffset);
        String formattedDate = targetDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        binding.tvCurrentDate.setText(formattedDate);
    }
    private List<ScheduleLesson> parseDaySchedule(Day_Schedule daySchedule, Schedules mainSchedule) {
        List<ScheduleLesson> lessonList = new ArrayList<>();
        String lessonsRaw = daySchedule.get_lesson();

        // Если строка пуста или состоит из пробелов/запятых, возвращаем пустой список
        if (lessonsRaw == null || lessonsRaw.replaceAll("[,\\s]", "").isEmpty()) {
            return lessonList;
        }

        // 1. Разбиваем всю строку на отдельные "блоки" занятий.
        // Разделителем является номер урока в формате "цифра)"
        String[] lessonBlocks = lessonsRaw.split("\\s*(?=\\d+\\))");

        for (String block : lessonBlocks) {
            String trimmedBlock = block.trim();
            if (trimmedBlock.isEmpty()) {
                continue;
            }

            try {
                // 2. Извлекаем номер урока
                String lessonNumber = trimmedBlock.substring(0, trimmedBlock.indexOf(')'));

                // 3. Извлекаем тип занятия (лек, лаб, сем) и имя преподавателя
                String subjectName = "";
                String teacherName = "";
                String classroom = "";
                ScheduleLesson.LessonType type = ScheduleLesson.LessonType.LECTURE; // Тип по умолчанию

                // Ищем преподавателя (Ф.И.О. или Ф. И.)
                Pattern teacherPattern = Pattern.compile("([А-ЯЁ][а-яё]+(?:\\s[А-ЯЁ]\\.){1,2})");
                Matcher teacherMatcher = teacherPattern.matcher(trimmedBlock);
                if (teacherMatcher.find()) {
                    teacherName = teacherMatcher.group(1);
                }

                // Ищем аудиторию (буква и 3 цифры)
                Pattern classroomPattern = Pattern.compile("([А-Я]\\d{3})");
                Matcher classroomMatcher = classroomPattern.matcher(trimmedBlock);
                if (classroomMatcher.find()) {
                    classroom = classroomMatcher.group(1).trim();
                }

                // Определяем тип и очищаем название предмета
                String tempSubject = trimmedBlock.substring(trimmedBlock.indexOf(')') + 1).trim();
                if (tempSubject.contains("лек.")) {
                    type = ScheduleLesson.LessonType.LECTURE;
                    subjectName = tempSubject.split("лек.")[0].trim();
                } else if (tempSubject.contains("лаб.")) {
                    type = ScheduleLesson.LessonType.LAB;
                    subjectName = tempSubject.split("лаб.")[0].trim();
                } else if (tempSubject.contains("сем.")) {
                    type = ScheduleLesson.LessonType.SEMINAR;
                    subjectName = tempSubject.split("сем.")[0].trim();
                } else {
                    // Если тип не указан, берем все до преподавателя или аудитории
                    int endOfSubject = tempSubject.length();
                    if (!teacherName.isEmpty()) {
                        endOfSubject = Math.min(endOfSubject, tempSubject.indexOf(teacherName));
                    }
                    if (!classroom.isEmpty()) {
                        endOfSubject = Math.min(endOfSubject, tempSubject.indexOf(classroom));
                    }
                    subjectName = tempSubject.substring(0, endOfSubject).trim();
                }

                // Убираем лишние переносы строк
                subjectName = subjectName.replace("\n", " ").trim();


                // 4. Получаем время
                int lessonIndex = Integer.parseInt(lessonNumber) - 1;
                Integer startTime = daySchedule.get_Lesson_StartTime(lessonIndex);
                Integer endTime = daySchedule.get_Lesson_EndTime(lessonIndex);
                String timeString = minutesToTime(startTime) + "\n" + minutesToTime(endTime);

                // 5. Добавляем готовый урок в список
                lessonList.add(new ScheduleLesson(
                        type,
                        lessonNumber,
                        timeString,
                        subjectName,
                        teacherName, // Теперь здесь реальный преподаватель
                        classroom      // И реальная аудитория
                ));

            } catch (Exception e) {
                Log.e("ParseError", "Не удалось распарсить блок занятия: '" + trimmedBlock + "'", e);
            }
        }
        return lessonList;
    }
    private String minutesToTime(int totalMinutes) {
        if (totalMinutes < 0) return "00:00";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // При уничтожении View, обнуляем binding, чтобы избежать утечек памяти.
        binding = null;
    }
}
