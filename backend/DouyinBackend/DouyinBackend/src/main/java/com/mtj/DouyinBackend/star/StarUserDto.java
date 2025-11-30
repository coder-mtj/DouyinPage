package com.mtj.DouyinBackend.star;

import lombok.Data;

@Data
public class StarUserDto {
    private Long id;
    private String name;
    private Integer avatarIndex;
    private Boolean verified;
    private Boolean followed;
    private String douyinId;
    private Boolean specialFollow;
    private String remark;
}
