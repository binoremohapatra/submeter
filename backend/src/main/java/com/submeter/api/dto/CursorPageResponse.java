package com.submeter.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Standard envelope for cursor-based pagination.
 *
 * <p>Cursor pagination is required for performance on large datasets.
 * Offset pagination (LIMIT/OFFSET) scans and discards rows, getting slower
 * as the offset increases. Cursor pagination uses index seeks.
 */
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class CursorPageResponse<T> {
    /** The paginated data items. */
    private List<T> content;
    
    /** The cursor to pass as 'after' to fetch the next page. Null if no more pages. */
    private String nextCursor;
    
    /** True if there is a subsequent page of results. */
    private boolean hasNext;

    // Getters and Setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    // Builder
    public static <T> CursorPageResponseBuilder<T> builder() {
        return new CursorPageResponseBuilder<T>();
    }

    public static class CursorPageResponseBuilder<T> {
        private List<T> content;
        private String nextCursor;
        private boolean hasNext;

        public CursorPageResponseBuilder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public CursorPageResponseBuilder<T> nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public CursorPageResponseBuilder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public CursorPageResponse<T> build() {
            CursorPageResponse<T> response = new CursorPageResponse<>();
            response.setContent(content);
            response.setNextCursor(nextCursor);
            response.setHasNext(hasNext);
            return response;
        }
    }
}
