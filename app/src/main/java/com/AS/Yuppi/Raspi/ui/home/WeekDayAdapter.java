package com.AS.Yuppi.Raspi.ui.home;

import android.graphics.Paint;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.AS.Yuppi.Raspi.R;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.DayViewHolder> {
    
    private String getRelativeDateString(LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, date);
        
        if (daysDiff == 0) return "Сегодня";
        if (daysDiff == 1) return "Завтра";
        if (daysDiff == -1) return "Вчера";
        if (daysDiff == 2) return "Через 2 дня";
        if (daysDiff == 3) return "Через 3 дня";
        if (daysDiff == 4) return "Через 4 дня";
        if (daysDiff == 5) return "Через 5 дней";
        if (daysDiff == 6) return "Через 6 дней";
        if (daysDiff == 7) return "Через неделю";
        if (daysDiff == -2) return "2 дня назад";
        if (daysDiff == -3) return "3 дня назад";
        if (daysDiff == -4) return "4 дня назад";
        if (daysDiff == -5) return "5 дней назад";
        if (daysDiff == -6) return "6 дней назад";
        if (daysDiff == -7) return "Неделю назад";
        return "";
        //return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private List<WeekDayData> weekDaysList;

    public WeekDayAdapter(List<WeekDayData> weekDays) {
        this.weekDaysList = new ArrayList<>(weekDays);
    }

    public void updateWeekDays(List<WeekDayData> newWeekDays) {
        this.weekDaysList.clear();
        this.weekDaysList.addAll(newWeekDays);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        WeekDayData dayData = weekDaysList.get(position);
        
        // Устанавливаем заголовок дня
        String dateLabel = dayData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String relativeLabel = getRelativeDateString(dayData.date);
        holder.tvDayHeader.setText(dateLabel + "\n" + relativeLabel);
        
        // Настраиваем RecyclerView для занятий этого дня
        holder.rvDayLessons.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        WeekScheduleAdapter dayAdapter = new WeekScheduleAdapter(dayData.lessons);
        holder.rvDayLessons.setAdapter(dayAdapter);
        
        // Измеряем и устанавливаем ширину карточки на основе реального контента
        holder.rvDayLessons.post(() -> {
            // Ждем, пока RecyclerView отрисует элементы
            holder.rvDayLessons.post(() -> {
                float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
                int padding = (int) (16 * density); // padding карточки (8dp * 2)
                int maxWidth = 0;
                
                // Измеряем ширину заголовка (может быть многострочным)
                holder.tvDayHeader.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                maxWidth = Math.max(maxWidth, holder.tvDayHeader.getMeasuredWidth());
                
                // Измеряем ширину элементов занятий после их отрисовки
                if (dayData.lessons != null && !dayData.lessons.isEmpty()) {
                    // Проверяем, есть ли ViewHolder'ы в RecyclerView
                    int childCount = holder.rvDayLessons.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = holder.rvDayLessons.getChildAt(i);
                        if (child != null) {
                            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                        }
                    }
                    
                    // Если элементы еще не отрисованы, используем приблизительный расчет
                    if (maxWidth == 0 || childCount == 0) {
                        TextPaint textPaint = new TextPaint();
                        textPaint.setTextSize(14 * density); // размер текста предмета
                        
                        for (ScheduleLesson lesson : dayData.lessons) {
                            int lessonWidth = 0;
                            
                            // Левый блок: иконка (32dp) + номер (~20dp) + время (~50dp) + отступы (16dp)
                            lessonWidth += (int) (118 * density);
                            
                            // Средний блок: название предмета (максимум 120dp по layout)
                            if (lesson.subjectName != null) {
                                float subjectWidth = textPaint.measureText(lesson.subjectName);
                                lessonWidth += Math.min((int) subjectWidth, (int) (120 * density));
                            } else {
                                lessonWidth += (int) (120 * density);
                            }
                            lessonWidth += (int) (8 * density); // margin
                            
                            // Правый блок: преподаватель + аудитория (~60dp)
                            if (lesson.teacherName != null) {
                                textPaint.setTextSize(12 * density);
                                float teacherWidth = textPaint.measureText(lesson.teacherName);
                                lessonWidth += Math.max((int) teacherWidth, (int) (60 * density));
                            } else {
                                lessonWidth += (int) (60 * density);
                            }
                            
                            maxWidth = Math.max(maxWidth, lessonWidth);
                        }
                    }
                }
                
                // Устанавливаем ширину с ограничением максимум 300dp
                int maxWidthDp = (int) (300 * density);
                int finalWidth = Math.min(maxWidth + padding, maxWidthDp);
                
                // Минимальная ширина для читаемости
                int minWidthDp = (int) (180 * density);
                finalWidth = Math.max(finalWidth, minWidthDp);
                
                ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                if (params != null) {
                    params.width = finalWidth;
                    holder.itemView.setLayoutParams(params);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return weekDaysList.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayHeader;
        RecyclerView rvDayLessons;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayHeader = itemView.findViewById(R.id.tv_day_header);
            rvDayLessons = itemView.findViewById(R.id.rv_day_lessons);
        }
    }

    public static class WeekDayData {
        LocalDate date;
        List<ScheduleLesson> lessons;

        public WeekDayData(LocalDate date, List<ScheduleLesson> lessons) {
            this.date = date;
            this.lessons = lessons;
        }
    }
}

