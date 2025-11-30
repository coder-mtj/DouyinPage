package com.mtj.douyinpage.data.remote;

public class StarUserDto {
    private Long id;
    private String name;
    private Integer avatarIndex;
    private Boolean verified;
    private Boolean followed;
    private String douyinId;
    private Boolean specialFollow;
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAvatarIndex() {
        return avatarIndex;
    }

    public void setAvatarIndex(Integer avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public Boolean getFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }

    public String getDouyinId() {
        return douyinId;
    }

    public void setDouyinId(String douyinId) {
        this.douyinId = douyinId;
    }

    public Boolean getSpecialFollow() {
        return specialFollow;
    }

    public void setSpecialFollow(Boolean specialFollow) {
        this.specialFollow = specialFollow;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
