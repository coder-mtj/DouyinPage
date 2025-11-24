package com.mtj.douyinpage.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mtj.douyinpage.databinding.BottomSheetMoreOptionsBinding;
import com.mtj.douyinpage.model.User;

/**
 * 底部菜单 BottomSheet
 * 
 * 职责：
 * - 显示用户列表项的更多选项菜单
 * - 处理特别关注、备注、取消关注等操作
 * - 通过 FragmentResult 将用户状态回传给列表
 */
public class MoreOptionsBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_USER = "arg_user";

    public static final String RESULT_KEY = "more_options_result";
    public static final String RESULT_USER_ID = "result_user_id";
    public static final String RESULT_SPECIAL = "result_special";
    public static final String RESULT_FOLLOWED = "result_followed";
    public static final String RESULT_REMARK = "result_remark";

    // ViewBinding 对象，用于访问布局中的所有视图
    private BottomSheetMoreOptionsBinding binding;
    private User user;
    private boolean isSpecialFollow;
    private boolean isFollowed;
    private String remark;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = BottomSheetMoreOptionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            Object obj = getArguments().getSerializable(ARG_USER);
            if (obj instanceof User) {
                user = (User) obj;
            }
        }
        if (user == null) {
            dismissAllowingStateLoss();
            return;
        }

        isSpecialFollow = user.isSpecialFollow();
        isFollowed = user.isFollowed();
        remark = user.getRemark();

        setupUserInfo();
        setupSpecialFollowSection();
        setupRemarkSection();
        setupCancelFollow();
        setupCloseButton();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    private void setupUserInfo() {
        binding.userNameText.setText(user.getName());
        String douyinText = TextUtils.isEmpty(user.getDouyinId()) ? "抖音号: 未设置" : "抖音号:" + user.getDouyinId();
        binding.userDouyinId.setText(douyinText);
        binding.remarkValue.setText(TextUtils.isEmpty(remark) ? "未设置" : remark);

        binding.copyDouyinButton.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(user.getDouyinId())) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("douyinId", user.getDouyinId());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "抖音号已复制", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "暂无抖音号可复制", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpecialFollowSection() {
        SwitchMaterial switchMaterial = binding.specialFollowSwitch;
        switchMaterial.setChecked(isSpecialFollow);
        switchMaterial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSpecialFollow = isChecked;
            emitResult(false);
        });
    }

    private void setupRemarkSection() {
        binding.remarkSection.setOnClickListener(v -> {
            final EditText editText = new EditText(requireContext());
            editText.setHint("请输入备注");
            editText.setText(remark);
            editText.setSelection(editText.getText().length());

            new AlertDialog.Builder(requireContext())
                    .setTitle("设置备注")
                    .setView(editText)
                    .setPositiveButton("保存", (dialog, which) -> {
                        remark = editText.getText().toString().trim();
                        binding.remarkValue.setText(TextUtils.isEmpty(remark) ? "未设置" : remark);
                        emitResult(false);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void setupCancelFollow() {
        binding.btnCancelFollow.setOnClickListener(v -> {
            isFollowed = false;
            emitResult(true);
        });
    }

    private void setupCloseButton() {
        binding.closeButton.setOnClickListener(v -> {
            emitResult(true);
        });
    }

    private void emitResult(boolean dismissAfter) {
        Bundle bundle = new Bundle();
        bundle.putInt(RESULT_USER_ID, user.getId());
        bundle.putBoolean(RESULT_SPECIAL, isSpecialFollow);
        bundle.putBoolean(RESULT_FOLLOWED, isFollowed);
        bundle.putString(RESULT_REMARK, remark);
        FragmentManager manager = getParentFragmentManager();
        manager.setFragmentResult(RESULT_KEY, bundle);
        if (dismissAfter) {
            dismiss();
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        emitResult(false);
    }

    /**
     * 工厂方法，创建 MoreOptionsBottomSheet 实例
     */
    public static MoreOptionsBottomSheet newInstance(User user) {
        MoreOptionsBottomSheet fragment = new MoreOptionsBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }
}
