package mod.sketchlibx.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.design.DesignActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;

public class GlobalSearchDialog extends BottomSheetDialogFragment {

    private final String sc_id;
    private final DesignActivity activity;
    private RecyclerView recyclerView;
    private SearchAdapter adapter;
    private ProjectSearchEngine searchEngine;

    public GlobalSearchDialog(String sc_id, DesignActivity activity) {
        this.sc_id = sc_id;
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // UI created programmatically to avoid extra XML files
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView title = new TextView(getContext());
        title.setText("Global Project Search");
        title.setTextSize(18);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        EditText searchBox = new EditText(getContext());
        searchBox.setHint("Search views, variables, logic...");
        searchBox.setSingleLine(true);
        root.addView(searchBox);

        recyclerView = new RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        root.addView(recyclerView);

        adapter = new SearchAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        searchEngine = new ProjectSearchEngine(sc_id);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Run in background thread to avoid lagging UI
                new Thread(() -> {
                    List<SearchResult> results = searchEngine.search(s.toString());
                    getActivity().runOnUiThread(() -> adapter.updateData(results));
                }).start();
            }
        });

        return root;
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<SearchResult> items;

        public SearchAdapter(List<SearchResult> items) {
            this.items = items;
        }

        public void updateData(List<SearchResult> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
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
            title.setTextColor(0xFF000000); // Black

            TextView subtitle = new TextView(parent.getContext());
            subtitle.setTag("subtitle");
            subtitle.setTextSize(12);
            subtitle.setTextColor(0xFF757575); // Grey

            layout.addView(title);
            layout.addView(subtitle);

            return new ViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SearchResult result = items.get(position);
            TextView title = holder.itemView.findViewWithTag("title");
            TextView subtitle = holder.itemView.findViewWithTag("subtitle");

            title.setText("[" + result.category + "] " + result.title);
            subtitle.setText(result.fileName + " • " + result.description);

            holder.itemView.setOnClickListener(v -> {
                dismiss();
                activity.jumpToFileAndTab(result.fileName, result.tabIndex);
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
