package com.example.notesapp.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.notesapp.R;
import com.example.notesapp.database.NoteDatabase;
import com.example.notesapp.models.Note;
import com.example.notesapp.services.NotificationReceiver;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteEditorFragment extends Fragment {

    private EditText etTitle, etContent;
    private Button btnSave, btnDelete, btnSetReminder;
    private TextView tvReminder;
    private Note note;
    private ExecutorService executorService;
    private long reminderTime = 0;

    public static NoteEditorFragment newInstance(Note note) {
        NoteEditorFragment fragment = new NoteEditorFragment();
        Bundle args = new Bundle();
        if (note != null) {
            args.putSerializable("note", note);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note_editor, container, false);

        executorService = Executors.newSingleThreadExecutor();

        etTitle = view.findViewById(R.id.etTitle);
        etContent = view.findViewById(R.id.etContent);
        btnSave = view.findViewById(R.id.btnSave);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnSetReminder = view.findViewById(R.id.btnSetReminder);
        tvReminder = view.findViewById(R.id.tvReminder);

        if (getArguments() != null && getArguments().containsKey("note")) {
            note = (Note) getArguments().getSerializable("note");
            if (note != null) {
                etTitle.setText(note.getTitle());
                etContent.setText(note.getContent());
                reminderTime = note.getReminderTime();
                if (reminderTime > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    tvReminder.setText("Напоминание: " + sdf.format(new Date(reminderTime)));
                    tvReminder.setVisibility(View.VISIBLE);
                }
            }
        }

        btnSave.setOnClickListener(v -> saveNote());
        btnDelete.setOnClickListener(v -> deleteNote());
        btnSetReminder.setOnClickListener(v -> showDateTimePicker());

        return view;
    }

    private void showDateTimePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setTitleText("Выберите время")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                reminderTime = calendar.getTimeInMillis();

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                tvReminder.setText("Напоминание: " + sdf.format(new Date(reminderTime)));
                tvReminder.setVisibility(View.VISIBLE);
            });

            timePicker.show(getParentFragmentManager(), "time_picker");
        });

        datePicker.show(getParentFragmentManager(), "date_picker");
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Введите заголовок", Toast.LENGTH_SHORT).show();
            return;
        }

        if (note == null) {
            note = new Note();
        }

        note.setTitle(title);
        note.setContent(content);
        note.setUpdatedAt(new Date());
        note.setHasReminder(reminderTime > 0);
        note.setReminderTime(reminderTime);

        executorService.execute(() -> {
            if (note.getId() == 0) {
                NoteDatabase.getInstance(getContext()).noteDao().insert(note);
            } else {
                NoteDatabase.getInstance(getContext()).noteDao().update(note);
            }

            if (reminderTime > 0) {
                scheduleNotification(note);
            }

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Заметка сохранена", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            });
        });
    }

    private void scheduleNotification(Note note) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", note.getTitle());
        intent.putExtra("content", note.getContent());
        intent.putExtra("noteId", note.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(), note.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (reminderTime > System.currentTimeMillis()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }

    private void deleteNote() {
        if (note != null && note.getId() > 0) {
            executorService.execute(() -> {
                NoteDatabase.getInstance(getContext()).noteDao().delete(note);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Заметка удалена", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                });
            });
        } else {
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}