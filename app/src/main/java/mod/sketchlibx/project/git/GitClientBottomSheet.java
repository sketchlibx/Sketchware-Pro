package mod.sketchlibx.project.git;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import a.a.a.jC;
import a.a.a.wq;
import a.a.a.yq;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.databinding.DialogCreateNewFileLayoutBinding;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.ThemeUtils;

public class GitClientBottomSheet extends BottomSheetDialogFragment {

    private String sc_id;
    private Git git;
    private String gitWorkspacePath;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDirectPushEnabled = false;

    // Changes Tab
    private RecyclerView rvChanges;
    private GitChangeAdapter changesAdapter;
    private TextView tvStatusEmpty;
    private MaterialButton btnCommit;
    private MaterialButton btnCommitPush;
    private TextInputEditText etCommitMsg;
    private TextInputLayout tilCommitMsg;

    // History Tab
    private RecyclerView rvHistory;
    private TextView tvHistoryEmpty;
    private GitHistoryAdapter historyAdapter;

    // Branches Tab
    private RecyclerView rvBranches;
    private GitBranchAdapter branchAdapter;

    // Remotes Tab
    private RecyclerView rvRemotes;
    private GitRemoteAdapter remoteAdapter;

    public GitClientBottomSheet() {
    }

    public GitClientBottomSheet(String sc_id) {
        this.sc_id = sc_id;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isDirectPushEnabled = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_GIT_DIRECT_PUSH);
        gitWorkspacePath = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/git_workspace";
        
