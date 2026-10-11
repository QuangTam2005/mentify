package vn.mentify.be;

import org.junit.jupiter.api.Test;
import vn.mentify.be.shared.api.PageMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageMetadataTest {

    @Test
    void roundsUpTotalPagesForPartialLastPage() {
        assertThat(PageMetadata.of(0, 20, 31).totalPages()).isEqualTo(2);
        assertThat(PageMetadata.of(1, 20, 31).totalPages()).isEqualTo(2);
    }

    @Test
    void emptyResultHasZeroTotalPages() {
        assertThat(PageMetadata.of(0, 20, 0).totalPages()).isZero();
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThatThrownBy(() -> PageMetadata.of(0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
