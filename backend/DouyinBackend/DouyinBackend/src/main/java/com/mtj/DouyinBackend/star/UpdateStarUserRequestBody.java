package com.mtj.DouyinBackend.star;

import lombok.Data;

@Data
public class UpdateStarUserRequestBody {
    private Boolean specialFollow;
    private Boolean followed;
    private String remark;
}
