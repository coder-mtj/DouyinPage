package com.mtj.douyinpage.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.mtj.douyinpage.databinding.FragmentEmptyBinding;

/**
 * 空状态 Fragment
 * 
 * 职责：
 * - 显示空状态提示信息
 * - 用于"互关"、"粉丝"、"朋友"三个标签页
 * - 支持自定义提示文本
 */
public class EmptyFragment extends Fragment {
    // 参数键：提示文本
    private static final String ARG_MESSAGE = "message";

    // ViewBinding 对象，用于访问布局中的所有视图
    private FragmentEmptyBinding binding;

    /**
     * 创建 Fragment 视图
     * 使用 ViewBinding 初始化布局
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // 使用 ViewBinding 初始化布局
        binding = FragmentEmptyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后调用
     * 从参数中获取提示文本并设置到 TextView
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 从 Bundle 中获取提示文本，如果没有则使用默认文本
        String message = "暂无内容";
        if (getArguments() != null) {
            message = getArguments().getString(ARG_MESSAGE, "暂无内容");
        }
        // 设置提示文本
        binding.emptyText.setText(message);
    }

    /**
     * 工厂方法，创建 EmptyFragment 实例
     * 
     * @param message 要显示的提示文本（如"暂无互关"、"暂无粉丝"、"暂无朋友"）
     * @return EmptyFragment 实例
     */
    public static EmptyFragment newInstance(String message) {
        EmptyFragment fragment = new EmptyFragment();
        // 创建 Bundle 并传入参数
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }
}
