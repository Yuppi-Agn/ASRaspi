package com.AS.Yuppi.Raspi.ui.addlecture;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.databinding.FragmentAddLectureBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddLectureFragment extends Fragment {

    private AddLectureViewModel viewModel;
    private FragmentAddLectureBinding binding;
    private final Calendar myCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(AddLectureViewModel.class);
        binding = FragmentAddLectureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupSubjectSpinner();
        setupDatePicker();

        binding.fabSaveLecture.setOnClickListener(v -> {
            // TODO: Добавить логику сохранения записи к лекции
            // String subject = binding.spinnerSubject.getSelectedItem().toString();
            // String date = binding.etDate.getText().toString();
            // String notes = binding.etLectureNotes.getText().toString();

            // Возвращаемся на предыдущий экран
            NavHostFragment.findNavController(this).navigateUp();
        });
    }
    private void setupSubjectSpinner() {
        // TODO: Замените этот массив реальным списком предметов
        String[] subjects = {"Дискретная математика", "Программирование", "Физика", "История"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, subjects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        binding.etDate.setText(sdf.format(myCalendar.getTime()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Обязательно для избежания утечек памяти
    }
}
