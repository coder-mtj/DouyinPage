package com.mtj.douyinpage.model;

/**
 * 用户数据模型
 * 
 * 用于表示关注列表中的用户信息
 */
import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // 用户 ID
    private int id;
    // 用户名
    private String name;
    // 用户头像资源 ID（drawable resource id）
    private int avatar;
    // 是否认证（显示金色 V 图标）
    private boolean isVerified;
    // 是否已关注（显示"已关注"按钮）
    private boolean isFollowed;
    // 抖音号
    private String douyinId;
    // 是否特别关注
    private boolean isSpecialFollow;
    // 备注名
    private String remark;

    /**
     * 构造函数
     * 
     * @param id 用户 ID
     * @param name 用户名
     * @param avatar 用户头像资源 ID
     * @param isVerified 是否认证
     * @param isFollowed 是否已关注
     */
    public User(int id, String name, int avatar, boolean isVerified, boolean isFollowed) {
        this(id, name, avatar, isVerified, isFollowed, "", false, "");
    }

    public User(int id, String name, int avatar, boolean isVerified, boolean isFollowed, String douyinId) {
        this(id, name, avatar, isVerified, isFollowed, douyinId, false, "");
    }

    public User(int id, String name, int avatar, boolean isVerified, boolean isFollowed,
                String douyinId, boolean isSpecialFollow, String remark) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.isVerified = isVerified;
        this.isFollowed = isFollowed;
        this.douyinId = douyinId;
        this.isSpecialFollow = isSpecialFollow;
        this.remark = remark;
    }

    // Getter 和 Setter 方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAvatar() {
        return avatar;
    }

    public void setAvatar(int avatar) {
        this.avatar = avatar;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isFollowed() {
        return isFollowed;
    }

    public void setFollowed(boolean followed) {
        isFollowed = followed;
    }

    public String getDouyinId() {
        return douyinId;
    }

    public void setDouyinId(String douyinId) {
        this.douyinId = douyinId;
    }

    public boolean isSpecialFollow() {
        return isSpecialFollow;
    }

    public void setSpecialFollow(boolean specialFollow) {
        isSpecialFollow = specialFollow;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
