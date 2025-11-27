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
    private int[] cachedWidths; // Кэш для ширин карточек
    private int currentWeekHash; // Хэш текущей недели для проверки необходимости пересчета

    public WeekDayAdapter(List<WeekDayData> weekDays) {
        this.weekDaysList = new ArrayList<>(weekDays);
        this.cachedWidths = new int[weekDays.size()];
        this.currentWeekHash = calculateWeekHash(weekDays);
    }

    public void updateWeekDays(List<WeekDayData> newWeekDays) {
        int newWeekHash = calculateWeekHash(newWeekDays);
        
        // Если неделя изменилась, очищаем кэш и пересчитываем размеры
        if (newWeekHash != currentWeekHash) {
            this.cachedWidths = new int[newWeekDays.size()];
            this.currentWeekHash = newWeekHash;
        }
        
        this.weekDaysList.clear();
        this.weekDaysList.addAll(newWeekDays);
        
        // Если размер массива изменился, пересоздаем кэш
        if (cachedWidths.length != newWeekDays.size()) {
            this.cachedWidths = new int[newWeekDays.size()];
        }
        
        notifyDataSetChanged();
    }
    
    private int calculateWeekHash(List<WeekDayData> weekDays) {
        // Создаем хэш на основе дат недели для определения, нужно ли пересчитывать размеры
        if (weekDays == null || weekDays.isEmpty()) {
            return 0;
        }
        int hash = 0;
        for (WeekDayData day : weekDays) {
            if (day != null && day.date != null) {
                hash = 31 * hash + day.date.hashCode();
            }
        }
        return hash;
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
        
        // Сбрасываем LayoutParams перед установкой новых, чтобы избежать конфликтов при переиспользовании
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            holder.itemView.setLayoutParams(params);
        } else {
            // Сбрасываем и ширину, и высоту для правильного переиспользования
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(params);
        }
        
        // Сбрасываем LayoutParams для внутреннего RecyclerView, чтобы избежать проблем с высотой
        ViewGroup.LayoutParams rvParams = holder.rvDayLessons.getLayoutParams();
        if (rvParams != null) {
            rvParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.rvDayLessons.setLayoutParams(rvParams);
        }
        
        // Устанавливаем заголовок дня
        String dateLabel = dayData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        String relativeLabel = getRelativeDateString(dayData.date);
        holder.tvDayHeader.setText(dateLabel + "\n" + relativeLabel);
        
        // Настраиваем RecyclerView для занятий этого дня
        // Важно: создаем новый LayoutManager для каждого ViewHolder, чтобы избежать проблем при переиспользовании
        holder.rvDayLessons.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        WeekScheduleAdapter dayAdapter = new WeekScheduleAdapter(dayData.lessons);
        holder.rvDayLessons.setAdapter(dayAdapter);
        
        // Принудительно запрашиваем перерисовку RecyclerView для правильного измерения высоты
        holder.rvDayLessons.requestLayout();
        
        // Используем кэшированную ширину, если она есть, иначе вычисляем
        int cachedWidth = (position < cachedWidths.length) ? cachedWidths[position] : 0;
        
        if (cachedWidth > 0) {
            // Используем кэшированную ширину
            ViewGroup.LayoutParams Thisparams = holder.itemView.getLayoutParams();
            if (Thisparams != null) {
                Thisparams.width = cachedWidth;
                Thisparams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                holder.itemView.setLayoutParams(Thisparams);
            }
        } else {
            // Вычисляем ширину только если она не кэширована
            holder.itemView.post(() -> {
                // Проверяем, что ViewHolder все еще привязан к той же позиции
                if (holder.getAdapterPosition() != position || position >= cachedWidths.length) {
                    return;
                }
                
                float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
                int padding = (int) (16 * density); // padding карточки (8dp * 2)
                int maxWidth = 0;
                
                // Измеряем ширину заголовка (может быть многострочным)
                holder.tvDayHeader.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                maxWidth = Math.max(maxWidth, holder.tvDayHeader.getMeasuredWidth());
                
                // Измеряем ширину элементов занятий
                if (dayData.lessons != null && !dayData.lessons.isEmpty()) {
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
                
                // Устанавливаем ширину с ограничением максимум 300dp
                int maxWidthDp = (int) (300 * density);
                int finalWidth = Math.min(maxWidth + padding, maxWidthDp);
                
                // Минимальная ширина для читаемости
                int minWidthDp = (int) (180 * density);
                finalWidth = Math.max(finalWidth, minWidthDp);
                
                // Кэшируем ширину
                cachedWidths[position] = finalWidth;
                
                // Устанавливаем ширину, высота остается WRAP_CONTENT
                ViewGroup.LayoutParams finalParams = holder.itemView.getLayoutParams();
                if (finalParams != null) {
                    finalParams.width = finalWidth;
                    // Высота должна оставаться WRAP_CONTENT для правильного отображения
                    finalParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    holder.itemView.setLayoutParams(finalParams);
                }
                
                // Принудительно запрашиваем перерисовку для правильного измерения высоты
                holder.itemView.requestLayout();
            });
        }
    }

    @Override
    public int getItemCount() {
        return weekDaysList.size();
    }
    
    @Override
    public void onViewRecycled(@NonNull DayViewHolder holder) {
        super.onViewRecycled(holder);
        // Сбрасываем LayoutParams при переиспользовании ViewHolder'а
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.itemView.setLayoutParams(params);
        }
        
        // Сбрасываем LayoutParams для внутреннего RecyclerView
        ViewGroup.LayoutParams rvParams = holder.rvDayLessons.getLayoutParams();
        if (rvParams != null) {
            rvParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            holder.rvDayLessons.setLayoutParams(rvParams);
        }
        
        // Очищаем адаптер внутреннего RecyclerView
        holder.rvDayLessons.setAdapter(null);
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

