package com.mtj.douyinpage.data.remote;

public class UpdateStarUserRequestBody {
    private Boolean specialFollow;
    private Boolean followed;
    private String remark;

    public Boolean getSpecialFollow() {
        return specialFollow;
    }

    public void setSpecialFollow(Boolean specialFollow) {
        this.specialFollow = specialFollow;
    }

    public Boolean getFollowed() {
        return followed;
    }

    public void setFollowed(Boolean followed) {
        this.followed = followed;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
