package com.mtj.DouyinBackend.star;

import lombok.Data;

import java.util.List;

@Data
public class StarUserPageResponse {
    private List<StarUserDto> content;
    private long total;
    private int page;
    private int size;
}
