package com.AS.Yuppi.Raspi.ui.home;import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.AS.Yuppi.Raspi.R;
import java.util.List;

/**
 * Класс-модель для занятия.
 * Добавлен Enum для удобного управления типами занятий.
 */
class ScheduleLesson {
    enum LessonType {
        LECTURE("ЛЕК"),
        LAB("ЛАБ"),
        SEMINAR("СЕМ"),
        PERSONAL("ЛИЧ"),
        TASK("ДЗ");

        private final String shortName;
        LessonType(String shortName) {
            this.shortName = shortName;
        }
        public String getShortName() {
            return shortName;
        }
    }

    boolean isHeader = false;
    String headerText;
    LessonType type;
    String number, time, subjectName, teacherName, classroom;
    // Задания для этого предмета (если крайний срок совпадает с днем показа)
    List<HometaskInfo> hometasks;
    // Личное мероприятие (если это личное мероприятие)
    PersonalEventInfo personalEvent;

    // Constructor for header
    public ScheduleLesson(String headerText) {
        this.isHeader = true;
        this.headerText = headerText;
    }

    // Constructor for lesson
    public ScheduleLesson(LessonType type, String number, String time, String subjectName, String teacherName, String classroom) {
        this.isHeader = false;
        this.type = type;
        this.number = number;
        this.time = time;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.classroom = classroom;
        this.hometasks = new java.util.ArrayList<>();
        this.personalEvent = null;
    }

    // Constructor for personal event
    public ScheduleLesson(String time, String eventName, String eventInfo) {
        this.isHeader = false;
        this.type = LessonType.PERSONAL;
        this.number = "";
        this.time = time;
        this.subjectName = eventName;
        this.teacherName = "";
        this.classroom = "";
        this.hometasks = new java.util.ArrayList<>();
        this.personalEvent = new PersonalEventInfo(eventName, eventInfo);
    }

    /**
     * Информация о задании для отображения в ячейке предмета.
     */
    public static class HometaskInfo {
        public String task;
        public boolean isDone;
        public String subject; // Предмет, к которому относится задание

        public HometaskInfo(String task, boolean isDone, String subject) {
            this.task = task;
            this.isDone = isDone;
            this.subject = subject;
        }
    }

    /**
     * Информация о личном мероприятии.
     */
    public static class PersonalEventInfo {
        public String name;
        public String info;

        public PersonalEventInfo(String name, String info) {
            this.name = name;
            this.info = info;
        }
    }
}


