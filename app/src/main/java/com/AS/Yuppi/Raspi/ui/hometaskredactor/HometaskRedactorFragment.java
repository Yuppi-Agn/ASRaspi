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
    enum TaskType { STUDY, PERSONAL }

    TaskType type;
    int indexOrId; // для учебных - индекс в списке Hometasks, для личных - id в БД
    String title;
    String dueDate;
    String description;
    boolean isCompleted;

    public HometaskItem(TaskType type, int indexOrId, String title, String dueDate, String description, boolean isCompleted) {
        this.type = type;
        this.indexOrId = indexOrId;
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
            if (actionListener != null) {
                actionListener.onToggleComplete(currentItem);
            }
        });

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

    private SchedulelController schedulelController;
    private UserController userController;
    private boolean isStudyMode = true;

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
    }

    private void prepareDataLists() {
        studyTasks = new ArrayList<>();
        personalTasks = new ArrayList<>();

        // Учебные задания из текущего расписания
        Schedules currentSchedule = schedulelController.getCurrentSchedule();
        if (currentSchedule != null) {
            List<Schedules.Hometask> hometasks = currentSchedule.getHometasks();
            for (int i = 0; i < hometasks.size(); i++) {
                Schedules.Hometask ht = hometasks.get(i);
                if (!ht.isPersonal()) { // только учебные
                    String title = ht.getLesson();
                    String due = ht.getEndpoint() != null ? "Конечная дата: " + ht.getEndpoint().toString() : "";
                    String desc = ht.getTask();
                    studyTasks.add(new HometaskItem(HometaskItem.TaskType.STUDY, i, title, due, desc, ht.isDone()));
                }
            }
        }

        // Личные задания из UserTask (UserController)
        List<UserTask> tasks = userController.getAllTasks();
        for (UserTask t : tasks) {
            String title = t.getName();
            String due = t.getEndpoint() != null ? "Конечная дата: " + t.getEndpoint().toString() : "";
            String desc = t.getTask();
            personalTasks.add(new HometaskItem(HometaskItem.TaskType.PERSONAL, t.getId(), title, due, desc, t.isDone()));
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.rvHometasks;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hometaskAdapter = new HometaskAdapter(new ArrayList<>(studyTasks), getContext());
        hometaskAdapter.setOnHometaskActionListener(new HometaskAdapter.OnHometaskActionListener() {
            @Override
            public void onToggleComplete(HometaskItem item) {
                if (item.type == HometaskItem.TaskType.STUDY) {
                    Schedules current = schedulelController.getCurrentSchedule();
                    if (current == null) return;
                    List<Schedules.Hometask> list = current.getHometasks();
                    if (item.indexOrId >= 0 && item.indexOrId < list.size()) {
                        Schedules.Hometask ht = list.get(item.indexOrId);
                        ht.setDone(!ht.isDone());
                        item.isCompleted = ht.isDone();
                        // сохраняем изменения расписания
                        schedulelController.seteditableSchedule(current);
                        schedulelController.saveEditableSchedule();
                        Toast.makeText(getContext(), ht.isDone() ? "Задание выполнено" : "Задание не выполнено", Toast.LENGTH_SHORT).show();
                        prepareDataLists();
                        hometaskAdapter.updateData(isStudyMode ? studyTasks : personalTasks);
                    }
                } else {
                    userController.toggleTaskDone(item.indexOrId);
                    Toast.makeText(getContext(), "Статус личного задания изменен", Toast.LENGTH_SHORT).show();
                    prepareDataLists();
                    hometaskAdapter.updateData(isStudyMode ? studyTasks : personalTasks);
                }
            }

            @Override
            public void onEdit(HometaskItem item) {
                Bundle args = new Bundle();
                args.putString("mode", item.type == HometaskItem.TaskType.STUDY ? "study" : "personal");
                args.putInt("taskId", item.indexOrId);
                NavHostFragment.findNavController(HometaskRedactorFragment.this)
                        .navigate(R.id.nav_add_hometask, args);
            }

            @Override
            public void onDelete(HometaskItem item) {
                if (item.type == HometaskItem.TaskType.STUDY) {
                    Schedules current = schedulelController.getCurrentSchedule();
                    if (current == null) return;
                    List<Schedules.Hometask> list = current.getHometasks();
                    if (item.indexOrId >= 0 && item.indexOrId < list.size()) {
                        list.remove(item.indexOrId);
                        schedulelController.seteditableSchedule(current);
                        schedulelController.saveEditableSchedule();
                        Toast.makeText(getContext(), "Учебное задание удалено", Toast.LENGTH_SHORT).show();
                        prepareDataLists();
                        hometaskAdapter.updateData(isStudyMode ? studyTasks : personalTasks);
                    }
                } else {
                    userController.deleteTask(item.indexOrId);
                    Toast.makeText(getContext(), "Личное задание удалено", Toast.LENGTH_SHORT).show();
                    prepareDataLists();
                    hometaskAdapter.updateData(isStudyMode ? studyTasks : personalTasks);
                }
            }
        });
        recyclerView.setAdapter(hometaskAdapter);
    }

    private void setupClickListeners() {
        binding.cardStudyTasks.setOnClickListener(v -> {
            isStudyMode = true;
            binding.cardStudyTasks.setStrokeWidth(dpToPx(1));
            binding.cardPersonalTasks.setStrokeWidth(0);
            binding.tvListHeader.setText("Учебные задания");
            prepareDataLists();
            hometaskAdapter.updateData(studyTasks);
        });

        binding.cardPersonalTasks.setOnClickListener(v -> {
            isStudyMode = false;
            binding.cardPersonalTasks.setStrokeWidth(dpToPx(1));
            binding.cardStudyTasks.setStrokeWidth(0);
            binding.tvListHeader.setText("Личные задания");
            prepareDataLists();
            hometaskAdapter.updateData(personalTasks);
        });

        binding.btnAddHometask.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("mode", isStudyMode ? "study" : "personal");
            args.putInt("taskId", -1);
            NavHostFragment.findNavController(HometaskRedactorFragment.this)
                    .navigate(R.id.nav_add_hometask, args);
        });

        // Перехватываем клики по кнопкам внутри элементов списка через tag и OnClickListener
        // Для этого понадобится доработать адаптер, но чтобы минимально изменять код,
        // будем использовать performClick через findViewById ниже в будущем шаге при необходимости.
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
