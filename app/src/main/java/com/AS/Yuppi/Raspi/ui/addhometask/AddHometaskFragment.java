package com.AS.Yuppi.Raspi.ui.addhometask;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.DataWorkers.UserController;
import com.AS.Yuppi.Raspi.DataWorkers.UserTask;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentAddHometaskBinding;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddHometaskFragment extends Fragment {

    private AddHometaskViewModel addHometaskViewModel;
    private FragmentAddHometaskBinding binding;
    private Calendar myCalendar;

    private SchedulelController schedulelController;
    private UserController userController;
    private String mode = "study"; // "study" или "personal"
    private int taskId = -1; // для study: индекс, для personal: id в БД
    private String scheduleName; // Имя расписания для задания
    private String scheduleAuthor; // Автор расписания для задания

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        addHometaskViewModel = new ViewModelProvider(this).get(AddHometaskViewModel.class);
        binding = FragmentAddHometaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Инициализируем календарь текущей датой из системного времени
        myCalendar = Calendar.getInstance();
        myCalendar.setTimeInMillis(System.currentTimeMillis());

        MySingleton singleton = MySingleton.getInstance(requireContext().getApplicationContext());
        schedulelController = singleton.getSchedulelController();
        userController = singleton.getUserController();

        if (getArguments() != null) {
            mode = getArguments().getString("mode", "study");
            taskId = getArguments().getInt("taskId", -1);
            scheduleName = getArguments().getString("scheduleName");
            scheduleAuthor = getArguments().getString("scheduleAuthor");
        }

        setupSubjectSpinner();
        setupDatePicker();
        setupLayoutForMode();
        
        // Обновляем подсказку в зависимости от режима
        if ("personal".equals(mode)) {
            binding.etTaskDescription.setHint("Введите описание задания...");
        } else {
            binding.etTaskDescription.setHint("Введите описание задания...");
        }
        
        // Если не редактируем, устанавливаем сегодняшнюю дату по умолчанию
        if (taskId < 0) {
            // Обновляем календарь до текущей даты из системного времени
            myCalendar.setTimeInMillis(System.currentTimeMillis());
            if ("personal".equals(mode)) {
                if (binding.etDatePersonal.getText().toString().trim().isEmpty()) {
                    updateLabelPersonal();
                }
            } else {
                if (binding.etDate.getText().toString().trim().isEmpty()) {
                    updateLabel();
                }
            }
        }
        
        prefillIfEditing();

        // Обработчик для плавающей кнопки "Сохранить"
        binding.fabSaveTask.setOnClickListener(v -> saveTask());
    }
    private void setupLayoutForMode() {
        if ("personal".equals(mode)) {
            // Для личных задач: показываем контейнер с названием и датой, скрываем спиннер предметов
            binding.personalTaskContainer.setVisibility(View.VISIBLE);
            binding.topControlsContainer.setVisibility(View.GONE);
            binding.etTaskDescription.setHint("Введите описание задания...");
        } else {
            // Для учебных задач: показываем спиннер предметов, скрываем контейнер для личных задач
            binding.personalTaskContainer.setVisibility(View.GONE);
            binding.topControlsContainer.setVisibility(View.VISIBLE);
            binding.etTaskDescription.setHint("Введите описание задания...");
        }
    }

    private void setupSubjectSpinner() {
        if ("personal".equals(mode)) {
            // Для личных задач спиннер не нужен
            return;
        }
        
        // Для учебных задач загружаем реальные предметы из расписания
        Schedules current = schedulelController.getCurrentSchedule();
        List<String> subjectsList = new ArrayList<>();
        
        if (current != null) {
            subjectsList = current.getSubjectsList();
        }
        
        // Если список пуст, добавляем заглушку
        if (subjectsList.isEmpty()) {
            subjectsList.add("Выберите предмет");
        }
        
        // Создаем адаптер для Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjectsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Применяем адаптер к Spinner
        binding.spinnerSubject.setAdapter(adapter);
    }

    private void setupDatePicker() {
        // Создаем слушателя для выбора даты (для учебных задач)
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        // Создаем слушателя для выбора даты (для личных задач)
        DatePickerDialog.OnDateSetListener dateSetListenerPersonal = (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabelPersonal();
        };

        // Устанавливаем обработчик клика на поле ввода даты (учебные задачи)
        binding.etDate.setOnClickListener(v -> {
            // Обновляем календарь до текущей даты, если поле пустое или не установлено
            String dateText = binding.etDate.getText().toString().trim();
            if (dateText.isEmpty()) {
                myCalendar.setTimeInMillis(System.currentTimeMillis());
            }
            new DatePickerDialog(requireContext(), dateSetListener,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Устанавливаем обработчик клика на поле ввода даты (личные задачи)
        binding.etDatePersonal.setOnClickListener(v -> {
            // Обновляем календарь до текущей даты, если поле пустое или не установлено
            String dateText = binding.etDatePersonal.getText().toString().trim();
            if (dateText.isEmpty()) {
                myCalendar.setTimeInMillis(System.currentTimeMillis());
            }
            new DatePickerDialog(requireContext(), dateSetListenerPersonal,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateLabel() {
        // Форматируем дату в нужный вид и устанавливаем в поле ввода
        String myFormat = "dd.MM.yyyy"; // Формат даты
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etDate.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateLabelPersonal() {
        // Форматируем дату в нужный вид и устанавливаем в поле ввода для личных задач
        String myFormat = "dd.MM.yyyy"; // Формат даты
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etDatePersonal.setText(sdf.format(myCalendar.getTime()));
    }

    private void prefillIfEditing() {
        if (taskId < 0) return;

        if ("study".equals(mode)) {
            Schedules current = schedulelController.getCurrentSchedule();
            if (current == null) return;
            if (taskId < 0 || taskId >= current.getHometasks().size()) return;
            Schedules.Hometask ht = current.getHometasks().get(taskId);

            // Предмет
            String lesson = ht.getLesson();
            if (binding.spinnerSubject.getVisibility() == View.VISIBLE) {
                ArrayAdapter adapter = (ArrayAdapter) binding.spinnerSubject.getAdapter();
                if (adapter != null) {
                    int pos = adapter.getPosition(lesson);
                    if (pos >= 0) binding.spinnerSubject.setSelection(pos);
                }
            }

            // Дата
            if (ht.getEndpoint() != null) {
                LocalDate d = ht.getEndpoint();
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                binding.etDate.setText(d.format(df));
                // Обновляем календарь для date picker
                myCalendar.set(d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth());
            }

            // Описание
            binding.etTaskDescription.setText(ht.getTask());

        } else {
            UserTask t = userController.getTaskById(taskId);
            if (t == null) return;

            // Для личных задач: используем отдельные поля для названия и описания
            binding.etTaskName.setText(t.getName());
            binding.etTaskDescription.setText(t.getTask() != null ? t.getTask() : "");

            if (t.getEndpoint() != null) {
                LocalDate d = t.getEndpoint();
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                binding.etDatePersonal.setText(d.format(df));
                myCalendar.set(d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth());
            }
        }
    }

    private void saveTask() {
        String dateStr;
        if ("personal".equals(mode)) {
            dateStr = binding.etDatePersonal.getText().toString().trim();
        } else {
            dateStr = binding.etDate.getText().toString().trim();
        }
        String description = binding.etTaskDescription.getText().toString().trim();

        LocalDate endpoint = null;
        if (!dateStr.isEmpty()) {
            try {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                endpoint = LocalDate.parse(dateStr, df);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Неверный формат даты. Используйте дд.мм.гггг", Toast.LENGTH_SHORT).show();
                return;
            }
        } else if ("personal".equals(mode)) {
            // Для личных заданий дата обязательна
            Toast.makeText(requireContext(), "Выберите дату выполнения задания", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("study".equals(mode)) {
            // Учебное задание
            String subject = "";
            if (binding.spinnerSubject.getVisibility() == View.VISIBLE && binding.spinnerSubject.getSelectedItem() != null) {
                subject = binding.spinnerSubject.getSelectedItem().toString();
            }
            
            if (subject.isEmpty() || "Выберите предмет".equals(subject)) {
                Toast.makeText(requireContext(), "Выберите предмет", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (description.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Введите описание задания", Toast.LENGTH_SHORT).show();
                return;
            }

            // Получаем расписание из аргументов или текущее
            Schedules current = null;
            if (scheduleName != null && scheduleAuthor != null) {
                String authorNameString = scheduleAuthor + "-" + scheduleName;
                schedulelController.loadCurrentSchedule(authorNameString);
                current = schedulelController.getCurrentSchedule();
            }
            
            if (current == null) {
                current = schedulelController.getCurrentSchedule();
            }
            
            if (current == null) {
                // Пытаемся получить первое выбранное расписание
                List<Schedules> selected = schedulelController.getSelectedSchedulesObjects();
                if (!selected.isEmpty()) {
                    current = selected.get(0);
                    schedulelController.setCurrentSchedule(current);
                }
            }
            
            if (current == null) {
                Toast.makeText(requireContext(), "Расписание не загружено", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigateUp();
                return;
            }
            
            // Загружаем расписание из БД, чтобы получить актуальную версию
            String currentScheduleName = current.getName();
            if (currentScheduleName != null && !currentScheduleName.isEmpty()) {
                String authorNameString = current.getAuthor() + "-" + currentScheduleName;
                schedulelController.loadCurrentSchedule(authorNameString);
                current = schedulelController.getCurrentSchedule();
                if (current == null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки расписания", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                    return;
                }
            }
            
            if (taskId >= 0 && taskId < current.getHometasks().size()) {
                // Редактирование существующего
                Schedules.Hometask ht = current.getHometasks().get(taskId);
                ht.setLesson(subject);
                ht.setTask(description);
                ht.setEndpoint(endpoint);
            } else {
                // Добавление нового
                Schedules.Hometask ht = current.addHometask(subject, description, endpoint);
                ht.setPersonal(false);
            }
            
            // Сохраняем изменения в БД
            schedulelController.seteditableSchedule(current);
            schedulelController.saveEditableScheduleToDB();
            
            // Обновляем текущее расписание в контроллере (уже содержит актуальные данные)
            schedulelController.setCurrentSchedule(current);
            
            Toast.makeText(requireContext(), "Задание сохранено", Toast.LENGTH_SHORT).show();
        } else {
            // Личное задание
            String name = binding.etTaskName.getText().toString().trim();
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название задания", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Проверяем, что дата была введена
            if (endpoint == null) {
                Toast.makeText(requireContext(), "Выберите дату выполнения задания", Toast.LENGTH_SHORT).show();
                return;
            }

            // Убеждаемся, что дата правильно установлена перед созданием UserTask
            if (endpoint == null) {
                Toast.makeText(requireContext(), "Ошибка: дата не установлена", Toast.LENGTH_SHORT).show();
                return;
            }
            
            UserTask t = new UserTask(endpoint, name, description);
            if (taskId > 0) {
                t.setId(taskId);
            }
            // Дополнительно убеждаемся, что дата правильно установлена
            t.setEndpoint(endpoint);
            
            // Проверяем, что дата действительно установлена
            if (t.getEndpoint() == null) {
                Toast.makeText(requireContext(), "Ошибка: не удалось установить дату", Toast.LENGTH_SHORT).show();
                return;
            }
            
            userController.saveOrUpdateTask(t);
            Toast.makeText(requireContext(), "Задание сохранено", Toast.LENGTH_SHORT).show();
        }

        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Обязательно для избежания утечек памяти
    }
}
