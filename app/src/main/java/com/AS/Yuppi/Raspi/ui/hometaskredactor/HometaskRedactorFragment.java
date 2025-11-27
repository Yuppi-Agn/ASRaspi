package com.AS.Yuppi.Raspi.ui.hometaskredactor;import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.navigation.fragment.NavHostFragment;

import com.AS.Yuppi.Raspi.DataWorkers.MySingleton;
import com.AS.Yuppi.Raspi.DataWorkers.SchedulelController;
import com.AS.Yuppi.Raspi.DataWorkers.Schedules;
import com.AS.Yuppi.Raspi.DataWorkers.UserController;
import com.AS.Yuppi.Raspi.DataWorkers.UserTask;
import com.AS.Yuppi.Raspi.R;
import com.AS.Yuppi.Raspi.databinding.FragmentHometaskRedactorBinding;

import java.util.ArrayList;
import java.util.List;

// --- Модель данных для одной задачи ---
class HometaskItem {
    enum TaskType { STUDY, PERSONAL, NOTE }

    TaskType type;
    int indexOrId; // для учебных - индекс в списке Hometasks, для личных - id в БД, для заметок - индекс в списке Notes
    String title;
    String dueDate;
    String description;
    boolean isCompleted;
    String scheduleName; // Имя расписания для учебных заданий и заметок
    String scheduleAuthor; // Автор расписания для учебных заданий и заметок
    java.time.LocalDate endpointDate; // Дата для сортировки

    public HometaskItem(TaskType type, int indexOrId, String title, String dueDate, String description, boolean isCompleted) {
        this.type = type;
        this.indexOrId = indexOrId;
        this.title = title;
        this.dueDate = dueDate;
        this.description = description;
        this.isCompleted = isCompleted;
    }
    
    public HometaskItem(TaskType type, int indexOrId, String title, String dueDate, String description, boolean isCompleted, String scheduleName, String scheduleAuthor) {
        this.type = type;
        this.indexOrId = indexOrId;
        this.title = title;
        this.dueDate = dueDate;
        this.description = description;
        this.isCompleted = isCompleted;
        this.scheduleName = scheduleName;
        this.scheduleAuthor = scheduleAuthor;
    }
    
    public HometaskItem(TaskType type, int indexOrId, String title, String dueDate, String description, boolean isCompleted, String scheduleName, String scheduleAuthor, java.time.LocalDate endpointDate) {
        this.type = type;
        this.indexOrId = indexOrId;
        this.title = title;
        this.dueDate = dueDate;
        this.description = description;
        this.isCompleted = isCompleted;
        this.scheduleName = scheduleName;
        this.scheduleAuthor = scheduleAuthor;
        this.endpointDate = endpointDate;
    }
}

// --- Адаптер для RecyclerView ---
class HometaskAdapter extends RecyclerView.Adapter<HometaskAdapter.HometaskViewHolder> {

    private List<HometaskItem> items;
    private Context context;

    interface OnHometaskActionListener {
        void onToggleComplete(HometaskItem item);
        void onEdit(HometaskItem item);
        void onDelete(HometaskItem item);
    }

    private OnHometaskActionListener actionListener;

    public HometaskAdapter(List<HometaskItem> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public void setOnHometaskActionListener(OnHometaskActionListener listener) {
        this.actionListener = listener;
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
        holder.tvTaskDescription.setText(currentItem.description);
        
        // Для заметок не показываем дату выполнения
        if (currentItem.type == HometaskItem.TaskType.NOTE) {
            holder.tvTaskDueDate.setVisibility(View.GONE);
            holder.btnTaskComplete.setVisibility(View.GONE);
        } else {
            holder.tvTaskDueDate.setVisibility(View.VISIBLE);
            holder.tvTaskDueDate.setText(currentItem.dueDate);
            holder.btnTaskComplete.setVisibility(View.VISIBLE);
            
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
                if (actionListener != null) {
                    actionListener.onToggleComplete(currentItem);
                }
            });
        }

