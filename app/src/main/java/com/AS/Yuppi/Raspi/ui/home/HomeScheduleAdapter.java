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
        SEMINAR("СЕМ");

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
            lessonHolder.tvClassNumber.setText(lesson.number);
            lessonHolder.tvClassTime.setText(lesson.time);
            lessonHolder.tvSubjectName.setText(lesson.subjectName);
            lessonHolder.tvTeacherName.setText(lesson.teacherName);
            lessonHolder.tvClassroom.setText(lesson.classroom);
            lessonHolder.tvClassDetail.setVisibility(View.GONE);
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
