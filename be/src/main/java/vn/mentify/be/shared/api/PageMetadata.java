package vn.mentify.be.shared.api;

public record PageMetadata(
        int page,
        int size,
        long totalItems,
        long totalPages) {

    public PageMetadata {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be greater than or equal to zero");
        }
        long expectedTotalPages = totalItems == 0 ? 0 : ((totalItems - 1) / size) + 1;
        if (totalPages != expectedTotalPages) {
            throw new IllegalArgumentException("totalPages does not match page size and total items");
        }
    }

    public static PageMetadata of(int page, int size, long totalItems) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        if (totalItems < 0) {
            throw new IllegalArgumentException("totalItems must be greater than or equal to zero");
        }
        long totalPages = totalItems == 0 ? 0 : ((totalItems - 1) / size) + 1;
        return new PageMetadata(page, size, totalItems, totalPages);
    }
}