        holder.btnTaskEdit.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onEdit(currentItem);
            }
        });

        holder.btnTaskDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(currentItem);
            }
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
    private List<HometaskItem> notes;

    private SchedulelController schedulelController;
    private UserController userController;
    private int currentMode = 0; // 0 = учебные, 1 = личные, 2 = заметки

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MySingleton singleton = MySingleton.getInstance(context.getApplicationContext());
        schedulelController = singleton.getSchedulelController();
        userController = singleton.getUserController();
    }

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
        setupSearch();
    }
    
    private void setupSearch() {
        if (binding.etSearch != null) {
            binding.etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTasks(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }
    
    private void filterTasks(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Показываем все элементы
            List<HometaskItem> sourceList;
            if (currentMode == 0) {
                sourceList = studyTasks;
            } else if (currentMode == 1) {
                sourceList = personalTasks;
            } else {
                sourceList = notes;
            }
            hometaskAdapter.updateData(sourceList);
            return;
        }
        
        String lowerQuery = query.toLowerCase().trim();
        List<HometaskItem> filtered = new ArrayList<>();
        List<HometaskItem> sourceList;
        if (currentMode == 0) {
            sourceList = studyTasks;
        } else if (currentMode == 1) {
            sourceList = personalTasks;
        } else {
            sourceList = notes;
        }
        
        for (HometaskItem item : sourceList) {
            if ((item.title != null && item.title.toLowerCase().contains(lowerQuery)) ||
                (item.description != null && item.description.toLowerCase().contains(lowerQuery)) ||
                (item.dueDate != null && item.dueDate.toLowerCase().contains(lowerQuery))) {
                filtered.add(item);
            }
        }
        
        hometaskAdapter.updateData(filtered);
    }

    private void prepareDataLists() {
        studyTasks = new ArrayList<>();
        personalTasks = new ArrayList<>();
        notes = new ArrayList<>();

        // Учебные задания из всех выбранных расписаний
        List<Schedules> selectedSchedules = schedulelController.getSelectedSchedulesObjects();
        if (selectedSchedules.isEmpty()) {
            Schedules currentSchedule = schedulelController.getCurrentSchedule();
            if (currentSchedule != null) {
                selectedSchedules.add(currentSchedule);
            }
        }
        
        for (Schedules schedule : selectedSchedules) {
            if (schedule == null) continue;
            List<Schedules.Hometask> hometasks = schedule.getHometasks();
            if (hometasks == null) continue;
            for (int i = 0; i < hometasks.size(); i++) {
                Schedules.Hometask ht = hometasks.get(i);
                if (ht != null && !ht.isPersonal()) { // только учебные
                    String title = ht.getLesson();
                    String due = ht.getEndpoint() != null ? "Конечная дата: " + ht.getEndpoint().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
                    String desc = ht.getTask();
                    studyTasks.add(new HometaskItem(HometaskItem.TaskType.STUDY, i, title, due, desc, ht.isDone(), schedule.getName(), schedule.getAuthor(), ht.getEndpoint()));
                }
            }
            
            // Заметки из расписания
            List<Schedules.Note> scheduleNotes = schedule.getNotes();
            if (scheduleNotes != null) {
                for (int i = 0; i < scheduleNotes.size(); i++) {
                    Schedules.Note note = scheduleNotes.get(i);
                    if (note != null) {
                        String title = note.getLesson();
                        String desc = note.getData();
                        notes.add(new HometaskItem(HometaskItem.TaskType.NOTE, i, title, "", desc, false, schedule.getName(), schedule.getAuthor()));
                    }
                }
            }
        }

        // Личные задания из UserTask (UserController)
        List<UserTask> tasks = userController.getAllTasks();
        for (UserTask t : tasks) {
            String title = t.getName();
            String due = t.getEndpoint() != null ? "Конечная дата: " + t.getEndpoint().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
            String desc = t.getTask();
            personalTasks.add(new HometaskItem(HometaskItem.TaskType.PERSONAL, t.getId(), title, due, desc, t.isDone(), null, null, t.getEndpoint()));
        }
        
        // Сортируем по дате: от новых к старым
        studyTasks.sort((t1, t2) -> {
            if (t1.endpointDate == null && t2.endpointDate == null) return 0;
            if (t1.endpointDate == null) return 1; // null в конец
            if (t2.endpointDate == null) return -1; // null в конец
            return t2.endpointDate.compareTo(t1.endpointDate); // от новых к старым
        });
        
        personalTasks.sort((t1, t2) -> {
            if (t1.endpointDate == null && t2.endpointDate == null) return 0;
            if (t1.endpointDate == null) return 1; // null в конец
            if (t2.endpointDate == null) return -1; // null в конец
            return t2.endpointDate.compareTo(t1.endpointDate); // от новых к старым
        });
        
        // Для заметок сортируем по дате создания (используем индекс как дату создания)
        notes.sort((n1, n2) -> Integer.compare(n2.indexOrId, n1.indexOrId)); // от новых к старым
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.rvHometasks;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hometaskAdapter = new HometaskAdapter(new ArrayList<>(studyTasks), getContext());
        hometaskAdapter.setOnHometaskActionListener(new HometaskAdapter.OnHometaskActionListener() {
            @Override
            public void onToggleComplete(HometaskItem item) {
                if (item.type == HometaskItem.TaskType.STUDY) {
                    // Находим расписание с этим заданием
                    List<Schedules> selectedSchedules = schedulelController.getSelectedSchedulesObjects();
                    if (selectedSchedules.isEmpty()) {
                        Schedules current = schedulelController.getCurrentSchedule();
                        if (current != null) selectedSchedules.add(current);
                    }
                    
                    // Используем сохраненную информацию о расписании
                    if (item.scheduleName != null && item.scheduleAuthor != null) {
                        String authorNameString = item.scheduleAuthor + "-" + item.scheduleName;
                        schedulelController.loadCurrentSchedule(authorNameString);
                        Schedules updated = schedulelController.getCurrentSchedule();
                        if (updated != null) {
                            List<Schedules.Hometask> updatedList = updated.getHometasks();
                            if (updatedList != null) {
                                // Находим задание по индексу и дополнительно проверяем по комбинации полей для надежности
                                int targetIndex = item.indexOrId;
                                if (targetIndex >= 0 && targetIndex < updatedList.size()) {
                                    // Сначала пробуем найти по индексу (если список не изменился)
                                    Schedules.Hometask targetHt = null;
                                    int foundIndex = -1;
                                    
                                    // Ищем задание, которое соответствует item по комбинации полей
                                    for (int idx = 0; idx < updatedList.size(); idx++) {
                                        Schedules.Hometask ht = updatedList.get(idx);
                                        if (ht != null && !ht.isPersonal() &&
                                            ht.getLesson() != null && ht.getLesson().equals(item.title) &&
                                            ht.getTask() != null && ht.getTask().equals(item.description) &&
                                            ((ht.getEndpoint() == null && item.endpointDate == null) ||
                                             (ht.getEndpoint() != null && item.endpointDate != null && ht.getEndpoint().equals(item.endpointDate)))) {
                                            targetHt = ht;
                                            foundIndex = idx;
                                            break;
                                        }
                                    }
                                    
                                    if (targetHt != null) {
                                        targetHt.setDone(!targetHt.isDone());
                                        item.isCompleted = targetHt.isDone();
                                        schedulelController.seteditableSchedule(updated);
                                        schedulelController.saveEditableScheduleToDB();
                                        Toast.makeText(getContext(), targetHt.isDone() ? "Задание выполнено" : "Задание не выполнено", Toast.LENGTH_SHORT).show();
                                        // Обновляем данные и адаптер
                                        prepareDataLists();
                                        updateAdapterData();
                                        // Уведомляем об изменении для обновления главной страницы
                                        schedulelController.notifyAction("HometaskUpdated");
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (item.type == HometaskItem.TaskType.PERSONAL) {
                    userController.toggleTaskDone(item.indexOrId);
                    Toast.makeText(getContext(), "Статус личного задания изменен", Toast.LENGTH_SHORT).show();
                    prepareDataLists();
                    updateAdapterData();
                }
                // Для заметок нет переключения выполнения
            }

            @Override
            public void onEdit(HometaskItem item) {
                if (item.type == HometaskItem.TaskType.NOTE) {
                    // Редактирование заметки
                    Bundle args = new Bundle();
                    args.putString("mode", "note");
                    args.putInt("noteIndex", item.indexOrId);
                    args.putString("subject", item.title);
                    args.putString("data", item.description);
                    args.putString("scheduleName", item.scheduleName);
                    args.putString("scheduleAuthor", item.scheduleAuthor);
                    NavHostFragment.findNavController(HometaskRedactorFragment.this)
                            .navigate(R.id.nav_add_lecture, args);
                } else {
                    Bundle args = new Bundle();
                    args.putString("mode", item.type == HometaskItem.TaskType.STUDY ? "study" : "personal");
                    args.putInt("taskId", item.indexOrId);
                    if (item.scheduleName != null && item.scheduleAuthor != null) {
                        args.putString("scheduleName", item.scheduleName);
                        args.putString("scheduleAuthor", item.scheduleAuthor);
                    }
                    NavHostFragment.findNavController(HometaskRedactorFragment.this)
                            .navigate(R.id.nav_add_hometask, args);
                }
            }

            @Override
            public void onDelete(HometaskItem item) {
                if (item.type == HometaskItem.TaskType.STUDY) {
                    // Используем сохраненную информацию о расписании
                    if (item.scheduleName != null && item.scheduleAuthor != null) {
                        String authorNameString = item.scheduleAuthor + "-" + item.scheduleName;
                        schedulelController.loadCurrentSchedule(authorNameString);
                        Schedules updated = schedulelController.getCurrentSchedule();
                        if (updated != null) {
                            List<Schedules.Hometask> updatedList = updated.getHometasks();
                            if (updatedList != null) {
                                // Ищем задание по комбинации полей для точной идентификации
                                for (int j = updatedList.size() - 1; j >= 0; j--) {
                                    Schedules.Hometask updatedHt = updatedList.get(j);
                                    if (updatedHt != null && !updatedHt.isPersonal() &&
                                        updatedHt.getLesson() != null && updatedHt.getLesson().equals(item.title) &&
                                        updatedHt.getTask() != null && updatedHt.getTask().equals(item.description) &&
                                        ((updatedHt.getEndpoint() == null && item.endpointDate == null) ||
                                         (updatedHt.getEndpoint() != null && item.endpointDate != null && updatedHt.getEndpoint().equals(item.endpointDate)))) {
                                        updatedList.remove(j);
                                        schedulelController.seteditableSchedule(updated);
                                        schedulelController.saveEditableScheduleToDB();
                                        Toast.makeText(getContext(), "Учебное задание удалено", Toast.LENGTH_SHORT).show();
                                        prepareDataLists();
                                        updateAdapterData();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (item.type == HometaskItem.TaskType.PERSONAL) {
                    userController.deleteTask(item.indexOrId);
                    Toast.makeText(getContext(), "Личное задание удалено", Toast.LENGTH_SHORT).show();
                    prepareDataLists();
                    updateAdapterData();
                } else if (item.type == HometaskItem.TaskType.NOTE) {
                    // Удаление заметки - используем сохраненную информацию о расписании
                    if (item.scheduleName != null && item.scheduleAuthor != null) {
                        String authorNameString = item.scheduleAuthor + "-" + item.scheduleName;
                        schedulelController.loadCurrentSchedule(authorNameString);
                        Schedules updated = schedulelController.getCurrentSchedule();
                        if (updated != null) {
                            List<Schedules.Note> updatedList = updated.getNotes();
                            if (updatedList != null) {
                                for (int j = 0; j < updatedList.size(); j++) {
                                    Schedules.Note updatedNote = updatedList.get(j);
                                    if (updatedNote != null && updatedNote.getLesson() != null && 
                                        updatedNote.getLesson().equals(item.title)) {
                                        updatedList.remove(j);
                                        schedulelController.seteditableSchedule(updated);
                                        schedulelController.saveEditableScheduleToDB();
                                        Toast.makeText(getContext(), "Заметка удалена", Toast.LENGTH_SHORT).show();
                                        prepareDataLists();
                                        updateAdapterData();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        recyclerView.setAdapter(hometaskAdapter);
    }

    private void setupClickListeners() {
        binding.cardStudyTasks.setOnClickListener(v -> {
            currentMode = 0;
            binding.cardStudyTasks.setStrokeWidth(dpToPx(1));
            binding.cardPersonalTasks.setStrokeWidth(0);
            binding.cardNotes.setStrokeWidth(0);
            binding.tvListHeader.setText("Учебные задания");
            binding.btnAddHometask.setText("Добавить задание");
            prepareDataLists();
            updateAdapterData();
        });

        binding.cardPersonalTasks.setOnClickListener(v -> {
            currentMode = 1;
            binding.cardPersonalTasks.setStrokeWidth(dpToPx(1));
            binding.cardStudyTasks.setStrokeWidth(0);
            binding.cardNotes.setStrokeWidth(0);
            binding.tvListHeader.setText("Личные задания");
            binding.btnAddHometask.setText("Добавить задание");
            prepareDataLists();
            updateAdapterData();
        });

        binding.cardNotes.setOnClickListener(v -> {
            currentMode = 2;
            binding.cardNotes.setStrokeWidth(dpToPx(1));
            binding.cardStudyTasks.setStrokeWidth(0);
            binding.cardPersonalTasks.setStrokeWidth(0);
            binding.tvListHeader.setText("Заметки");
            binding.btnAddHometask.setText("Добавить заметку");
            prepareDataLists();
            updateAdapterData();
        });

        binding.btnAddHometask.setOnClickListener(v -> {
            if (currentMode == 2) {
                // Добавление заметки
                Bundle args = new Bundle();
                args.putString("mode", "note");
                args.putInt("noteIndex", -1);
                NavHostFragment.findNavController(HometaskRedactorFragment.this)
                        .navigate(R.id.nav_add_lecture, args);
            } else {
                Bundle args = new Bundle();
                args.putString("mode", currentMode == 0 ? "study" : "personal");
                args.putInt("taskId", -1);
                NavHostFragment.findNavController(HometaskRedactorFragment.this)
                        .navigate(R.id.nav_add_hometask, args);
            }
        });
    }
    
    private void updateAdapterData() {
        List<HometaskItem> sourceList;
        if (currentMode == 0) {
            sourceList = studyTasks;
        } else if (currentMode == 1) {
            sourceList = personalTasks;
        } else {
            sourceList = notes;
        }
        hometaskAdapter.updateData(sourceList);
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
