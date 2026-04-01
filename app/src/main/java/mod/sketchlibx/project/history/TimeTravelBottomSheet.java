package mod.sketchlibx.project.history;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.design.DesignActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pro.sketchware.utility.SketchwareUtil;

public class TimeTravelBottomSheet extends BottomSheetDialogFragment {

    private final String sc_id;
    private final DesignActivity activity;

    public TimeTravelBottomSheet(String sc_id, DesignActivity activity) {
        this.sc_id = sc_id;
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(getContext());
        title.setText("Version History");
        title.setTextSize(18);
        title.setPadding(0, 0, 0, 8);
        root.addView(title);

        TextView subtitle = new TextView(getContext());
        subtitle.setText("Select a snapshot to instantly restore your blocks and views. Warning: Current unsaved changes will be lost.");
        subtitle.setTextSize(12);
        subtitle.setTextColor(0xFF757575);
        subtitle.setPadding(0, 0, 0, 24);
        root.addView(subtitle);

        RecyclerView recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        root.addView(recyclerView);

        File historyFolder = new File(Environment.getExternalStorageDirectory(), ".sketchware/backups/history/" + sc_id);
        List<File> snapshots = new ArrayList<>();
        if (historyFolder.exists() && historyFolder.listFiles() != null) {
            snapshots.addAll(Arrays.asList(historyFolder.listFiles()));
            // Sort by newest first
            snapshots.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        }

        if (snapshots.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("No history found. Save or Run the project to create a snapshot.");
            emptyText.setPadding(0, 32, 0, 0);
            root.addView(emptyText);
        } else {
            recyclerView.setAdapter(new SnapshotAdapter(snapshots));
        }

        return root;
    }

    private class SnapshotAdapter extends RecyclerView.Adapter<SnapshotAdapter.ViewHolder> {
        private final List<File> items;

        public SnapshotAdapter(List<File> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(16, 24, 16, 24);
            layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView title = new TextView(parent.getContext());
            title.setTag("title");
            title.setTextSize(16);
            title.setTextColor(0xFF000000);

            layout.addView(title);
            return new ViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = items.get(position);
            TextView title = holder.itemView.findViewWithTag("title");
            
            // Clean up name for display: Snapshot_10-Oct-2025_11-45-00_AM.zip -> 10-Oct-2025 11:45:00 AM
            String displayName = file.getName().replace("Snapshot_", "").replace(".zip", "").replace("_", " ");
            title.setText("🕒 " + displayName);

            holder.itemView.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Restore Snapshot?")
                        .setMessage("Are you sure you want to travel back in time to this version? Your current blocks and UI will be overwritten.")
                        .setPositiveButton("Restore", (dialog, which) -> {
                            dismiss();
                            if (TimeMachineManager.restoreSnapshot(sc_id, file)) {
                                activity.reloadProjectAfterTimeTravel();
                            } else {
                                SketchwareUtil.toastError("Failed to restore snapshot.");
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
