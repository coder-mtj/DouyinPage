package com.mtj.douyinpage.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.mtj.douyinpage.R;
import com.mtj.douyinpage.adapter.FollowingAdapter;
import com.mtj.douyinpage.data.UserStorage;
import com.mtj.douyinpage.databinding.FragmentFollowingBinding;
import com.mtj.douyinpage.model.User;
import java.util.ArrayList;
import java.util.List;

/**
 * 关注列表 Fragment
 * 
 * 职责：
 * - 显示用户关注列表
 * - 管理 RecyclerView 和适配器
 * - 处理用户列表项的点击事件（三点菜单）
 * - 显示关注数量统计
 */
public class FollowingFragment extends Fragment {
    // ViewBinding 对象，用于访问布局中的所有视图
    private FragmentFollowingBinding binding;
    private List<User> users;
    private FollowingAdapter adapter;

    /**
     * 创建 Fragment 视图
     * 使用 ViewBinding 初始化布局
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 使用 ViewBinding 初始化布局
        binding = FragmentFollowingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * 视图创建完成后调用
     * 初始化 RecyclerView 和数据
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 设置 RecyclerView
        setupRecyclerView();
        // 监听底部弹窗返回的结果
        setupFragmentResultListener();
    }

    /**
     * 设置 RecyclerView
     * - 创建用户数据列表
     * - 创建适配器并绑定到 RecyclerView
     * - 设置布局管理器为线性布局
     * - 更新关注数量文本
     */
    private void setupRecyclerView() {
        users = UserStorage.loadUsers(requireContext());
        if (users.isEmpty()) {
            users = createDefaultUsers();
            persistUsers();
        }

        // 创建适配器，并传入三点菜单点击回调
        adapter = new FollowingAdapter(users, user -> {
            // 点击三点菜单时，显示底部菜单
            showMoreOptionsBottomSheet(user);
        }, changedUser -> persistUsers());

        // 配置 RecyclerView
        // 设置布局管理器为线性布局（垂直排列）
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 设置适配器
        binding.recyclerView.setAdapter(adapter);

        // 更新关注数量文本
        updateFollowingCount();
    }

    /**
     * 监听底部弹窗返回的用户状态更新
     */
    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener(MoreOptionsBottomSheet.RESULT_KEY,
                this,
                (requestKey, result) -> {
                    int userId = result.getInt(MoreOptionsBottomSheet.RESULT_USER_ID, -1);
                    if (userId == -1) {
                        return;
                    }
                    for (int i = 0; i < users.size(); i++) {
                        User target = users.get(i);
                        if (target.getId() == userId) {
                            boolean special = result.getBoolean(MoreOptionsBottomSheet.RESULT_SPECIAL, target.isSpecialFollow());
                            boolean followed = result.getBoolean(MoreOptionsBottomSheet.RESULT_FOLLOWED, target.isFollowed());
                            String remark = result.getString(MoreOptionsBottomSheet.RESULT_REMARK, target.getRemark());
                            target.setSpecialFollow(special);
                            target.setFollowed(followed);
                            target.setRemark(remark);
                            adapter.notifyItemChanged(i);
                            persistUsers();
                            break;
                        }
                    }
                });
    }

    /**
     * 显示底部菜单
     * 当用户点击三点菜单按钮时调用
     * 
     * @param user 被点击的用户对象
     */
    private void showMoreOptionsBottomSheet(User user) {
        // 创建底部菜单 Fragment 实例
        MoreOptionsBottomSheet bottomSheet = MoreOptionsBottomSheet.newInstance(user);
        // 显示底部菜单
        bottomSheet.show(getParentFragmentManager(), "MoreOptions");
    }

    /**
     * 默认关注列表
     */
    private List<User> createDefaultUsers() {
        List<User> defaults = new ArrayList<>();
        defaults.add(new User(1, "周杰伦", R.drawable.avatar_1, true, true, "JayChou001"));
        defaults.add(new User(2, "王心凌", R.drawable.avatar_2, true, true, "CyndiWang905"));
        defaults.add(new User(3, "张韶涵", R.drawable.avatar_3, true, true, "AngelaCMusic"));
        defaults.add(new User(4, "张靓颖", R.drawable.avatar_4, true, true, "JaneZhangMusic"));
        defaults.add(new User(5, "五月天", R.drawable.avatar_5, true, true, "MaydayLive"));
        defaults.add(new User(6, "林俊杰", R.drawable.avatar_6, true, true, "JJLinStudio"));
        defaults.add(new User(7, "薛之谦", R.drawable.avatar_7, true, true, "JokerXueOfficial"));
        defaults.add(new User(8, "刘德华", R.drawable.avatar_8, true, true, "AndyLauLegend"));
        defaults.add(new User(9, "杨幂", R.drawable.avatar_9, true, true, "MiniYang888"));
        defaults.add(new User(10, "赵丽颖", R.drawable.avatar_10, true, true, "ZhaoLiyingStar"));
        defaults.add(new User(11, "邓紫棋", R.drawable.avatar_11, true, true, "GEM-Deng"));
        defaults.add(new User(12, "陈奕迅", R.drawable.avatar_12, true, true, "EasonStyle"));
        defaults.add(new User(13, "易烊千玺", R.drawable.avatar_13, true, true, "JacksonYeeTF"));
        defaults.add(new User(14, "王俊凯", R.drawable.avatar_14, true, true, "KarryWangTF"));
        defaults.add(new User(15, "蔡依林", R.drawable.avatar_15, true, true, "JolinDance"));
        return defaults;
    }

    private void persistUsers() {
        UserStorage.saveUsers(requireContext(), users);
    }

    private void updateFollowingCount() {
        binding.followingCountText.setText(String.format("我的关注(%d人)", users.size()));
    }
}
