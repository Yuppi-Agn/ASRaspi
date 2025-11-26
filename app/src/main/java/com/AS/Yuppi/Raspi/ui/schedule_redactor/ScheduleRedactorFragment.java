package com.AS.Yuppi.Raspi.ui.schedule_redactor;

import android.content.Context;
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
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.DataWorkers.UserController;
import com.AS.Yuppi.Raspi.DataWorkers.UserEvents;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentScheduleRedactorBinding;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

// --- Модель данных, универсальная для обоих списков ---
class EditorItem {
    String title;
    String details;
    boolean isSelected; // Для управления иконкой выбора
    // Дополнительное поле для хранения служебных данных (например, "Author-Name")
    String meta;

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

    interface OnEditorItemActionListener {
        void onItemSelected(int position, EditorItem item);
        void onEdit(int position, EditorItem item);
        void onDelete(int position, EditorItem item);
    }

    private OnEditorItemActionListener actionListener;

    public EditorAdapter(List<EditorItem> items) {
        this.items = items;
    }

    public void setOnEditorItemActionListener(OnEditorItemActionListener listener) {
        this.actionListener = listener;
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
            if (actionListener != null) {
                actionListener.onItemSelected(holder.getBindingAdapterPosition(), currentItem);
            }
        });

        holder.btnEditItem.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(holder.getBindingAdapterPosition(), currentItem);
            }
        });

        holder.btnDeleteItem.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(holder.getBindingAdapterPosition(), currentItem);
            }
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

    private SchedulelController schedulelController;
    private UserController userController;
    private boolean isEventsMode = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MySingleton singleton = MySingleton.getInstance(context.getApplicationContext());
        schedulelController = singleton.getSchedulelController();
        userController = singleton.getUserController();
    }


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

        setupBottomButtons();
    }

    /**
     * Метод для подготовки списков с реальными данными расписаний и мероприятий.
     */
    private void prepareDataLists() {
        scheduleItems = new ArrayList<>();

        // Загружаем список строк "Author-Name" из контроллера
        List<String> rawList = schedulelController.fillSchedulesListFromDB();
        for (String entry : rawList) {
            String author = entry;
            String name = entry;
            String[] parts = entry.split("-", 2);
            if (parts.length == 2) {
                author = parts[0].trim();
                name = parts[1].trim();
            }
            EditorItem item = new EditorItem(name, "Автор: " + author);
            item.meta = entry; // сохраняем полную строку "Author-Name" для дальнейших операций
            scheduleItems.add(item);
        }

        eventItems = new ArrayList<>();
        List<UserEvents> events = userController.getAllEvents();
        for (UserEvents event : events) {
            String title = event.getName();
            String details = event.getDate() + " " + String.format("%02d:%02d", event.getTime() / 60, event.getTime() % 60);
            if (event.getInfo() != null && !event.getInfo().isEmpty()) {
                details += " — " + event.getInfo();
            }
            EditorItem item = new EditorItem(title, details);
            item.meta = String.valueOf(event.getId()); // сохраняем ID события
            item.isSelected = event.isEnable();        // используем выбор как индикатор включенности
            eventItems.add(item);
        }

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
        editorAdapter.setOnEditorItemActionListener(new EditorAdapter.OnEditorItemActionListener() {
            @Override
            public void onItemSelected(int position, EditorItem item) {
                if (!isEventsMode) {
                    if (item.meta != null) {
                        schedulelController.loadCurrentSchedule(item.meta);
                        Toast.makeText(requireContext(), "Текущее расписание: " + item.title, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Переключаем включенность события
                    if (item.meta != null) {
                        int id = Integer.parseInt(item.meta);
                        userController.toggleEventEnabled(id);
                        // Перечитываем список событий
                        prepareDataLists();
                        editorAdapter.updateData(new ArrayList<>(eventItems));
                    }
                }
            }

            @Override
            public void onEdit(int position, EditorItem item) {
                if (!isEventsMode) {
                    // Загружаем выбранное расписание в editableSchedule и открываем экран редактора
                    if (item.meta != null) {
                        schedulelController.loadCurrentSchedule(item.meta);
                        Schedules current = schedulelController.getCurrentSchedule();
                        if (current != null) {
                            schedulelController.seteditableSchedule(current);
                        }
                    }
                    NavHostFragment.findNavController(ScheduleRedactorFragment.this)
                            .navigate(R.id.nav_schedule_redactor2);
                } else {
                    // Открываем редактор события
                    if (item.meta != null) {
                        int id = Integer.parseInt(item.meta);
                        Bundle args = new Bundle();
                        args.putInt("eventId", id);
                        NavHostFragment.findNavController(ScheduleRedactorFragment.this)
                                .navigate(R.id.nav_event_editor, args);
                    }
                }
            }

            @Override
            public void onDelete(int position, EditorItem item) {
                if (!isEventsMode) {
                    if (item.meta != null) {
                        schedulelController.deleteSchedule(item.meta);
                        Toast.makeText(requireContext(), "Расписание удалено: " + item.title, Toast.LENGTH_SHORT).show();
                        // Обновляем список после удаления
                        prepareDataLists();
                        editorAdapter.updateData(new ArrayList<>(scheduleItems));
                    }
                } else {
                    if (item.meta != null) {
                        int id = Integer.parseInt(item.meta);
                        userController.deleteEvent(id);
                        Toast.makeText(requireContext(), "Событие удалено: " + item.title, Toast.LENGTH_SHORT).show();
                        prepareDataLists();
                        editorAdapter.updateData(new ArrayList<>(eventItems));
                    }
                }
            }
        });
        recyclerView.setAdapter(editorAdapter);
    }

    private void setupTabClickListeners() {
        // Нажатие на "Расписания"
        binding.cardSchedules.setOnClickListener(v -> {
            isEventsMode = false;
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
            isEventsMode = true;
            binding.schedulesButtonsLayout.setVisibility(View.GONE);
            binding.schedulesButtons2Layout.setVisibility(View.VISIBLE);
            binding.cardSchedules.setStrokeWidth(0);
            binding.cardEvents.setStrokeWidth(dpToPx(1));
            binding.tvListHeader.setText("Список мероприятий");
            binding.etSearch.setHint("Поиск по мероприятиям");

            // --- Обновляем данные в адаптере на список МЕРОПРИЯТИЙ ---
            prepareDataLists();
            editorAdapter.updateData(eventItems);
        });
    }

    private void setupBottomButtons() {
        // Кнопка "Создать расписание"
        binding.btnCreateSchedule.setOnClickListener(v -> {
            // Создаем новое редактируемое расписание
            schedulelController.createeditableSchedule();
            Toast.makeText(requireContext(), "Создано новое расписание", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(ScheduleRedactorFragment.this)
                    .navigate(R.id.nav_schedule_redactor2);
        });

        // Кнопка "Прочитать из файла" — просто переходим в старый редактор, где уже есть логика импорта
        binding.btnReadFromFile.setOnClickListener(v -> {
            NavHostFragment.findNavController(ScheduleRedactorFragment.this)
                    .navigate(R.id.nav_schedule_redactor2);
        });

        // Кнопка "Добавить мероприятие"
        binding.btnAddEvent.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("eventId", -1); // новый ивент
            NavHostFragment.findNavController(ScheduleRedactorFragment.this)
                    .navigate(R.id.nav_event_editor, args);
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
