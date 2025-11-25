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

    LessonType type;
    String number, time, subjectName, teacherName, classroom;

    public ScheduleLesson(LessonType type, String number, String time, String subjectName, String teacherName, String classroom) {
        this.type = type;
        this.number = number;
        this.time = time;
        this.subjectName = subjectName;
        this.teacherName = teacherName;
        this.classroom = classroom;
    }
}


public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.LessonViewHolder> {

    private final List<ScheduleLesson> lessonList;

    public HomeScheduleAdapter(List<ScheduleLesson> lessonList) {
        this.lessonList = lessonList;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        ScheduleLesson lesson = lessonList.get(position);

        // Устанавливаем текст и цвет кружка
        holder.tvClassType.setText(lesson.type.getShortName());
        holder.setClassTypeColor(lesson.type); // <--- ВЫЗЫВАЕМ НОВЫЙ МЕТОД

        // Заполняем остальные поля
        holder.tvClassNumber.setText(lesson.number);
        holder.tvClassTime.setText(lesson.time);
        holder.tvSubjectName.setText(lesson.subjectName);
        holder.tvTeacherName.setText(lesson.teacherName);
        holder.tvClassroom.setText(lesson.classroom);
        holder.tvClassDetail.setVisibility(View.GONE);
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