        try {
            File gitDir = new File(gitWorkspacePath, ".git");
            if (gitDir.exists()) {
                git = Git.open(new File(gitWorkspacePath));
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

        GitPagerAdapter adapter = new GitPagerAdapter(requireContext(), inflater);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (git != null) git.close();
    }


    // 1. Changes Tab

    private void setupChangesTab(View view) {
        rvChanges = view.findViewById(R.id.rv_changes);
        tvStatusEmpty = view.findViewById(R.id.tv_status_empty);
        btnCommit = view.findViewById(R.id.btn_commit);
        btnCommitPush = view.findViewById(R.id.btn_commit_push);
        etCommitMsg = view.findViewById(R.id.et_commit_msg);
        tilCommitMsg = view.findViewById(R.id.til_commit_msg);
        MaterialButton btnStageAll = view.findViewById(R.id.btn_stage_all);
        ImageView btnRefresh = view.findViewById(R.id.btn_refresh);

        rvChanges.setLayoutManager(new LinearLayoutManager(getContext()));
        changesAdapter = new GitChangeAdapter();
        rvChanges.setAdapter(changesAdapter);

        if (isDirectPushEnabled) btnCommitPush.setVisibility(View.VISIBLE);

        updateCommitButtonState();
        etCommitMsg.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateCommitButtonState(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnRefresh.setOnClickListener(v -> loadGitStatus());
        btnStageAll.setOnClickListener(v -> performStageAll());
        btnCommit.setOnClickListener(v -> performCommit(false));
        btnCommitPush.setOnClickListener(v -> performCommit(true));

        loadGitStatus();
    }

    private void updateCommitButtonState() {
        boolean hasMessage = etCommitMsg.getText() != null && !etCommitMsg.getText().toString().trim().isEmpty();
        boolean hasStagedFiles = changesAdapter != null && changesAdapter.hasStagedFiles();
        btnCommit.setEnabled(hasMessage && hasStagedFiles);
        btnCommitPush.setEnabled(hasMessage && hasStagedFiles);
        tilCommitMsg.setHelperText(hasStagedFiles ? null : "Stage files to commit");
    }

    private void loadGitStatus() {
        if (git == null) return;
        new Thread(() -> {
            try {
                Status status = git.status().call();
                List<GitFile> fileList = new ArrayList<>();

                for (String s : status.getUntracked()) fileList.add(new GitFile(s, "Untracked", false, Color.parseColor("#4CAF50")));
                for (String s : status.getModified()) fileList.add(new GitFile(s, "Modified", false, Color.parseColor("#2196F3")));
                for (String s : status.getMissing()) fileList.add(new GitFile(s, "Deleted", false, Color.parseColor("#F44336")));

                for (String s : status.getAdded()) fileList.add(new GitFile(s, "Added", true, Color.parseColor("#4CAF50")));
                for (String s : status.getChanged()) fileList.add(new GitFile(s, "Modified", true, Color.parseColor("#2196F3")));
                for (String s : status.getRemoved()) fileList.add(new GitFile(s, "Deleted", true, Color.parseColor("#F44336")));

                mainHandler.post(() -> {
                    if (fileList.isEmpty()) {
                        tvStatusEmpty.setVisibility(View.VISIBLE);
                        rvChanges.setVisibility(View.GONE);
                    } else {
                        tvStatusEmpty.setVisibility(View.GONE);
                        rvChanges.setVisibility(View.VISIBLE);
                    }
                    changesAdapter.setFiles(fileList);
                    updateCommitButtonState();
                });
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Failed to get status: " + e.getMessage()));
            }
        }).start();
    }

    private void performStageAll() {
        if (git == null) return;
        SketchwareUtil.toast("Syncing core files to Git workspace & staging...");
        new Thread(() -> {
            try {
                // Generate files to temporary mysc folder
                yq projectYq = new yq(getContext(), sc_id);
                projectYq.a(jC.c(sc_id), jC.b(sc_id), jC.a(sc_id));

                // Path to core source in mysc
                String generatedMain = wq.d(sc_id) + File.separator + "app" + File.separator + "src" + File.separator + "main";
                // Target path in our safe git workspace
                String targetMain = gitWorkspacePath + File.separator + "app" + File.separator + "src" + File.separator + "main";

                // Ensure perfect synchronization (deleting first handles deleted blocks/files)
                FileUtil.deleteFile(targetMain);
                FileUtil.copyDirectory(new File(generatedMain), new File(targetMain));

                // Add to Git
                git.add().addFilepattern(".").call();
                
                // Track missing/deleted files
                Status status = git.status().call();
                if (!status.getMissing().isEmpty()) {
                    org.eclipse.jgit.api.RmCommand rm = git.rm();
                    for (String missing : status.getMissing()) rm.addFilepattern(missing);
                    rm.call();
                }

                loadGitStatus();
                mainHandler.post(() -> SketchwareUtil.toast("Files synced & staged cleanly!"));
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Stage failed: " + e.getMessage()));
            }
        }).start();
    }

    private void performCommit(boolean pushAfter) {
        if (git == null) return;
        String message = etCommitMsg.getText().toString().trim();
        new Thread(() -> {
            try {
                git.commit().setMessage(message).call();
                mainHandler.post(() -> {
                    etCommitMsg.setText("");
                    SketchwareUtil.toast("Committed successfully!");
                    loadGitStatus();
                    loadHistory();
                });
                if (pushAfter) {
                    performNetworkOperation("Pushing...", () -> git.push().setCredentialsProvider(getCredentials()).call());
                }
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Commit failed: " + e.getMessage()));
            }
        }).start();
    }


    // 2. History Tab

    private void setupHistoryTab(View view) {
        rvHistory = view.findViewById(R.id.rv_history);
        tvHistoryEmpty = view.findViewById(R.id.tv_history_empty);
        ImageView btnRefresh = view.findViewById(R.id.btn_refresh_history);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new GitHistoryAdapter();
        rvHistory.setAdapter(historyAdapter);

        btnRefresh.setOnClickListener(v -> loadHistory());
        loadHistory();
    }

    private void loadHistory() {
        if (git == null || historyAdapter == null) return;
        new Thread(() -> {
            try {
                Iterable<RevCommit> commits = git.log().call();
                List<RevCommit> commitList = new ArrayList<>();
                for (RevCommit commit : commits) {
                    commitList.add(commit);
                }
                mainHandler.post(() -> {
                    if (commitList.isEmpty()) {
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        tvHistoryEmpty.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        historyAdapter.setCommits(commitList);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                });
            }
        }).start();
    }


    // 3. Branches Tab

    private void setupBranchesTab(View view) {
        rvBranches = view.findViewById(R.id.rv_branches);
        ExtendedFloatingActionButton fabNewBranch = view.findViewById(R.id.fab_new_branch);

        rvBranches.setLayoutManager(new LinearLayoutManager(getContext()));
        branchAdapter = new GitBranchAdapter();
        rvBranches.setAdapter(branchAdapter);

        fabNewBranch.setOnClickListener(v -> showAddBranchDialog());

        loadBranches();
    }

    private void loadBranches() {
        if (git == null || branchAdapter == null) return;
        new Thread(() -> {
            try {
                List<Ref> call = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
                String currentBranch = git.getRepository().getBranch();
                mainHandler.post(() -> branchAdapter.setBranches(call, currentBranch));
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Failed to load branches: " + e.getMessage()));
            }
        }).start();
    }

    private void showAddBranchDialog() {
        DialogCreateNewFileLayoutBinding binding = DialogCreateNewFileLayoutBinding.inflate(getLayoutInflater());
        binding.chipGroupTypes.setVisibility(View.GONE);
        binding.inputText.setHint("Branch name");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create Branch")
                .setView(binding.getRoot())
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = binding.inputText.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createNewBranch(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createNewBranch(String name) {
        new Thread(() -> {
            try {
                git.branchCreate().setName(name).call();
                mainHandler.post(() -> {
                    SketchwareUtil.toast("Branch created!");
                    loadBranches();
                });
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Failed to create branch: " + e.getMessage()));
            }
        }).start();
    }


    // 4. Remotes & Network Tab

    private void setupRemotesTab(View view) {
        rvRemotes = view.findViewById(R.id.rv_remotes);
        ExtendedFloatingActionButton fabNewRemote = view.findViewById(R.id.fab_new_remote);
        MaterialButton btnFetch = view.findViewById(R.id.btn_git_fetch);
        MaterialButton btnPull = view.findViewById(R.id.btn_git_pull);
        MaterialButton btnPush = view.findViewById(R.id.btn_git_push);

        rvRemotes.setLayoutManager(new LinearLayoutManager(getContext()));
        remoteAdapter = new GitRemoteAdapter();
        rvRemotes.setAdapter(remoteAdapter);

        fabNewRemote.setOnClickListener(v -> showAddRemoteDialog());

        btnFetch.setOnClickListener(v -> performNetworkOperation("Fetching...", () -> git.fetch().setCredentialsProvider(getCredentials()).call()));
        btnPull.setOnClickListener(v -> performNetworkOperation("Pulling...", () -> git.pull().setCredentialsProvider(getCredentials()).call()));
        btnPush.setOnClickListener(v -> performNetworkOperation("Pushing...", () -> git.push().setCredentialsProvider(getCredentials()).call()));

        loadRemotes();
    }

    private void loadRemotes() {
        if (git == null || remoteAdapter == null) return;
        new Thread(() -> {
            try {
                List<RemoteConfig> remotes = git.remoteList().call();
                mainHandler.post(() -> remoteAdapter.setRemotes(remotes));
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Failed to load remotes: " + e.getMessage()));
            }
        }).start();
    }

    private void showAddRemoteDialog() {
        // Since we need 2 inputs for Remotes, we programmatically create properly styled Material 3 Layouts
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 24);

        TextInputLayout tilName = new TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        tilName.setHint("Remote name (e.g. origin)");
        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setText("origin");
        tilName.addView(etName);

        TextInputLayout tilUrl = new TextInputLayout(requireContext(), null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        tilUrl.setHint("Repository URL (https://...)");
        TextInputEditText etUrl = new TextInputEditText(requireContext());
        tilUrl.addView(etUrl);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 24, 0, 0);
        tilUrl.setLayoutParams(params);

        container.addView(tilName);
        container.addView(tilUrl);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Remote")
                .setView(container)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();
                    if (!name.isEmpty() && !url.isEmpty()) {
                        addNewRemote(name, url);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addNewRemote(String name, String url) {
        new Thread(() -> {
            try {
                git.remoteAdd().setName(name).setUri(new URIish(url)).call();
                mainHandler.post(() -> {
                    SketchwareUtil.toast("Remote added!");
                    loadRemotes();
                });
            } catch (Exception e) {
                mainHandler.post(() -> SketchwareUtil.toastError("Failed to add remote: " + e.getMessage()));
            }
        }).start();
    }

    private CredentialsProvider getCredentials() {
        SharedPreferences prefs = requireContext().getSharedPreferences("GitConfig_" + sc_id, Context.MODE_PRIVATE);
        String token = prefs.getString("pat_token", "");
        String email = "developer@sketchware.pro";
        try {
            email = git.getRepository().getConfig().getString("user", null, "email");
        } catch (Exception ignored) {}

        // For GitHub/GitLab with PAT, username can be email/anything, and password must be the Token
        return new UsernamePasswordCredentialsProvider(email, token);
    }

    private void performNetworkOperation(String startMessage, NetworkAction action) {
        SketchwareUtil.toast(startMessage);
        new Thread(() -> {
            try {
                action.execute();
                mainHandler.post(() -> {
                    SketchwareUtil.toast("Operation Successful!");
                    loadGitStatus();
                    loadHistory();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    SketchwareUtil.toastError("Network Error: " + e.getMessage());
                    Log.e("GitClient", "Network Operation Error", e);
                });
            }
        }).start();
    }

    private interface NetworkAction {
        void execute() throws Exception;
    }

    // 5. Settings Tab
    private void setupSettingsTab(View view) {
        TextInputEditText etName = view.findViewById(R.id.et_git_name);
        TextInputEditText etEmail = view.findViewById(R.id.et_git_email);
        TextInputEditText etToken = view.findViewById(R.id.et_git_token);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_settings);

        SharedPreferences prefs = requireContext().getSharedPreferences("GitConfig_" + sc_id, Context.MODE_PRIVATE);
        
        try {
            StoredConfig config = git.getRepository().getConfig();
            etName.setText(config.getString("user", null, "name"));
            etEmail.setText(config.getString("user", null, "email"));
        } catch (Exception ignored) {}

        etToken.setText(prefs.getString("pat_token", ""));

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String token = etToken.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty()) {
                SketchwareUtil.toastError("Name and Email are required!");
                return;
            }

            try {
                StoredConfig config = git.getRepository().getConfig();
                config.setString("user", null, "name", name);
                config.setString("user", null, "email", email);
                config.save();

                prefs.edit().putString("pat_token", token).apply();
                SketchwareUtil.toast("Git Configuration Saved!");
            } catch (Exception e) {
                SketchwareUtil.toastError("Failed to save: " + e.getMessage());
            }
        });
    }

    // Adapters (Changes, History, Branches, Remotes)

    private static class GitFile {
        String path, statusLabel;
        boolean isStaged;
        int color;
        GitFile(String path, String statusLabel, boolean isStaged, int color) {
            this.path = path; this.statusLabel = statusLabel; this.isStaged = isStaged; this.color = color;
        }
    }

    private class GitChangeAdapter extends RecyclerView.Adapter<GitChangeAdapter.ViewHolder> {
        private List<GitFile> files = new ArrayList<>();
        public void setFiles(List<GitFile> files) { this.files = files; notifyDataSetChanged(); }
        public boolean hasStagedFiles() { for (GitFile f : files) if (f.isStaged) return true; return false; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(parent.getContext()); root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); root.setOrientation(0); root.setGravity(16); root.setPadding(0, 16, 0, 16);
            TextView tvBadge = new TextView(parent.getContext()); tvBadge.setTextSize(12f); tvBadge.setPadding(12, 4, 12, 4); tvBadge.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2); badgeParams.setMarginEnd(16); root.addView(tvBadge, badgeParams);
            TextView tvPath = new TextView(parent.getContext()); tvPath.setTextSize(14f); tvPath.setMaxLines(1); tvPath.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE); tvPath.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurface));
            root.addView(tvPath, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView tvAction = new TextView(parent.getContext()); tvAction.setTextSize(12f); tvAction.setPadding(16, 8, 16, 8); tvAction.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorPrimary)); root.addView(tvAction);
            return new ViewHolder(root, tvBadge, tvPath, tvAction);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GitFile file = files.get(position);
            holder.tvBadge.setText(file.statusLabel.substring(0, 1));
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(); gd.setColor(file.color); gd.setCornerRadius(8f); holder.tvBadge.setBackground(gd);
            holder.tvPath.setText(file.path);
            holder.tvAction.setText(file.isStaged ? "UNSTAGE" : "STAGE");
            holder.tvAction.setOnClickListener(v -> {
                if (git == null) return;
                new Thread(() -> {
                    try {
                        if (file.isStaged) git.reset().addPath(file.path).call();
                        else if (file.statusLabel.equals("Deleted")) git.rm().addFilepattern(file.path).call();
                        else git.add().addFilepattern(file.path).call();
                        loadGitStatus();
                    } catch (Exception e) { mainHandler.post(() -> SketchwareUtil.toastError("Action failed: " + e.getMessage())); }
                }).start();
            });
        }
        @Override public int getItemCount() { return files.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvBadge, tvPath, tvAction; ViewHolder(View i, TextView b, TextView p, TextView a) { super(i); tvBadge=b; tvPath=p; tvAction=a; } }
    }

    private class GitHistoryAdapter extends RecyclerView.Adapter<GitHistoryAdapter.ViewHolder> {
        private List<RevCommit> commits = new ArrayList<>();
        private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        public void setCommits(List<RevCommit> commits) { this.commits = commits; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MaterialCardView card = new MaterialCardView(parent.getContext()); card.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); ((RecyclerView.LayoutParams) card.getLayoutParams()).setMargins(0, 0, 0, 16); card.setRadius(16f); card.setCardElevation(0f); card.setCardBackgroundColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorSurfaceVariant));
            LinearLayout root = new LinearLayout(parent.getContext()); root.setOrientation(1); root.setPadding(32, 24, 32, 24); card.addView(root);
            TextView tvMessage = new TextView(parent.getContext()); tvMessage.setTextSize(16f); tvMessage.setTypeface(null, Typeface.BOLD); tvMessage.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurface)); root.addView(tvMessage);
            TextView tvDetails = new TextView(parent.getContext()); tvDetails.setTextSize(12f); tvDetails.setPadding(0, 8, 0, 0); tvDetails.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurfaceVariant)); root.addView(tvDetails);
            return new ViewHolder(card, tvMessage, tvDetails);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RevCommit commit = commits.get(position);
            holder.tvMessage.setText(commit.getShortMessage());
            holder.tvDetails.setText(String.format("%s • %s • %s", commit.getName().substring(0, 7), commit.getAuthorIdent().getName(), sdf.format(new Date(commit.getCommitTime() * 1000L))));
        }
        @Override public int getItemCount() { return commits.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvMessage, tvDetails; ViewHolder(View i, TextView m, TextView d) { super(i); tvMessage=m; tvDetails=d; } }
    }

    private class GitBranchAdapter extends RecyclerView.Adapter<GitBranchAdapter.ViewHolder> {
        private List<Ref> branches = new ArrayList<>();
        private String currentBranch = "";
        public void setBranches(List<Ref> branches, String currentBranch) { this.branches = branches; this.currentBranch = currentBranch; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(parent.getContext()); root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); root.setOrientation(0); root.setGravity(16); root.setPadding(0, 24, 0, 24);
            TextView tvName = new TextView(parent.getContext()); tvName.setTextSize(16f); tvName.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurface)); root.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView tvCheckout = new TextView(parent.getContext()); tvCheckout.setTextSize(12f); tvCheckout.setPadding(16, 8, 16, 8); tvCheckout.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorPrimary)); root.addView(tvCheckout);
            TextView tvDelete = new TextView(parent.getContext()); tvDelete.setTextSize(12f); tvDelete.setPadding(16, 8, 0, 8); tvDelete.setTextColor(Color.parseColor("#F44336")); root.addView(tvDelete);
            return new ViewHolder(root, tvName, tvCheckout, tvDelete);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Ref ref = branches.get(position);
            String name = ref.getName().replace("refs/heads/", "").replace("refs/remotes/", "");
            boolean isCurrent = ref.getName().equals(currentBranch) || name.equals(currentBranch.replace("refs/heads/", ""));
            
            holder.tvName.setText(isCurrent ? "✓ " + name : name);
            holder.tvName.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
            
            if (isCurrent) {
                holder.tvCheckout.setText("ACTIVE");
                holder.tvCheckout.setEnabled(false);
                holder.tvDelete.setVisibility(View.GONE);
            } else {
                holder.tvCheckout.setText("CHECKOUT");
                holder.tvCheckout.setEnabled(true);
                holder.tvDelete.setVisibility(View.VISIBLE);
                holder.tvDelete.setText("DELETE");
                
                holder.tvCheckout.setOnClickListener(v -> {
                    new Thread(() -> {
                        try { git.checkout().setName(name).call(); mainHandler.post(() -> { SketchwareUtil.toast("Switched to " + name); loadBranches(); });
                        } catch (Exception e) { mainHandler.post(() -> SketchwareUtil.toastError("Checkout failed")); }
                    }).start();
                });
                holder.tvDelete.setOnClickListener(v -> {
                    new Thread(() -> {
                        try { git.branchDelete().setBranchNames(name).setForce(true).call(); mainHandler.post(() -> { SketchwareUtil.toast("Branch deleted"); loadBranches(); });
                        } catch (Exception e) { mainHandler.post(() -> SketchwareUtil.toastError("Delete failed")); }
                    }).start();
                });
            }
        }
        @Override public int getItemCount() { return branches.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvName, tvCheckout, tvDelete; ViewHolder(View i, TextView n, TextView c, TextView d) { super(i); tvName=n; tvCheckout=c; tvDelete=d; } }
    }

    private class GitRemoteAdapter extends RecyclerView.Adapter<GitRemoteAdapter.ViewHolder> {
        private List<RemoteConfig> remotes = new ArrayList<>();
        public void setRemotes(List<RemoteConfig> remotes) { this.remotes = remotes; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(parent.getContext()); root.setLayoutParams(new RecyclerView.LayoutParams(-1, -2)); root.setOrientation(1); root.setPadding(0, 16, 0, 16);
            LinearLayout top = new LinearLayout(parent.getContext()); top.setOrientation(0); root.addView(top);
            TextView tvName = new TextView(parent.getContext()); tvName.setTextSize(16f); tvName.setTypeface(null, Typeface.BOLD); tvName.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurface)); top.addView(tvName, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView tvDelete = new TextView(parent.getContext()); tvDelete.setTextSize(12f); tvDelete.setTextColor(Color.parseColor("#F44336")); tvDelete.setText("REMOVE"); top.addView(tvDelete);
            TextView tvUrl = new TextView(parent.getContext()); tvUrl.setTextSize(12f); tvUrl.setTextColor(ThemeUtils.getColor(parent.getContext(), R.attr.colorOnSurfaceVariant)); root.addView(tvUrl);
            return new ViewHolder(root, tvName, tvUrl, tvDelete);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RemoteConfig remote = remotes.get(position);
            holder.tvName.setText(remote.getName());
            holder.tvUrl.setText(remote.getURIs().isEmpty() ? "No URL" : remote.getURIs().get(0).toString());
            holder.tvDelete.setOnClickListener(v -> {
                new Thread(() -> {
                    try { git.remoteRemove().setRemoteName(remote.getName()).call(); mainHandler.post(() -> { SketchwareUtil.toast("Removed remote"); loadRemotes(); });
                    } catch (Exception e) { mainHandler.post(() -> SketchwareUtil.toastError("Remove failed")); }
                }).start();
            });
        }
        @Override public int getItemCount() { return remotes.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvName, tvUrl, tvDelete; ViewHolder(View i, TextView n, TextView u, TextView d) { super(i); tvName=n; tvUrl=u; tvDelete=d; } }
    }

    private class GitPagerAdapter extends PagerAdapter {
        private final Context context;
        private final LayoutInflater inflater;
        private final String[] tabTitles = {"Changes", "History", "Branches", "Remotes", "Settings"};

        public GitPagerAdapter(Context context, LayoutInflater inflater) {
            this.context = context;
            this.inflater = inflater;
        }

        @Override public int getCount() { return tabTitles.length; }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view;
            if (position == 0) {
                view = inflater.inflate(R.layout.tab_git_changes, container, false);
                setupChangesTab(view);
            } else if (position == 1) {
                view = inflater.inflate(R.layout.tab_git_history, container, false);
                setupHistoryTab(view);
            } else if (position == 2) {
                view = inflater.inflate(R.layout.tab_git_branches, container, false);
                setupBranchesTab(view);
            } else if (position == 3) {
                view = inflater.inflate(R.layout.tab_git_remotes, container, false);
                setupRemotesTab(view);
            } else if (position == 4) {
                view = inflater.inflate(R.layout.tab_git_settings, container, false);
                setupSettingsTab(view);
            } else {
                FrameLayout frameLayout = new FrameLayout(context);
                view = frameLayout;
            }
            container.addView(view);
            return view;
        }

        @Override public boolean isViewFromObject(@NonNull View view, @NonNull Object object) { return view == object; }
        @Override public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) { container.removeView((View) object); }
        @Nullable @Override public CharSequence getPageTitle(int position) { return tabTitles[position]; }
    }
}