public class HomeScheduleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ScheduleLesson> lessonList;
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_LESSON = 1;

    public HomeScheduleAdapter(List<ScheduleLesson> lessonList) {
        this.lessonList = lessonList;
    }

    @Override
    public int getItemViewType(int position) {
        return lessonList.get(position).isHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_LESSON;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            // Create a simple TextView for header
            TextView headerView = new TextView(parent.getContext());
            headerView.setPadding(16, 16, 16, 8);
            headerView.setTextSize(18);
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setTextColor(ContextCompat.getColor(parent.getContext(), android.R.color.black));
            return new HeaderViewHolder(headerView);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new LessonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ScheduleLesson lesson = lessonList.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).textView.setText(lesson.headerText);
        } else if (holder instanceof LessonViewHolder) {
            LessonViewHolder lessonHolder = (LessonViewHolder) holder;
            // Устанавливаем текст и цвет кружка
            lessonHolder.tvClassType.setText(lesson.type.getShortName());
            lessonHolder.setClassTypeColor(lesson.type);

            // Заполняем остальные поля
            if (lesson.number != null && !lesson.number.isEmpty()) {
                lessonHolder.tvClassNumber.setText(lesson.number);
                lessonHolder.tvClassNumber.setVisibility(View.VISIBLE);
            } else {
                lessonHolder.tvClassNumber.setVisibility(View.GONE);
            }
            lessonHolder.tvClassTime.setText(lesson.time);
            lessonHolder.tvSubjectName.setText(lesson.subjectName);
            
            // Для личных мероприятий и заданий не показываем преподавателя и аудиторию
            if (lesson.type == ScheduleLesson.LessonType.PERSONAL || lesson.type == ScheduleLesson.LessonType.TASK) {
                lessonHolder.tvTeacherName.setVisibility(View.GONE);
                lessonHolder.tvClassroom.setVisibility(View.GONE);
                // Для заданий также скрываем время
                if (lesson.type == ScheduleLesson.LessonType.TASK) {
                    lessonHolder.tvClassTime.setVisibility(View.GONE);
                } else {
                    lessonHolder.tvClassTime.setVisibility(View.VISIBLE);
                }
            } else {
                lessonHolder.tvTeacherName.setText(lesson.teacherName);
                lessonHolder.tvClassroom.setText(lesson.classroom);
                lessonHolder.tvTeacherName.setVisibility(View.VISIBLE);
                lessonHolder.tvClassroom.setVisibility(View.VISIBLE);
            }
            
            // Отображаем задания под названием предмета
            if (lesson.hometasks != null && !lesson.hometasks.isEmpty()) {
                StringBuilder tasksText = new StringBuilder();
                for (ScheduleLesson.HometaskInfo task : lesson.hometasks) {
                    if (tasksText.length() > 0) {
                        tasksText.append("\n");
                    }
                    String status = task.isDone ? "✓" : "○";
                    tasksText.append(status).append(" ").append(task.task);
                }
                lessonHolder.tvClassDetail.setText(tasksText.toString());
                lessonHolder.tvClassDetail.setVisibility(View.VISIBLE);
            } else if (lesson.personalEvent != null && lesson.personalEvent.info != null && !lesson.personalEvent.info.isEmpty()) {
                // Для личных мероприятий показываем описание
                lessonHolder.tvClassDetail.setText(lesson.personalEvent.info);
                lessonHolder.tvClassDetail.setVisibility(View.VISIBLE);
            } else {
                lessonHolder.tvClassDetail.setVisibility(View.GONE);
            }
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        HeaderViewHolder(TextView itemView) {
            super(itemView);
            this.textView = itemView;
        }
    }
    public void updateLessons(List<ScheduleLesson> newLessons) {
        lessonList.clear();
        lessonList.addAll(newLessons);
        notifyDataSetChanged(); // Сообщаем RecyclerView, что данные полностью изменились
    }

    @Override
    public int getItemCount() {
        return lessonList.size();
    }

    /**
     * ViewHolder теперь содержит логику для смены цвета.
     */
    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassType, tvClassNumber, tvClassTime;
        TextView tvSubjectName, tvClassDetail;
        TextView tvTeacherName, tvClassroom;
        Context context; // Храним контекст для доступа к цветам

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext(); // Получаем контекст
            tvClassType = itemView.findViewById(R.id.tv_class_type);
            tvClassNumber = itemView.findViewById(R.id.tv_class_number);
            tvClassTime = itemView.findViewById(R.id.tv_class_time);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvClassDetail = itemView.findViewById(R.id.tv_class_detail);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name);
            tvClassroom = itemView.findViewById(R.id.tv_classroom);
        }


        /**
         * Новый метод для установки цвета кружка в зависимости от типа занятия.
         * @param type Тип занятия (ЛЕК, ЛАБ, СЕМ)
         */
        public void setClassTypeColor(ScheduleLesson.LessonType type) {
            int colorRes;
            switch (type) {
                case LECTURE:
                    // Желтый (Я использую стандартный цвет, замените на свой, если нужно)
                    colorRes = R.color.lesson_type_lecture;
                    break;
                case LAB:
                    // Бирюзовый
                    colorRes = R.color.lesson_type_lab;
                    break;
                case SEMINAR:
                    // Зеленый
                    colorRes = R.color.lesson_type_seminar;
                    break;
                case PERSONAL:
                    // Фиолетовый для личных мероприятий
                    colorRes = R.color.lesson_type_personal;
                    break;
                case TASK:
                    // Фиолетовый для заданий (как у PERSONAL)
                    colorRes = R.color.lesson_type_personal;
                    break;
                default:
                    // Цвет по умолчанию, если что-то пойдет не так
                    colorRes = R.color.lesson_type_default;
                    break;
            }

            // Получаем Drawable фона и меняем его цвет
            GradientDrawable background = (GradientDrawable) tvClassType.getBackground();
            background.setColor(ContextCompat.getColor(context, colorRes));
        }
    }
}
