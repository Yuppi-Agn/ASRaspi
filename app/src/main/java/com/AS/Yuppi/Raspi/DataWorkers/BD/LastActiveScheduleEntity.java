package com.AS.Yuppi.Raspi.DataWorkers.BD;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "last_active_schedule_table")
public class LastActiveScheduleEntity {
    @PrimaryKey
    private int id = 1;
    private String lastActiveScheduleAuthorName;
    public LastActiveScheduleEntity(String lastActiveScheduleAuthorName) {
        this.lastActiveScheduleAuthorName = lastActiveScheduleAuthorName;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getLastActiveScheduleAuthorName() { return lastActiveScheduleAuthorName; }
    public void setLastActiveScheduleAuthorName(String lastActiveScheduleAuthorName) { this.lastActiveScheduleAuthorName = lastActiveScheduleAuthorName; }
}