package com.example.backend.Post;

import java.util.List;

public class CursorPageResponse<T> {
    private List<T> content;
    private Long nextCursor; // ID của bài viết cuối cùng trong danh sách này
    private boolean hasNext; // Cờ báo cho Front-end biết còn bài để tải không

    public CursorPageResponse(List<T> content, Long nextCursor, boolean hasNext) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    // Getter và Setter
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public Long getNextCursor() { return nextCursor; }
    public void setNextCursor(Long nextCursor) { this.nextCursor = nextCursor; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}