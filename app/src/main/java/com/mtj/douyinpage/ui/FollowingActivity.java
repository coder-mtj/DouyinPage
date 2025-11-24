package com.mtj.douyinpage.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mtj.douyinpage.adapter.FollowingPagerAdapter;
import com.mtj.douyinpage.databinding.ActivityFollowingBinding;

/**
 * 关注页面 Activity
 * 
 * 职责：
 * - 管理顶部栏（返回按钮）
 * - 动态生成四个标签文本（互关、关注、粉丝、朋友）到 TabLayout
 * - 管理 ViewPager2 和 TabLayout 的联动
 * - 初始化并显示关注列表页面
 */
public class FollowingActivity extends AppCompatActivity {
    // ViewBinding 对象，用于访问布局中的所有视图
    private ActivityFollowingBinding binding;

    /**
     * Activity 创建时调用
     * 初始化布局、设置顶部栏、配置 ViewPager
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 使用 ViewBinding 初始化布局
        binding = ActivityFollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置顶部栏（返回按钮）
        setupToolbar();
        // 设置 ViewPager2 和 TabLayout
        setupViewPager();
    }

    /**
     * 设置顶部栏
     * - 返回按钮：点击时关闭当前 Activity
     */
    private void setupToolbar() {
        binding.backButton.setOnClickListener(v -> {
            // 点击返回按钮，关闭 Activity
            finish();
        });
    }

    /**
     * 设置 ViewPager2 和 TabLayout
     * - 创建 ViewPager 适配器，管理四个 Fragment
     * - 使用 TabLayoutMediator 将 TabLayout 和 ViewPager 绑定
     * - 设置初始选中页面为"关注"（第 1 个标签）
     */
    private void setupViewPager() {
        // 创建 ViewPager 适配器
        FollowingPagerAdapter adapter = new FollowingPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // 使用 TabLayoutMediator 将 TabLayout 和 ViewPager 联动
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            // 根据位置设置标签文本
            switch (position) {
                case 0:
                    tab.setText("互关");      // 第 0 页：互关
                    break;
                case 1:
                    tab.setText("关注");      // 第 1 页：关注（默认选中）
                    break;
                case 2:
                    tab.setText("粉丝");      // 第 2 页：粉丝
                    break;
                case 3:
                    tab.setText("朋友");      // 第 3 页：朋友
                    break;
            }
        }).attach();

        // 设置初始选中页面为"关注"（第 1 个标签）
        binding.viewPager.setCurrentItem(1);
    }
}
