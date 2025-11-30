package com.mtj.douyinpage.data.remote;

import java.util.List;

public class StarUserPageResponse {
    private java.util.List<StarUserDto> content;
    private long total;
    private int page;
    private int size;

    public List<StarUserDto> getContent() {
        return content;
    }

    public void setContent(List<StarUserDto> content) {
        this.content = content;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
