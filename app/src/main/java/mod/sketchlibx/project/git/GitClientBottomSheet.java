package mod.sketchlibx.project.git;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;

import org.eclipse.jgit.api.Git;

import java.io.File;

import a.a.a.wq;
import pro.sketchware.R;

public class GitClientBottomSheet extends BottomSheetDialogFragment {

    private String sc_id;
    private Git git;

    public GitClientBottomSheet() {
    }

    public GitClientBottomSheet(String sc_id) {
        this.sc_id = sc_id;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize JGit Instance for the current project
        try {
            File gitDir = new File(wq.d(sc_id), ".git");
            if (gitDir.exists()) {
                git = Git.open(new File(wq.d(sc_id)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
                BottomSheetBehavior.from(bottomSheetInternal).setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_git_client, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        ViewPager viewPager = view.findViewById(R.id.view_pager);

        GitPagerAdapter adapter = new GitPagerAdapter(requireContext());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (git != null) {
            git.close(); // Prevent memory leaks
        }
    }

    // ------------------------------------------------------
    // Pager Adapter for our 5 Tabs
    // ------------------------------------------------------
    private class GitPagerAdapter extends PagerAdapter {
        private final Context context;
        private final String[] tabTitles = {"Changes", "History", "Branches", "Remotes", "Settings"};

        public GitPagerAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return tabTitles.length;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            // Temporary Placeholder Views for Phase 1
            // We will replace these with actual XML layouts in Phase 2
            FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT));

            TextView placeholderText = new TextView(context);
            placeholderText.setText("UI for " + tabTitles[position] + " will be here soon! 🚀");
            placeholderText.setGravity(Gravity.CENTER);
            placeholderText.setTextSize(16f);
            
            frameLayout.addView(placeholderText);
            container.addView(frameLayout);
            return frameLayout;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }
    }
}
