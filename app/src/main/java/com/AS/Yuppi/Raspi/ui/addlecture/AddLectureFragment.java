package com.AS.Yuppi.Raspi.ui.addlecture;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.databinding.FragmentAddLectureBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddLectureFragment extends Fragment {

    private AddLectureViewModel viewModel;
    private FragmentAddLectureBinding binding;
    private final Calendar myCalendar = Calendar.getInstance();
    private SchedulelController schedulelController;
    private String mode = "note"; // "note" для заметок
    private int noteIndex = -1; // индекс заметки для редактирования
    private String scheduleName; // Имя расписания для заметки
    private String scheduleAuthor; // Автор расписания для заметки

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AddLectureViewModel.class);
        binding = FragmentAddLectureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        schedulelController = MySingleton.getInstance(requireContext().getApplicationContext()).getSchedulelController();

        if (getArguments() != null) {
            mode = getArguments().getString("mode", "note");
            noteIndex = getArguments().getInt("noteIndex", -1);
            scheduleName = getArguments().getString("scheduleName");
            scheduleAuthor = getArguments().getString("scheduleAuthor");
        }

        setupSubjectSpinner();
        setupDatePicker();
        prefillIfEditing();

        binding.fabSaveLecture.setOnClickListener(v -> saveNote());
    }
    
    private void prefillIfEditing() {
        if (noteIndex < 0) return;
        
        // Загружаем расписание из аргументов или используем текущее
        Schedules current = null;
        if (scheduleName != null && scheduleAuthor != null) {
            String authorNameString = scheduleAuthor + "-" + scheduleName;
            schedulelController.loadCurrentSchedule(authorNameString);
            current = schedulelController.getCurrentSchedule();
        }
        
        if (current == null) {
            current = schedulelController.getCurrentSchedule();
        }
        
        if (current == null) return;
        List<Schedules.Note> notes = current.getNotes();
        if (notes == null || noteIndex >= notes.size()) return;
        
        Schedules.Note note = notes.get(noteIndex);
        if (note == null) return;
        
        // Заполняем поля
        String subject = note.getLesson();
        if (binding.spinnerSubject.getVisibility() == View.VISIBLE) {
            ArrayAdapter adapter = (ArrayAdapter) binding.spinnerSubject.getAdapter();
            if (adapter != null) {
                int pos = adapter.getPosition(subject);
                if (pos >= 0) binding.spinnerSubject.setSelection(pos);
            }
        }
        
        binding.etLectureNotes.setText(note.getData());
    }
    
    private void saveNote() {
        String subject = "";
        if (binding.spinnerSubject.getVisibility() == View.VISIBLE && binding.spinnerSubject.getSelectedItem() != null) {
            subject = binding.spinnerSubject.getSelectedItem().toString();
        }
        
        if (subject.isEmpty() || "Выберите предмет".equals(subject)) {
            Toast.makeText(requireContext(), "Выберите предмет", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String data = binding.etLectureNotes.getText().toString().trim();
        if (data.isEmpty()) {
            Toast.makeText(requireContext(), "Введите текст заметки", Toast.LENGTH_SHORT).show();
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
        
        // Загружаем расписание из БД для получения актуальной версии
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
        
        List<Schedules.Note> notes = current.getNotes();
        if (notes == null) {
            notes = new ArrayList<>();
            current.setNotes(notes);
        }
        
        if (noteIndex >= 0 && noteIndex < notes.size()) {
            // Редактирование существующей заметки
            Schedules.Note note = notes.get(noteIndex);
            note.setLesson(subject);
            note.setData(data);
        } else {
            // Добавление новой заметки
            Schedules.Note note = current.addNote(subject, data);
            note.setPersonal(false);
        }
        
        // Сохраняем изменения в БД
        schedulelController.seteditableSchedule(current);
        schedulelController.saveEditableScheduleToDB();
        // Обновляем текущее расписание
        schedulelController.setCurrentSchedule(current);
        Toast.makeText(requireContext(), "Заметка сохранена", Toast.LENGTH_SHORT).show();
        
        NavHostFragment.findNavController(this).navigateUp();
    }
    private void setupSubjectSpinner() {
        // Загружаем реальные предметы из расписания
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
        DatePickerDialog.OnDateSetListener dateSetListener = (v, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        binding.etDate.setOnClickListener(v -> new DatePickerDialog(requireContext(), dateSetListener,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
        ).show());
        // Установим текущую дату при открытии
        updateLabel();
    }

    private void updateLabel() {
        String myFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etDate.setText(sdf.format(myCalendar.getTime()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Обязательно для избежания утечек памяти
    }
}
