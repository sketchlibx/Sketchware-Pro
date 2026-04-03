package mod.sketchlibx.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.besome.sketch.design.DesignActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import a.a.a.jC;
import a.a.a.yq;
import mod.hey.studios.code.SrcCodeEditor;
import mod.hey.studios.project.ProjectSettings;
import mod.hilal.saif.activities.android_manifest.AndroidManifestInjection;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class AdvancedSettingsBottomSheet extends BottomSheetDialogFragment {

    private final String sc_id;
    private final String currentJavaFileName;
    private final DesignActivity activity;
    private ProjectSettings projectSettings;

    public AdvancedSettingsBottomSheet(String sc_id, String currentJavaFileName, DesignActivity activity) {
        this.sc_id = sc_id;
        this.currentJavaFileName = currentJavaFileName;
        this.activity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_advanced_settings, container, false);
        projectSettings = new ProjectSettings(sc_id);

        CheckBox cbForceAndroidX = root.findViewById(R.id.cb_force_androidx);
        CheckBox cbKotlinConversion = root.findViewById(R.id.cb_java_to_kotlin);
        
        cbForceAndroidX.setChecked(projectSettings.getValue(ProjectSettings.SETTING_FORCE_ANDROIDX, ProjectSettings.SETTING_GENERIC_VALUE_FALSE).equals("true"));
        cbKotlinConversion.setChecked(projectSettings.getValue(ProjectSettings.SETTING_JAVA_TO_KOTLIN, ProjectSettings.SETTING_GENERIC_VALUE_FALSE).equals("true"));

        cbForceAndroidX.setOnCheckedChangeListener((buttonView, isChecked) -> projectSettings.setValue(ProjectSettings.SETTING_FORCE_ANDROIDX, isChecked ? "true" : "false"));
        cbKotlinConversion.setOnCheckedChangeListener((buttonView, isChecked) -> projectSettings.setValue(ProjectSettings.SETTING_JAVA_TO_KOTLIN, isChecked ? "true" : "false"));

        root.findViewById(R.id.btn_project_analyzer).setOnClickListener(v -> {
            dismiss();
            runProjectAnalyzer();
        });

        root.findViewById(R.id.btn_custom_java).setOnClickListener(v -> {
            dismiss();
            openCustomJavaEditor();
        });

        root.findViewById(R.id.btn_custom_manifest).setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(activity, AndroidManifestInjection.class);
            intent.putExtra("sc_id", sc_id);
            intent.putExtra("file_name", currentJavaFileName);
            startActivity(intent);
        });

        return root;
    }

    private void runProjectAnalyzer() {
        AlertDialog progress = new MaterialAlertDialogBuilder(activity)
                .setTitle("Analyzing Project...")
                .setMessage("Scanning for unused views, duplicate IDs and heavy layouts. Please wait.")
                .setCancelable(false)
                .show();

        new Thread(() -> {
            String report = ProjectAnalyzerEngine.analyze(sc_id);
            activity.runOnUiThread(() -> {
                progress.dismiss();
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Analyzer Report")
                        .setMessage(report)
                        .setPositiveButton("Dismiss", null)
                        .show();
            });
        }).start();
    }

    private void openCustomJavaEditor() {
        String customJavaDir = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + sc_id + "/custom_java/";
        FileUtil.makeDir(customJavaDir);
        String customJavaPath = customJavaDir + currentJavaFileName;

        if (!FileUtil.isExistFile(customJavaPath)) {
            String source = new yq(activity.getApplicationContext(), sc_id).getFileSrc(currentJavaFileName, jC.b(sc_id), jC.a(sc_id), jC.c(sc_id));
            FileUtil.writeFile(customJavaPath, source);
        }

        Intent intent = new Intent(activity, SrcCodeEditor.class);
        intent.putExtra("content", customJavaPath);
        intent.putExtra("title", "Custom " + currentJavaFileName);
        intent.putExtra("sc_id", sc_id);
        startActivity(intent);
    }
}
