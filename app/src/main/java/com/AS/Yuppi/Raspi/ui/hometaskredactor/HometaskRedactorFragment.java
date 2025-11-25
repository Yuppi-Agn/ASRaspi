package com.AS.Yuppi.Raspi.ui.hometaskredactor;import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentHometaskRedactorBinding;

import java.util.ArrayList;
import java.util.List;

// --- Модель данных для одной задачи ---
class HometaskItem {
    String title;
    String dueDate;
    String description;
    boolean isCompleted;

    public HometaskItem(String title, String dueDate, String description, boolean isCompleted) {
        this.title = title;
        this.dueDate = dueDate;
        this.description = description;
        this.isCompleted = isCompleted;
    }
}

// --- Адаптер для RecyclerView ---
class HometaskAdapter extends RecyclerView.Adapter<HometaskAdapter.HometaskViewHolder> {

    private List<HometaskItem> items;
    private Context context;

    public HometaskAdapter(List<HometaskItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void updateData(List<HometaskItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HometaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hometask, parent, false);
        return new HometaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HometaskViewHolder holder, int position) {
        HometaskItem currentItem = items.get(position);

        holder.tvTaskTitle.setText(currentItem.title);
        holder.tvTaskDueDate.setText(currentItem.dueDate);
        holder.tvTaskDescription.setText(currentItem.description);

        // --- ЛОГИКА ДЛЯ ИКОНКИ ВЫПОЛНЕНИЯ (ИЗМЕНЕНО) ---
        if (currentItem.isCompleted) {
            holder.btnTaskComplete.setImageResource(R.drawable.done_outline_24);
            holder.btnTaskComplete.setColorFilter(ContextCompat.getColor(context, R.color.green)); // Зеленый цвет для tint
        } else {
            // ИЗМЕНЕНО: Устанавливаем иконку красного крестика
            holder.btnTaskComplete.setImageResource(R.drawable.close_24);
            // ИЗМЕНЕНО: Устанавливаем красный цвет для tint
            holder.btnTaskComplete.setColorFilter(ContextCompat.getColor(context, R.color.red));
        }

        // Обработчик для смены состояния
        holder.btnTaskComplete.setOnClickListener(v -> {
            currentItem.isCompleted = !currentItem.isCompleted;
            notifyItemChanged(position);
            String status = currentItem.isCompleted ? "Выполнено" : "Не выполнено";
            Toast.makeText(v.getContext(), status + ": " + currentItem.title, Toast.LENGTH_SHORT).show();
        });

        holder.btnTaskEdit.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Редактировать: " + currentItem.title, Toast.LENGTH_SHORT).show();
        });

        holder.btnTaskDelete.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Удалить: " + currentItem.title, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class HometaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTitle, tvTaskDueDate, tvTaskDescription;
        ImageButton btnTaskComplete, btnTaskEdit, btnTaskDelete;

        public HometaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            tvTaskDueDate = itemView.findViewById(R.id.tv_task_due_date);
            tvTaskDescription = itemView.findViewById(R.id.tv_task_description);
            btnTaskComplete = itemView.findViewById(R.id.btn_task_complete);
            btnTaskEdit = itemView.findViewById(R.id.btn_task_edit);
            btnTaskDelete = itemView.findViewById(R.id.btn_task_delete);
        }
    }
}


public class HometaskRedactorFragment extends Fragment {

    private HometaskRedactorViewModel hometaskRedactorViewModel;
    private FragmentHometaskRedactorBinding binding;

    private HometaskAdapter hometaskAdapter;
    private List<HometaskItem> studyTasks;
    private List<HometaskItem> personalTasks;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        hometaskRedactorViewModel =
                new ViewModelProvider(this).get(HometaskRedactorViewModel.class);

        binding = FragmentHometaskRedactorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null) {
            Log.e("HometaskRedactor", "ОШИБКА: View Binding не был инициализирован!");
            return;
        }

        prepareDataLists();
        setupRecyclerView();
        setupClickListeners();
    }

    private void prepareDataLists() {
        // Данные для "Учебных"
        studyTasks = new ArrayList<>();
        studyTasks.add(new HometaskItem("Высшая математика", "Конечная дата: 08.09.2025", "Стр. 1 №1-8", true));
        studyTasks.add(new HometaskItem("Информатика", "Конечная дата: 10.09.2025", "Подготовить доклад о структурах данных", true));
        studyTasks.add(new HometaskItem("Физика", "Конечная дата: 12.09.2025", "Лабораторная работа №3", true));

        // Данные для "Личных"
        personalTasks = new ArrayList<>();
        personalTasks.add(new HometaskItem("Купить продукты", "Конечная дата: сегодня", "Молоко, хлеб, яйца", true));
        // Последний элемент теперь не выполнен
        personalTasks.add(new HometaskItem("Записаться в спортзал", "Конечная дата: до конца недели", "", false));
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.rvHometasks;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Передаем контекст в адаптер
        hometaskAdapter = new HometaskAdapter(new ArrayList<>(studyTasks), getContext());
        recyclerView.setAdapter(hometaskAdapter);
    }

    private void setupClickListeners() {
        binding.cardStudyTasks.setOnClickListener(v -> {
            binding.cardStudyTasks.setStrokeWidth(dpToPx(1));
            binding.cardPersonalTasks.setStrokeWidth(0);
            binding.tvListHeader.setText("Учебные задания");
            hometaskAdapter.updateData(studyTasks);
        });

        binding.cardPersonalTasks.setOnClickListener(v -> {
            binding.cardPersonalTasks.setStrokeWidth(dpToPx(1));
            binding.cardStudyTasks.setStrokeWidth(0);
            binding.tvListHeader.setText("Личные задания");
            hometaskAdapter.updateData(personalTasks);
        });

        binding.btnAddHometask.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Открытие экрана добавления задания", Toast.LENGTH_SHORT).show();
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) {
            return 0;
        }
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
