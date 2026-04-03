package mod.sketchlibx.settings;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ViewBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import a.a.a.jC;

public class ProjectAnalyzerEngine {

    public static String analyze(String sc_id) {
        StringBuilder report = new StringBuilder();
        
        ArrayList<ProjectFileBean> allFiles = jC.b(sc_id).b();
        if (allFiles == null || allFiles.isEmpty()) return "Failed to analyze project.";

        int unusedViewsCount = 0;
        int duplicateIdCount = 0;
        int heavyLayoutCount = 0;

        for (ProjectFileBean file : allFiles) {
            String targetFileName = file.getXmlName().isEmpty() ? file.getJavaName() : file.getXmlName();
            ArrayList<ViewBean> views = jC.a(sc_id).d(targetFileName);
            HashMap<String, ArrayList<BlockBean>> events = jC.a(sc_id).b(file.getJavaName());

            Set<String> usedViewIds = new HashSet<>();
            Set<String> declaredViewIds = new HashSet<>();

            // Extract all Block arguments looking for View IDs
            if (events != null) {
                for (Map.Entry<String, ArrayList<BlockBean>> entry : events.entrySet()) {
                    for (BlockBean block : entry.getValue()) {
                        if (block.parameters != null) {
                            usedViewIds.addAll(block.parameters);
                        }
                    }
                }
            }

            if (views != null) {
                if (views.size() > 60) heavyLayoutCount++; // Heavy Layout Detection

                for (ViewBean view : views) {
                    // Check for Duplicates
                    if (!declaredViewIds.add(view.id)) {
                        duplicateIdCount++;
                        report.append("- Duplicate ID: ").append(view.id).append(" in ").append(targetFileName).append("\n");
                    }

                    if (!view.id.equals("linear1") && !view.id.equals("root") && !usedViewIds.contains(view.id)) {
                        unusedViewsCount++;
                    }
                }
            }
        }

        StringBuilder header = new StringBuilder();
        header.append("--- Project Analysis Report ---\n\n");
        header.append("Duplicate IDs: ").append(duplicateIdCount).append("\n");
        header.append("Unused Views: ").append(unusedViewsCount).append("\n");
        header.append("Heavy Layouts (>60 views): ").append(heavyLayoutCount).append("\n");

        // Only show details section if there are actually duplicates to show
        if (duplicateIdCount > 0) {
            header.append("\nDetails:\n");
        }

        report.insert(0, header.toString());

        // Clean status message at the bottom
        if (duplicateIdCount == 0 && unusedViewsCount < 5 && heavyLayoutCount == 0) {
            report.append("\nStatus: Your project is well optimized.");
        } else {
            report.append("\nStatus: Project could use some optimization.");
        }

        return report.toString();
    }
}
