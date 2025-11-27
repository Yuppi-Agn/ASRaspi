package com.AS.Yuppi.Raspi.ui.home;

import com.AS.Yuppi.Raspi.DataWorkers.Day_Schedule;
import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.DataWorkers.UserController;
import com.AS.Yuppi.Raspi.DataWorkers.UserEvents;
import com.AS.Yuppi.Raspi.DataWorkers.UserTask;
import com.AS.Yuppi.Raspi.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.time.LocalDateTime;
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
    private UserController userController;
    private HomeScheduleAdapter scheduleAdapter;
    private WeekDayAdapter weekDayAdapter;
    private int currentDayOffset = 0;
    private boolean isWeekViewMode = false;
    private int currentWeekOffset = 0;
    private Handler countdownHandler;
    private Runnable countdownRunnable;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MySingleton singleton = MySingleton.getInstance(context.getApplicationContext());
        schedulelController = singleton.getSchedulelController();
        userController = singleton.getUserController();
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
        setupCountdownTimer();

        // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
        // Используем LifecycleOwner для безопасной подписки на события.
        // Это гарантирует, что лямбда будет выполняться только когда View существует.
        final LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        schedulelController.addOnActionListener(data -> {
            // Проверяем, что View все еще активно (состояние RESUMED, STARTED и т.д.)
            if (viewLifecycleOwner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                // Теперь вызов updateScheduleData абсолютно безопасен
                if ("HometaskUpdated".equals(data) || "ScheduleSaved".equals(data) || "CurrentScheduleLoaded".equals(data) || 
                    "SelectedSchedulesUpdated".equals(data) || "UserTaskUpdated".equals(data)) {
                    updateScheduleData();
                }
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
        weekDayAdapter = new WeekDayAdapter(new ArrayList<>());
        scheduleRecyclerView.setAdapter(scheduleAdapter);
    }

    private void setupClickListeners() {
        if (binding == null) return;
        
        // Day/Week navigation buttons
        binding.btnPrevDay.setOnClickListener(v -> {
            if (isWeekViewMode) {
                currentWeekOffset--;
                updateScheduleData();
            } else {
                currentDayOffset--;
                updateScheduleData();
            }
        });

        binding.btnNextDay.setOnClickListener(v -> {
            if (isWeekViewMode) {
                currentWeekOffset++;
                updateScheduleData();
            } else {
                currentDayOffset++;
                updateScheduleData();
            }
        });

        // Action buttons
        binding.btnAddHometask.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("mode", "study");
            args.putInt("taskId", -1);
            NavHostFragment.findNavController(HomeFragment.this)
                    .navigate(R.id.action_nav_home_to_addHometaskFragment, args);
        });

        binding.btnAddLectureNote.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_nav_home_to_addLectureFragment));

        // Add event button
        if (binding.btnAddEvent != null) {
            binding.btnAddEvent.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putInt("eventId", -1);
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.action_nav_home_to_event_editor, args);
            });
        }

        // Show week button - toggle between day and week view
        if (binding.btnShowWeek != null) {
            binding.btnShowWeek.setOnClickListener(v -> {
                isWeekViewMode = !isWeekViewMode;
                RecyclerView scheduleRecyclerView = binding.scheduleList;
                
                if (isWeekViewMode) {
                    // Switch to week view - reset to current week
                    currentWeekOffset = 0;
                    binding.btnShowWeek.setText("Показать на день");
                    // Change RecyclerView to horizontal scrolling with week adapter
                    scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                    scheduleRecyclerView.setAdapter(weekDayAdapter);
                } else {
                    // Switch to day view - reset to today
                    currentDayOffset = 0;
                    binding.btnShowWeek.setText("Показать на неделю");
                    // Change RecyclerView back to vertical scrolling with day adapter
                    scheduleRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    scheduleRecyclerView.setAdapter(scheduleAdapter);
                }
                updateScheduleData();
            });
        }
    }

    private void updateScheduleData() {
        // Проверка на существование View
        if (binding == null || scheduleAdapter == null) {
            return;
        }

        updateDateHeader();
        updateTodayHeader();

        // Получаем все выбранные расписания
        List<Schedules> selectedSchedules = schedulelController.getSelectedSchedulesObjects();

        if (selectedSchedules.isEmpty()) {
            Log.w("HomeFragment", "Нет выбранных расписаний.");
            scheduleAdapter.updateLessons(new ArrayList<>());
            if (weekDayAdapter != null && binding != null) {
                weekDayAdapter.updateWeekDays(new ArrayList<>());
            }
            return;
        }

        if (isWeekViewMode) {
            // Week view: show horizontal scrolling week schedule
            setupWeekView(selectedSchedules);
        } else {
            // Day view: объединяем данные из всех выбранных расписаний
            List<ScheduleLesson> allLessons = new ArrayList<>();
            
            // Объединяем уроки из всех выбранных расписаний
            for (Schedules schedule : selectedSchedules) {
                Day_Schedule daySchedule = schedule.getScheduleForDayOffset(currentDayOffset);
                if (daySchedule != null) {
                    List<ScheduleLesson> lessons = parseDaySchedule(daySchedule, schedule);
                    allLessons.addAll(lessons);
                }
            }
            
            // Добавляем личные мероприятия (UserEvents)
            List<UserEvents> personalEvents = userController.getEventsForDayOffset(currentDayOffset);
            for (UserEvents event : personalEvents) {
                if (event.isEnable()) {
                    String timeStr = String.format("%02d:%02d", event.getTime() / 60, event.getTime() % 60);
                    ScheduleLesson personalLesson = new ScheduleLesson(timeStr, event.getName(), event.getInfo());
                    allLessons.add(personalLesson);
                }
            }
            
            // Добавляем задания к соответствующим предметам
            LocalDate targetDate = LocalDate.now().plusDays(currentDayOffset);
            for (Schedules schedule : selectedSchedules) {
                List<Schedules.Hometask> hometasks = schedule.getHometasks();
                if (hometasks == null) continue;
                for (Schedules.Hometask hometask : hometasks) {
                    if (hometask == null) continue;
                    if (hometask.getEndpoint() != null && hometask.getEndpoint().equals(targetDate) && !hometask.isPersonal()) {
                        // Находим соответствующий урок по предмету
                        String subject = hometask.getLesson();
                        if (subject == null || subject.isEmpty()) continue;
                        boolean found = false;
                        for (ScheduleLesson lesson : allLessons) {
                            if (!lesson.isHeader && lesson.subjectName != null) {
                                // Сравниваем названия предметов (учитываем возможные различия в пробелах и регистре)
                                String lessonSubject = lesson.subjectName.trim();
                                String hometaskSubject = subject.trim();
                                if (lessonSubject.equalsIgnoreCase(hometaskSubject) || 
                                    lessonSubject.contains(hometaskSubject) || 
                                    hometaskSubject.contains(lessonSubject)) {
                                    ScheduleLesson.HometaskInfo taskInfo = new ScheduleLesson.HometaskInfo(
                                        hometask.getTask(), hometask.isDone(), subject);
                                    if (lesson.hometasks == null) {
                                        lesson.hometasks = new ArrayList<>();
                                    }
                                    lesson.hometasks.add(taskInfo);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        // Если урок не найден, создаем отдельный элемент для задания
                        if (!found) {
                            ScheduleLesson taskLesson = new ScheduleLesson(
                                ScheduleLesson.LessonType.LECTURE, "", "", subject, "", "");
                            ScheduleLesson.HometaskInfo taskInfo = new ScheduleLesson.HometaskInfo(
                                hometask.getTask(), hometask.isDone(), subject);
                            taskLesson.hometasks = new ArrayList<>();
                            taskLesson.hometasks.add(taskInfo);
                            allLessons.add(taskLesson);
                        }
                    }
                }
            }
            
            // Добавляем личные задания (UserTask) как отдельные элементы
            List<UserTask> personalTasks = userController.getTasksForDayOffset(currentDayOffset);
            for (UserTask task : personalTasks) {
                if (task.getEndpoint() != null && task.getEndpoint().equals(targetDate)) {
                    // Личные задания отображаем как "ДЗ" с фиолетовым цветом
                    ScheduleLesson personalTaskLesson = new ScheduleLesson(
                        ScheduleLesson.LessonType.TASK, "", "", task.getName(), "", "");
                    // Добавляем задание с информацией о выполнении
                    ScheduleLesson.HometaskInfo taskInfo = new ScheduleLesson.HometaskInfo(
                        task.getTask() != null ? task.getTask() : "", task.isDone(), task.getName());
                    personalTaskLesson.hometasks = new ArrayList<>();
                    personalTaskLesson.hometasks.add(taskInfo);
                    allLessons.add(personalTaskLesson);
                }
            }
            
            // Сортируем по времени начала
            allLessons.sort((l1, l2) -> {
                if (l1.isHeader || l2.isHeader) return 0;
                // Извлекаем время начала из строки времени
                try {
                    String time1 = l1.time.split("\n")[0];
                    String time2 = l2.time.split("\n")[0];
                    String[] parts1 = time1.split(":");
                    String[] parts2 = time2.split(":");
                    int minutes1 = Integer.parseInt(parts1[0]) * 60 + Integer.parseInt(parts1[1]);
                    int minutes2 = Integer.parseInt(parts2[0]) * 60 + Integer.parseInt(parts2[1]);
                    return Integer.compare(minutes1, minutes2);
                } catch (Exception e) {
                    return 0;
                }
            });

            // Обновляем RecyclerView
            scheduleAdapter.updateLessons(allLessons);
        }
    }

    private void updateTodayHeader() {
        if (binding == null) return;
        
        if (isWeekViewMode) {
            // In week view, don't show the "Сегодня" header and "Следующее занятие" block
            binding.tvTodayHeader.setVisibility(View.GONE);
            if (binding.nextClassContainer != null) {
                binding.nextClassContainer.setVisibility(View.GONE);
            }
        } else {
            // In day view, show relative date and "Следующее занятие" block
            binding.tvTodayHeader.setVisibility(View.VISIBLE);
            if (binding.nextClassContainer != null) {
                binding.nextClassContainer.setVisibility(View.VISIBLE);
            }
            LocalDate targetDate = LocalDate.now().plusDays(currentDayOffset);
            String relativeDate = getRelativeDateString(targetDate);
            binding.tvTodayHeader.setText(relativeDate + ":");
        }
    }

    private void setupWeekView(List<Schedules> selectedSchedules) {
        // Week view: create a list of day cards
        List<WeekDayAdapter.WeekDayData> weekDays = new ArrayList<>();
        LocalDate weekStart = LocalDate.now().plusWeeks(currentWeekOffset).with(java.time.DayOfWeek.MONDAY);
        
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = weekStart.plusDays(i);
            int dayOffset = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dayDate);
            
            // Объединяем уроки из всех выбранных расписаний
            List<ScheduleLesson> dayLessons = new ArrayList<>();
            for (Schedules schedule : selectedSchedules) {
                Day_Schedule daySchedule = schedule.getScheduleForDayOffset(dayOffset);
                if (daySchedule != null) {
                    List<ScheduleLesson> lessons = parseDaySchedule(daySchedule, schedule);
                    dayLessons.addAll(lessons);
                }
            }
            
            // Добавляем личные мероприятия
            List<UserEvents> personalEvents = userController.getEventsForDayOffset(dayOffset);
            for (UserEvents event : personalEvents) {
                if (event.isEnable()) {
                    String timeStr = String.format("%02d:%02d", event.getTime() / 60, event.getTime() % 60);
                    ScheduleLesson personalLesson = new ScheduleLesson(timeStr, event.getName(), event.getInfo());
                    dayLessons.add(personalLesson);
                }
            }
            
            // Добавляем задания к соответствующим предметам
            for (Schedules schedule : selectedSchedules) {
                if (schedule == null) continue;
                List<Schedules.Hometask> hometasks = schedule.getHometasks();
                if (hometasks == null) continue;
                for (Schedules.Hometask hometask : hometasks) {
                    if (hometask == null) continue;
                    if (hometask.getEndpoint() != null && hometask.getEndpoint().equals(dayDate) && !hometask.isPersonal()) {
                        String subject = hometask.getLesson();
                        if (subject == null || subject.isEmpty()) continue;
                        boolean found = false;
                        for (ScheduleLesson lesson : dayLessons) {
                            if (!lesson.isHeader && lesson.subjectName != null) {
                                // Сравниваем названия предметов (учитываем возможные различия в пробелах и регистре)
                                String lessonSubject = lesson.subjectName.trim();
                                String hometaskSubject = subject.trim();
                                if (lessonSubject.equalsIgnoreCase(hometaskSubject) || 
                                    lessonSubject.contains(hometaskSubject) || 
                                    hometaskSubject.contains(lessonSubject)) {
                                    ScheduleLesson.HometaskInfo taskInfo = new ScheduleLesson.HometaskInfo(
                                        hometask.getTask(), hometask.isDone(), subject);
                                    if (lesson.hometasks == null) {
                                        lesson.hometasks = new ArrayList<>();
                                    }
                                    lesson.hometasks.add(taskInfo);
                                    found = true;
                                    break;
                                }
                            }
                        }
                        // Если урок не найден, создаем отдельный элемент для задания
                        if (!found) {
                            ScheduleLesson taskLesson = new ScheduleLesson(
                                ScheduleLesson.LessonType.LECTURE, "", "", subject, "", "");
                            ScheduleLesson.HometaskInfo taskInfo = new ScheduleLesson.HometaskInfo(
                                hometask.getTask(), hometask.isDone(), subject);
                            taskLesson.hometasks = new ArrayList<>();
                            taskLesson.hometasks.add(taskInfo);
                            dayLessons.add(taskLesson);
                        }
                    }
                }
            }
            
            // Сортируем по времени
            dayLessons.sort((l1, l2) -> {
                if (l1.isHeader || l2.isHeader) return 0;
                try {
                    String time1 = l1.time.split("\n")[0];
                    String time2 = l2.time.split("\n")[0];
                    String[] parts1 = time1.split(":");
                    String[] parts2 = time2.split(":");
                    int minutes1 = Integer.parseInt(parts1[0]) * 60 + Integer.parseInt(parts1[1]);
                    int minutes2 = Integer.parseInt(parts2[0]) * 60 + Integer.parseInt(parts2[1]);
                    return Integer.compare(minutes1, minutes2);
                } catch (Exception e) {
                    return 0;
                }
            });
            
            weekDays.add(new WeekDayAdapter.WeekDayData(dayDate, dayLessons));
        }
        
        if (weekDayAdapter == null) {
            weekDayAdapter = new WeekDayAdapter(weekDays);
            if (binding != null) {
                binding.scheduleList.setAdapter(weekDayAdapter);
            }
        } else {
            weekDayAdapter.updateWeekDays(weekDays);
        }
    }

    private String getRelativeDateString(LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, date);
        
        if (daysDiff == 0) {
            return "Сегодня";
        } else if (daysDiff == 1) {
            return "Завтра";
        } else if (daysDiff == -1) {
            return "Вчера";
        } else if (daysDiff == 2) {
            return "Через 2 дня";
        } else if (daysDiff == 3) {
            return "Через 3 дня";
        } else if (daysDiff == 4) {
            return "Через 4 дня";
        } else if (daysDiff == 5) {
            return "Через 5 дней";
        } else if (daysDiff == 6) {
            return "Через 6 дней";
        } else if (daysDiff == 7) {
            return "Через неделю";
        } else if (daysDiff == -2) {
            return "2 дня назад";
        } else if (daysDiff == -3) {
            return "3 дня назад";
        } else if (daysDiff == -4) {
            return "4 дня назад";
        } else if (daysDiff == -5) {
            return "5 дней назад";
        } else if (daysDiff == -6) {
            return "6 дней назад";
        } else if (daysDiff == -7) {
            return "Неделю назад";
        } else {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }


    private void updateDateHeader() {
        if (binding == null) return;
        
        if (isWeekViewMode) {
            // Week view: show week range (Monday to Sunday)
            LocalDate weekStart = LocalDate.now().plusWeeks(currentWeekOffset).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            String weekRange = weekStart.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + 
                             " - " + weekEnd.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            binding.tvCurrentDate.setText(weekRange);
        } else {
            // Day view: show single date with format dd.MM.yyyy
            LocalDate targetDate = LocalDate.now().plusDays(currentDayOffset);
            String formattedDate = targetDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            binding.tvCurrentDate.setText(formattedDate);
        }
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

    private void setupCountdownTimer() {
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateCountdown();
                countdownHandler.postDelayed(this, 1000); // Update every second
            }
        };
        countdownHandler.post(countdownRunnable);
    }

    private void updateCountdown() {
        if (binding == null) return;

        Schedules currentSchedule = schedulelController.getCurrentSchedule();
        if (currentSchedule == null) {
            binding.tvNextClassTime.setText("");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        int currentMinutes = now.getHour() * 60 + now.getMinute();

        // Get today's schedule
        Day_Schedule daySchedule = currentSchedule.getScheduleForDayOffset(0);
        if (daySchedule == null) {
            binding.tvNextClassTime.setText("");
            return;
        }

        String lessonsRaw = daySchedule.get_lesson();
        if (lessonsRaw == null || lessonsRaw.trim().isEmpty()) {
            binding.tvNextClassTime.setText("");
            return;
        }

        // Find current or next lesson
        String[] lessonBlocks = lessonsRaw.split("\\s*(?=\\d+\\))");
        Integer currentLessonStart = null;
        Integer currentLessonEnd = null;
        Integer nextLessonStart = null;

        for (String block : lessonBlocks) {
            try {
                String lessonNumber = block.substring(0, block.indexOf(')'));
                int lessonIndex = Integer.parseInt(lessonNumber) - 1;
                
                Integer startTime = daySchedule.get_Lesson_StartTime(lessonIndex);
                Integer endTime = daySchedule.get_Lesson_EndTime(lessonIndex);
                
                if (startTime == null || endTime == null || startTime == 0) continue;

                // Check if this is the current lesson
                if (currentMinutes >= startTime && currentMinutes < endTime) {
                    currentLessonStart = startTime;
                    currentLessonEnd = endTime;
                }
                // Check if this is the next lesson
                if (nextLessonStart == null && currentMinutes < startTime) {
                    nextLessonStart = startTime;
                }
            } catch (Exception e) {
                // Skip invalid lesson blocks
            }
        }

        // Update countdown text
        if (currentLessonStart != null && currentLessonEnd != null) {
            // Currently in a lesson - show time until end
            int minutesRemaining = currentLessonEnd - currentMinutes;
            if (minutesRemaining > 0) {
                int hours = minutesRemaining / 60;
                int mins = minutesRemaining % 60;
                if (hours > 0) {
                    binding.tvNextClassTime.setText(String.format(Locale.getDefault(), 
                        "До конца занятия осталось %d ч %d мин", hours, mins));
                } else {
                    binding.tvNextClassTime.setText(String.format(Locale.getDefault(), 
                        "До конца занятия осталось %d мин", mins));
                }
            } else {
                binding.tvNextClassTime.setText("Занятие закончилось");
            }
        } else if (nextLessonStart != null) {
            // Next lesson today
            int minutesRemaining = nextLessonStart - currentMinutes;
            if (minutesRemaining > 0) {
                int hours = minutesRemaining / 60;
                int mins = minutesRemaining % 60;
                if (hours > 0) {
                    binding.tvNextClassTime.setText(String.format(Locale.getDefault(), 
                        "До начала занятия осталось %d ч %d мин", hours, mins));
                } else {
                    binding.tvNextClassTime.setText(String.format(Locale.getDefault(), 
                        "До начала занятия осталось %d мин", mins));
                }
            } else {
                binding.tvNextClassTime.setText("Занятие начинается");
            }
        } else {
            // No more lessons today
            binding.tvNextClassTime.setText("Занятия закончились");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop countdown timer
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
        // При уничтожении View, обнуляем binding, чтобы избежать утечек памяти.
        binding = null;
    }
}
