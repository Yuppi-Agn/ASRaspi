package com.AS.Yuppi.Raspi.ui.addhometask;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentAddHometaskBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddHometaskFragment extends Fragment {

    private AddHometaskViewModel addHometaskViewModel;
    private FragmentAddHometaskBinding binding;
    private final Calendar myCalendar = Calendar.getInstance();

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

        setupSubjectSpinner();
        setupDatePicker();

        // Обработчик для плавающей кнопки "Сохранить"
        binding.fabSaveTask.setOnClickListener(v -> {
            // TODO: Добавить логику сохранения задания (в базу данных или ViewModel)
            // String subject = binding.spinnerSubject.getSelectedItem().toString();
            // String date = binding.etDate.getText().toString();
            // String description = binding.etTaskDescription.getText().toString();

            // После сохранения возвращаемся на предыдущий экран
            NavHostFragment.findNavController(this).navigateUp();
        });
    }
    private void setupSubjectSpinner() {
        // TODO: Загрузите реальный список предметов из вашей базы данных или ресурсов
        String[] subjects = {"Дискретная математика", "Программирование", "Физика", "История"};

        // Создаем адаптер для Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Применяем адаптер к Spinner
        binding.spinnerSubject.setAdapter(adapter);
    }

    private void setupDatePicker() {
        // Создаем слушателя для выбора даты
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateLabel();
        };

        // Устанавливаем обработчик клика на поле ввода даты
        binding.etDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), dateSetListener,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateLabel() {
        // Форматируем дату в нужный вид и устанавливаем в поле ввода
        String myFormat = "dd/MM/yyyy"; // Формат даты
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etDate.setText(sdf.format(myCalendar.getTime()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Обязательно для избежания утечек памяти
    }
}
