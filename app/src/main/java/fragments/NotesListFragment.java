package com.example.notesapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.notesapp.R;
import com.example.notesapp.adapters.NoteAdapter;
import com.example.notesapp.database.NoteDatabase;
import com.example.notesapp.models.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesListFragment extends Fragment implements NoteAdapter.OnNoteClickListener {

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAdd;
    private ExecutorService executorService;
    private List<Note> notes;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes_list, container, false);

        executorService = Executors.newSingleThreadExecutor();

        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAdd = view.findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        fabAdd.setOnClickListener(v -> openNoteEditor(null));

        swipeRefresh.setOnRefreshListener(() -> loadNotes());

        loadNotes();

        return view;
    }

    private void loadNotes() {
        executorService.execute(() -> {
            notes = NoteDatabase.getInstance(getContext()).noteDao().getAllNotes();
            requireActivity().runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new NoteAdapter(notes, this);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateNotes(notes);
                }
                swipeRefresh.setRefreshing(false);
            });
        });
    }

    private void openNoteEditor(Note note) {
        NoteEditorFragment fragment = NoteEditorFragment.newInstance(note);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNoteClick(Note note) {
        openNoteEditor(note);
    }

    @Override
    public void onNoteLongClick(Note note) {
        // Удаление заметки
        executorService.execute(() -> {
            NoteDatabase.getInstance(getContext()).noteDao().delete(note);
            requireActivity().runOnUiThread(() -> {
                loadNotes();
                Toast.makeText(getContext(), "Заметка удалена", Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}