package com.mtj.douyinpage.adapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.mtj.douyinpage.ui.EmptyFragment;
import com.mtj.douyinpage.ui.FollowingFragment;

/**
 * ViewPager2 适配器
 * 
 * 职责：
 * - 管理四个标签页对应的 Fragment
 * - 根据位置创建相应的 Fragment 实例
 * - 支持页面之间的左右滑动
 */
public class FollowingPagerAdapter extends FragmentStateAdapter {
    /**
     * 构造函数
     * 
     * @param activity AppCompatActivity 实例
     */
    public FollowingPagerAdapter(AppCompatActivity activity) {
        super(activity);
    }

    /**
     * 获取页面总数
     * 四个标签页：互关、关注、粉丝、朋友
     * 
     * @return 页面总数 4
     */
    @Override
    public int getItemCount() {
        return 4;
    }

    /**
     * 根据位置创建对应的 Fragment
     * 
     * @param position 页面位置
     *                 0 - 互关（空状态）
     *                 1 - 关注（用户列表）
     *                 2 - 粉丝（空状态）
     *                 3 - 朋友（空状态）
     * @return 对应位置的 Fragment 实例
     */
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // 互关标签页 - 显示空状态
                return EmptyFragment.newInstance("暂无互关");
            case 1:
                // 关注标签页 - 显示用户列表
                return new FollowingFragment();
            case 2:
                // 粉丝标签页 - 显示空状态
                return EmptyFragment.newInstance("暂无粉丝");
            case 3:
                // 朋友标签页 - 显示空状态
                return EmptyFragment.newInstance("暂无朋友");
            default:
                // 默认返回空 Fragment
                return new Fragment();
        }
    }
}
