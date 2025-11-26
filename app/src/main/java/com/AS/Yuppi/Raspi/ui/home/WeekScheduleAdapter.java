package com.AS.Yuppi.Raspi.ui.home;

import android.content.Context;
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

public class WeekScheduleAdapter extends RecyclerView.Adapter<WeekScheduleAdapter.LessonViewHolder> {

    private final List<ScheduleLesson> lessonList;

    public WeekScheduleAdapter(List<ScheduleLesson> lessonList) {
        this.lessonList = lessonList;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_week, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        ScheduleLesson lesson = lessonList.get(position);

        // Устанавливаем текст и цвет кружка
        holder.tvClassType.setText(lesson.type.getShortName());
        holder.setClassTypeColor(lesson.type);

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
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return lessonList.size();
    }

    public static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassType, tvClassNumber, tvClassTime;
        TextView tvSubjectName, tvClassDetail;
        TextView tvTeacherName, tvClassroom;
        Context context;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            tvClassType = itemView.findViewById(R.id.tv_class_type);
            tvClassNumber = itemView.findViewById(R.id.tv_class_number);
            tvClassTime = itemView.findViewById(R.id.tv_class_time);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvClassDetail = itemView.findViewById(R.id.tv_class_detail);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name);
            tvClassroom = itemView.findViewById(R.id.tv_classroom);
        }

        public void setClassTypeColor(ScheduleLesson.LessonType type) {
            int colorRes;
            switch (type) {
                case LECTURE:
                    colorRes = R.color.lesson_type_lecture;
                    break;
                case LAB:
                    colorRes = R.color.lesson_type_lab;
                    break;
                case SEMINAR:
                    colorRes = R.color.lesson_type_seminar;
                    break;
                default:
                    colorRes = R.color.lesson_type_default;
                    break;
            }

            GradientDrawable background = (GradientDrawable) tvClassType.getBackground();
            background.setColor(ContextCompat.getColor(context, colorRes));
        }
    }
}

