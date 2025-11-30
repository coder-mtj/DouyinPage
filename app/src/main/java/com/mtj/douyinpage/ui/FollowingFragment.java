package com.mtj.douyinpage.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mtj.douyinpage.R;
import com.mtj.douyinpage.adapter.FollowingAdapter;
import com.mtj.douyinpage.data.UserStorage;
import com.mtj.douyinpage.databinding.FragmentFollowingBinding;
import com.mtj.douyinpage.model.User;
import com.mtj.douyinpage.data.remote.ApiClient;
import com.mtj.douyinpage.data.remote.StarUserApi;
import com.mtj.douyinpage.data.remote.StarUserDto;
import com.mtj.douyinpage.data.remote.StarUserPageResponse;
import com.mtj.douyinpage.data.remote.UpdateStarUserRequestBody;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private static final String TAG = "FollowingFragment";
    // ViewBinding 对象，用于访问布局中的所有视图
    private FragmentFollowingBinding binding;
    private List<User> users;
    private FollowingAdapter adapter;
    private StarUserApi starUserApi;
    private static final int PAGE_SIZE = 10;
    private int currentPage = -1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private long totalCount = 0L;

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
        users = new ArrayList<>();

        // 创建适配器，并传入三点菜单点击回调
        adapter = new FollowingAdapter(users, user -> {
            // 点击三点菜单时，显示底部菜单
            showMoreOptionsBottomSheet(user);
        }, changedUser -> {
            persistUsers();
            updateUserOnBackend(changedUser);
        });

        // 配置 RecyclerView
        // 设置布局管理器为线性布局（垂直排列）
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 设置适配器
        binding.recyclerView.setAdapter(adapter);

        // 滚动到底部时加载下一页
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1) && hasMore && !isLoading) {
                    loadNextPage();
                }
            }
        });

        // 设置下拉刷新监听器
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            // 下拉刷新时重新加载第一页数据
            refreshData();
        });

        // 从后端加载用户列表，失败时回退到本地缓存
        loadUsersFromBackendOrLocal();
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
                            boolean special = result.getBoolean(MoreOptionsBottomSheet.RESULT_SPECIAL,
                                    target.isSpecialFollow());
                            boolean followed = result.getBoolean(MoreOptionsBottomSheet.RESULT_FOLLOWED,
                                    target.isFollowed());
                            String remark = result.getString(MoreOptionsBottomSheet.RESULT_REMARK, target.getRemark());
                            target.setSpecialFollow(special);
                            target.setFollowed(followed);
                            target.setRemark(remark);
                            adapter.notifyItemChanged(i);
                            persistUsers();
                            updateUserOnBackend(target);
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
        long displayCount = totalCount > 0 ? totalCount : users.size();
        binding.followingCountText.setText(String.format("我的关注(%d人)", displayCount));
    }

    private void loadUsersFromBackendOrLocal() {
        if (starUserApi == null) {
            starUserApi = ApiClient.getStarUserApi();
        }
        currentPage = -1;
        hasMore = true;
        totalCount = 0L;
        loadPage(0, true);
    }

    private void loadNextPage() {
        if (!hasMore || isLoading) {
            return;
        }
        int nextPage = currentPage + 1;
        loadPage(nextPage, false);
    }

    private void loadPage(final int page, final boolean clearBefore) {
        if (starUserApi == null) {
            starUserApi = ApiClient.getStarUserApi();
        }
        isLoading = true;
        Call<StarUserPageResponse> call = starUserApi.list(page, PAGE_SIZE);
        call.enqueue(new Callback<StarUserPageResponse>() {
            @Override
            public void onResponse(Call<StarUserPageResponse> call, Response<StarUserPageResponse> response) {
                if (!isAdded()) {
                    isLoading = false;
                    binding.swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    StarUserPageResponse body = response.body();
                    List<StarUserDto> dtos = body.getContent();
                    List<User> mapped = mapRemoteUsers(dtos);
                    Log.d(TAG, "loadPage success: page=" + page
                            + ", receivedDtos=" + (dtos == null ? 0 : dtos.size())
                            + ", mappedUsers=" + mapped.size()
                            + ", total=" + body.getTotal()
                            + ", users=" + mapped);
                    if (clearBefore) {
                        users.clear();
                    }
                    int oldSize = users.size();
                    users.addAll(mapped);
                    if (clearBefore) {
                        adapter.notifyDataSetChanged();
                    } else {
                        adapter.notifyItemRangeInserted(oldSize, mapped.size());
                    }
                    totalCount = body.getTotal();
                    currentPage = page;
                    hasMore = !mapped.isEmpty() && (long) (currentPage + 1) * PAGE_SIZE < totalCount;
                    persistUsers();
                    updateFollowingCount();
                } else {
                    Log.e(TAG, "loadUsersFromBackendOrLocal: response not successful, code=" + response.code());
                    Toast.makeText(requireContext(), "加载关注列表失败", Toast.LENGTH_SHORT).show();
                }
                isLoading = false;
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<StarUserPageResponse> call, Throwable t) {
                if (!isAdded()) {
                    isLoading = false;
                    binding.swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                Log.e(TAG, "loadUsersFromBackendOrLocal: failed", t);
                Toast.makeText(requireContext(), "网络异常，无法加载关注列表", Toast.LENGTH_SHORT).show();
                isLoading = false;
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * 刷新数据
     * 下拉刷新时调用，重新加载第一页数据
     */
    private void refreshData() {
        currentPage = -1;
        hasMore = true;
        totalCount = 0L;
        loadPage(0, true);
    }

    private List<User> mapRemoteUsers(List<StarUserDto> dtoList) {
        List<User> result = new ArrayList<>();
        if (dtoList == null) {
            return result;
        }
        for (StarUserDto dto : dtoList) {
            if (dto == null || dto.getId() == null) {
                continue;
            }
            int avatarResId = getAvatarResId(dto.getAvatarIndex());
            User user = new User(
                    dto.getId().intValue(),
                    dto.getName(),
                    avatarResId,
                    Boolean.TRUE.equals(dto.getVerified()),
                    Boolean.TRUE.equals(dto.getFollowed()),
                    dto.getDouyinId(),
                    Boolean.TRUE.equals(dto.getSpecialFollow()),
                    dto.getRemark() == null ? "" : dto.getRemark());
            result.add(user);
        }
        return result;
    }

    private int getAvatarResId(Integer avatarIndex) {
        int index = (avatarIndex == null || avatarIndex <= 0) ? 1 : avatarIndex;
        switch (index) {
            case 1:
                return R.drawable.avatar_1;
            case 2:
                return R.drawable.avatar_2;
            case 3:
                return R.drawable.avatar_3;
            case 4:
                return R.drawable.avatar_4;
            case 5:
                return R.drawable.avatar_5;
            case 6:
                return R.drawable.avatar_6;
            case 7:
                return R.drawable.avatar_7;
            case 8:
                return R.drawable.avatar_8;
            case 9:
                return R.drawable.avatar_9;
            case 10:
                return R.drawable.avatar_10;
            case 11:
                return R.drawable.avatar_11;
            case 12:
                return R.drawable.avatar_12;
            case 13:
                return R.drawable.avatar_13;
            case 14:
                return R.drawable.avatar_14;
            case 15:
                return R.drawable.avatar_15;
            default:
                return R.drawable.avatar_1;
        }
    }

    private void updateUserOnBackend(User user) {
        if (user == null) {
            return;
        }
        if (starUserApi == null) {
            starUserApi = ApiClient.getStarUserApi();
        }
        UpdateStarUserRequestBody body = new UpdateStarUserRequestBody();
        body.setSpecialFollow(user.isSpecialFollow());
        body.setFollowed(user.isFollowed());
        body.setRemark(user.getRemark());
        Call<Void> call = starUserApi.updateUser(user.getId(), body);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "updateUserOnBackend: failed, code=" + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "updateUserOnBackend: error", t);
            }
        });
    }
}
