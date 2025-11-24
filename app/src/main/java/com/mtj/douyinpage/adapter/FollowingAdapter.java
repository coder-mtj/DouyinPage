package com.mtj.douyinpage.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.mtj.douyinpage.R;
import com.mtj.douyinpage.databinding.ItemFollowingBinding;
import com.mtj.douyinpage.model.User;
import java.util.List;

/**
 * 关注列表 RecyclerView 适配器
 * 
 * 职责：
 * - 管理用户列表数据
 * - 创建和绑定列表项视图
 * - 处理三点菜单按钮点击事件
 */
public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.ViewHolder> {
    // 用户数据列表
    private List<User> users;
    // 三点菜单点击回调
    private OnMoreClickListener onMoreClick;
    // 用户数据变更回调（用于持久化）
    private OnUserChangedListener onUserChanged;

    /**
     * 三点菜单点击回调接口
     */
    public interface OnMoreClickListener {
        void onMoreClick(User user);
    }

    public interface OnUserChangedListener {
        void onUserChanged(User user);
    }

    /**
     * 构造函数
     * 
     * @param users 用户列表
     * @param onMoreClick 三点菜单点击回调
     */
    public FollowingAdapter(List<User> users, OnMoreClickListener onMoreClick, OnUserChangedListener onUserChanged) {
        this.users = users;
        this.onMoreClick = onMoreClick;
        this.onUserChanged = onUserChanged;
    }

    /**
     * 创建列表项视图持有者
     * 当 RecyclerView 需要新的列表项时调用
     * 
     * @param parent 父容器
     * @param viewType 视图类型
     * @return 新创建的 ViewHolder
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 使用 ViewBinding 创建列表项布局
        ItemFollowingBinding binding = ItemFollowingBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    /**
     * 绑定数据到列表项视图
     * 当 RecyclerView 需要显示某个位置的数据时调用
     * 
     * @param holder 列表项视图持有者
     * @param position 数据位置
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // 绑定对应位置的用户数据
        holder.bind(users.get(position));
    }

    /**
     * 获取列表项总数
     * 
     * @return 用户列表的大小
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * 列表项视图持有者
     * 负责绑定单个用户数据到视图
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemFollowingBinding binding;

        /**
         * 构造函数
         * 
         * @param binding ViewBinding 对象
         */
        public ViewHolder(ItemFollowingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * 绑定用户数据到视图
         * 
         * @param user 要绑定的用户对象
         */
        public void bind(User user) {
            // 设置用户头像
            binding.userAvatar.setImageResource(user.getAvatar());
            // 设置用户名（优先显示备注）
            String displayName = TextUtils.isEmpty(user.getRemark()) ? user.getName() : user.getRemark();
            binding.userName.setText(displayName);
            // 根据是否认证显示/隐藏认证图标
            binding.verifiedIcon.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
            // 显示/隐藏特别关注标签
            binding.specialTag.setVisibility(user.isSpecialFollow() ? View.VISIBLE : View.GONE);
            // 初始化关注按钮外观
            updateFollowButton(user);
            // 点击按钮切换关注状态
            binding.followButton.setOnClickListener(v -> {
                user.setFollowed(!user.isFollowed());
                updateFollowButton(user);
                if (onUserChanged != null) {
                    onUserChanged.onUserChanged(user);
                }
            });
            // 设置三点菜单按钮点击监听
            binding.moreButton.setOnClickListener(v -> {
                // 触发回调，传入被点击的用户
                if (onMoreClick != null) {
                    onMoreClick.onMoreClick(user);
                }
            });
        }

        /**
         * 根据关注状态更新按钮样式
         */
        private void updateFollowButton(User user) {
            if (user.isFollowed()) {
                binding.followButton.setText("已关注");
                binding.followButton.setBackgroundResource(R.drawable.button_background);
                binding.followButton.setTextColor(Color.BLACK);
            } else {
                binding.followButton.setText("关注");
                binding.followButton.setBackgroundResource(R.drawable.button_follow_active);
                binding.followButton.setTextColor(Color.WHITE);
            }
        }
    }
}
