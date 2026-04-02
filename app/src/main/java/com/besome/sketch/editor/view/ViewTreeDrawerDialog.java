package com.besome.sketch.editor.view;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ViewBean;

import java.util.ArrayList;
import java.util.HashMap;

import pro.sketchware.R;

public class ViewTreeDrawerDialog extends DialogFragment {

    private final ArrayList<ViewBean> currentViews;
    private final OnViewSelectedListener listener;
    private final ArrayList<TreeNode> treeNodesList = new ArrayList<>();

    public interface OnViewSelectedListener {
        void onSelected(String viewId);
    }

    public ViewTreeDrawerDialog(ArrayList<ViewBean> views, OnViewSelectedListener listener) {
        this.currentViews = views;
        this.listener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.START); // Opens from the left like a real Drawer
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.Animation_Design_BottomSheetDialog);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new ViewGroup.LayoutParams(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 320, getResources().getDisplayMetrics()),
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Use Surface Container Low for Drawer Background (Matches Design Drawer)
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.colorSurfaceContainerLow, typedValue, true);
        root.setBackgroundColor(typedValue.data);

        // Add Header Title "Component Tree"
        TextView header = new TextView(requireContext());
        header.setText("Component Tree");
        header.setTextSize(20);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        requireContext().getTheme().resolveAttribute(R.attr.colorOnSurface, typedValue, true);
        header.setTextColor(typedValue.data);
        
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        header.setPadding(padding, padding + 16, padding, padding);
        root.addView(header);

        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setClipToPadding(false);
        rv.setPadding(0, 0, 0, padding);
        
        buildTree();
        rv.setAdapter(new TreeAdapter());
        
        root.addView(rv);
        return root;
    }

    private void buildTree() {
        HashMap<String, ArrayList<ViewBean>> childrenMap = new HashMap<>();
        ArrayList<ViewBean> rootViews = new ArrayList<>();

        for (ViewBean bean : currentViews) {
            if (bean.parent == null || bean.parent.equals("root") || bean.parent.isEmpty()) {
                rootViews.add(bean);
            } else {
                childrenMap.computeIfAbsent(bean.parent, k -> new ArrayList<>()).add(bean);
            }
        }

        for (ViewBean root : rootViews) {
            buildTreeList(root, childrenMap, 0);
        }
    }

    private void buildTreeList(ViewBean parent, HashMap<String, ArrayList<ViewBean>> childrenMap, int depth) {
        treeNodesList.add(new TreeNode(parent, depth));

        ArrayList<ViewBean> children = childrenMap.get(parent.id);
        if (children != null) {
            for (ViewBean child : children) {
                buildTreeList(child, childrenMap, depth + 1);
            }
        }
    }

    private static class TreeNode {
        ViewBean viewBean;
        int depth;
        TreeNode(ViewBean viewBean, int depth) {
            this.viewBean = viewBean;
            this.depth = depth;
        }
    }

    private class TreeAdapter extends RecyclerView.Adapter<TreeAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_tree_node, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TreeNode node = treeNodesList.get(position);
            
            // Set Titles and Icons
            holder.tvTitle.setText(node.viewBean.id);
            
            String typeName = ViewBean.getViewTypeName(node.viewBean.type);
            // If it's a Custom View or similar, show that information too
            if (node.viewBean.customView != null && !node.viewBean.customView.isEmpty() && !node.viewBean.customView.equals("none") && !node.viewBean.customView.equals("NONE")) {
                typeName += " (" + node.viewBean.customView + ")";
            }
            holder.tvSubtitle.setText(typeName);
            holder.imgIcon.setImageResource(ViewBean.getViewTypeResId(node.viewBean.type));

            // Setup Indentation based on Depth
            int paddingBase = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
            int paddingDepth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, node.depth * 22, getResources().getDisplayMetrics());
            
            holder.containerLayout.setPadding(paddingBase + paddingDepth, holder.containerLayout.getPaddingTop(), 
                    holder.containerLayout.getPaddingRight(), holder.containerLayout.getPaddingBottom());

            // Show "└" branch symbol if it's a child element
            if (node.depth > 0) {
                holder.tvBranch.setVisibility(View.VISIBLE);
            } else {
                holder.tvBranch.setVisibility(View.GONE);
            }

            // Handle Click
            holder.itemView.setOnClickListener(v -> {
                listener.onSelected(node.viewBean.id);
                dismiss();
            });
        }

        @Override
        public int getItemCount() {
            return treeNodesList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout containerLayout;
            TextView tvBranch, tvTitle, tvSubtitle;
            ImageView imgIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                containerLayout = itemView.findViewById(R.id.container_layout);
                tvBranch = itemView.findViewById(R.id.tv_branch);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
                imgIcon = itemView.findViewById(R.id.img_icon);
            }
        }
    }
}
