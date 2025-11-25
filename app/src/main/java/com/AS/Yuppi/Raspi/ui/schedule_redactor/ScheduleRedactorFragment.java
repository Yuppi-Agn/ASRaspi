package com.AS.Yuppi.Raspi.ui.schedule_redactor;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView; // Импорт для ImageView
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentScheduleRedactorBinding;

import java.util.ArrayList;
import java.util.List;

// --- Модель данных, универсальная для обоих списков ---
class EditorItem {
    String title;
    String details;
    boolean isSelected; // Для управления иконкой выбора

    public EditorItem(String title, String details) {
        this.title = title;
        this.details = details;
        this.isSelected = false; // По умолчанию не выбрано
    }
}

// --- Адаптер, который мы будем использовать для обоих списков ---
class EditorAdapter extends RecyclerView.Adapter<EditorAdapter.EditorViewHolder> {

    // Теперь список можно будет менять извне
    private List<EditorItem> items;

    public EditorAdapter(List<EditorItem> items) {
        this.items = items;
    }

    // Метод для обновления данных в адаптере
    public void updateData(List<EditorItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged(); // Сообщаем RecyclerView, что данные изменились
    }


    @NonNull
    @Override
    public EditorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_redactor, parent, false);
        return new EditorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditorViewHolder holder, int position) {
        EditorItem currentItem = items.get(position);

        holder.tvItemTitle.setText(currentItem.title);
        holder.tvItemDetails.setText(currentItem.details);

        if (currentItem.details != null && !currentItem.details.isEmpty()) {
            holder.tvItemDetails.setVisibility(View.VISIBLE);
        } else {
            holder.tvItemDetails.setVisibility(View.GONE);
        }

        if (currentItem.isSelected) {
            holder.ivSelectionIndicator.setImageResource(R.drawable.radio_button_checked_24);
        } else {
            holder.ivSelectionIndicator.setImageResource(R.drawable.radio_button_unchecked_24);
        }

        holder.ivSelectionIndicator.setOnClickListener(v -> {
            for(EditorItem item : items) {
                item.isSelected = false;
            }
            currentItem.isSelected = true;
            notifyDataSetChanged();
            Toast.makeText(v.getContext(), "Выбрано: " + currentItem.title, Toast.LENGTH_SHORT).show();
        });

        holder.btnEditItem.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Редактировать: " + currentItem.title, Toast.LENGTH_SHORT).show();
        });

        holder.btnDeleteItem.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Удалить: " + currentItem.title, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        // Проверяем на null на всякий случай
        return items != null ? items.size() : 0;
    }

    public static class EditorViewHolder extends RecyclerView.ViewHolder {
        public TextView tvItemTitle;
        public TextView tvItemDetails;
        public ImageView ivSelectionIndicator;
        public ImageButton btnEditItem;
        public ImageButton btnDeleteItem;

        public EditorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemTitle = itemView.findViewById(R.id.tv_item_title);
            tvItemDetails = itemView.findViewById(R.id.tv_item_details);
            ivSelectionIndicator = itemView.findViewById(R.id.iv_selection_indicator);
            btnEditItem = itemView.findViewById(R.id.btn_edit_item);
            btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}


public class ScheduleRedactorFragment extends Fragment {

    private ScheduleRedactorViewModel scheduleRedactorViewModel;
    private FragmentScheduleRedactorBinding binding;

    private RecyclerView recyclerView;
    private EditorAdapter editorAdapter;

    // --- Создаем два разных списка для данных ---
    private List<EditorItem> scheduleItems;
    private List<EditorItem> eventItems;


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        scheduleRedactorViewModel = new ViewModelProvider(this).get(ScheduleRedactorViewModel.class);
        binding = FragmentScheduleRedactorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (binding == null) {
            Log.e("ScheduleRedactor", "ОШИБКА: Binding is null in onViewCreated.");
            return;
        }

        // 1. Готовим данные
        prepareDataLists();
        // 2. Настраиваем RecyclerView с начальными данными
        setupRecyclerView();
        // 3. Настраиваем клики по табам для смены данных
        setupTabClickListeners();
    }

    /**
     * Метод для подготовки списков с данными-заглушками.
     */
    private void prepareDataLists() {
        // Данные для "Расписаний"
        scheduleItems = new ArrayList<>();
        scheduleItems.add(new EditorItem("ИГХТУ ИТИЦЭ 1/233", "С 9 сентября 13:40-15:00, каждые 3 дня"));
        scheduleItems.add(new EditorItem("Школа 21, бассейн С++", "С 10 ноября, каждый день"));

        // Данные для "Мероприятий"
        eventItems = new ArrayList<>();
        eventItems.add(new EditorItem("День рождения друга", "25 декабря, 18:00"));
        eventItems.add(new EditorItem("Поход к врачу", "15 ноября, 10:30, каб. 305"));
        eventItems.add(new EditorItem("Встреча по проекту Yuppi", "Каждую пятницу в 16:00"));

        // Устанавливаем одному элементу в начальном списке статус "выбран"
        if (!scheduleItems.isEmpty()) {
            scheduleItems.get(0).isSelected = true;
        }
    }

    private void setupRecyclerView() {
        recyclerView = binding.recyclerViewEditor;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Создаем адаптер с начальными данными (списком расписаний)
        editorAdapter = new EditorAdapter(new ArrayList<>(scheduleItems));
        recyclerView.setAdapter(editorAdapter);
    }

    private void setupTabClickListeners() {
        // Нажатие на "Расписания"
        binding.cardSchedules.setOnClickListener(v -> {
            binding.schedulesButtonsLayout.setVisibility(View.VISIBLE);
            binding.schedulesButtons2Layout.setVisibility(View.GONE);
            binding.cardSchedules.setStrokeWidth(dpToPx(1));
            binding.cardEvents.setStrokeWidth(0);
            binding.tvListHeader.setText("Список расписаний");
            binding.etSearch.setHint("Поиск по расписаниям");

            // --- Обновляем данные в адаптере на список РАСПИСАНИЙ ---
            editorAdapter.updateData(scheduleItems);
        });

        // Нажатие на "Мероприятия"
        binding.cardEvents.setOnClickListener(v -> {
            binding.schedulesButtonsLayout.setVisibility(View.GONE);
            binding.schedulesButtons2Layout.setVisibility(View.VISIBLE);
            binding.cardSchedules.setStrokeWidth(0);
            binding.cardEvents.setStrokeWidth(dpToPx(1));
            binding.tvListHeader.setText("Список мероприятий");
            binding.etSearch.setHint("Поиск по мероприятиям");

            // --- Обновляем данные в адаптере на список МЕРОПРИЯТИЙ ---
            editorAdapter.updateData(eventItems);
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return 0;
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
