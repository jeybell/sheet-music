package com.jeybell.sheetmusic.song;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

/**
 * #82 곡 목록 페이지네이션 검증.
 * exists/member of 를 포함한 JPQL 의 count 쿼리 자동 파생과 페이지 분할이 정상 동작하는지 확인한다.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:songpage;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SongSearchPaginationTest {

    @Autowired
    private SongRepository songRepository;

    private Song persist(String title, String artist, List<String> tags, String sheetKey) {
        Song song = new Song(title, artist, null, null, null);
        if (tags != null) song.updateTags(tags);
        if (sheetKey != null) song.addSheet(new SongSheet(sheetKey, null, null));
        return songRepository.save(song);
    }

    @Test
    void 페이지_분할과_총건수가_정확하다() {
        persist("곡1", "A", List.of("찬양"), "G");
        persist("곡2", "B", List.of("경배"), "C");
        persist("곡3", "C", null, null);

        var sort = Sort.by(Sort.Direction.DESC, "createdAt");

        Page<Song> first = songRepository.searchSongs(null, null, null, PageRequest.of(0, 2, sort));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).hasSize(2);
        assertThat(first.hasNext()).isTrue();

        Page<Song> second = songRepository.searchSongs(null, null, null, PageRequest.of(1, 2, sort));
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void 태그_및_키_필터가_동작한다() {
        persist("곡1", "A", List.of("찬양"), "G");
        persist("곡2", "B", List.of("경배"), "C");
        persist("곡3", "C", null, null);

        var page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThat(songRepository.searchSongs(null, null, "찬양", page).getTotalElements()).isEqualTo(1);
        // songKey 는 서비스에서 소문자 %..% 형태로 정규화되어 전달됨
        assertThat(songRepository.searchSongs(null, "%g%", null, page).getTotalElements()).isEqualTo(1);
        assertThat(songRepository.searchSongs("%곡%", null, null, page).getTotalElements()).isEqualTo(3);
    }
}
