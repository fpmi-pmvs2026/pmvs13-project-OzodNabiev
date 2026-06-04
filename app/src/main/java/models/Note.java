package com.example.notesapp.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.Date;

@Entity(tableName = "notes")
public class Note implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private boolean hasReminder;
    private long reminderTime;

    public Note() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.hasReminder = false;
        this.reminderTime = 0;
    }

    // Геттеры
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public boolean isHasReminder() { return hasReminder; }
    public long getReminderTime() { return reminderTime; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public void setHasReminder(boolean hasReminder) { this.hasReminder = hasReminder; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
}