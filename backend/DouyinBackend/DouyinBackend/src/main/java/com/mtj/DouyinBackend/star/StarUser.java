package com.mtj.DouyinBackend.star;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StarUser {
    private Long id;
    private String name;
    private Integer avatarIndex;
    private Boolean verified;
    private Boolean followed;
    private String douyinId;
    private Boolean specialFollow;
    private String remark;
}
