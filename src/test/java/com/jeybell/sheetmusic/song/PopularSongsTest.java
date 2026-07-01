package com.jeybell.sheetmusic.song;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeybell.sheetmusic.setlist.Setlist;
import com.jeybell.sheetmusic.setlist.SetlistItem;
import com.jeybell.sheetmusic.song.dto.PopularSongResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 * #76 홈 대시보드 - 자주 쓰는 곡 집계 검증.
 * 활성 콘티의 setlist_items 참조 횟수 내림차순, 삭제된 콘티는 제외.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:popular;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class PopularSongsTest {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private EntityManager em;

    private Song persistSong(String title) {
        Song song = new Song(title, null, null, null, null);
        em.persist(song);
        return song;
    }

    private Setlist persistSetlist(boolean deleted, Song... songs) {
        Setlist setlist = new Setlist(LocalDate.of(2026, 7, 1), "콘티", null);
        int order = 1;
        for (Song s : songs) {
            setlist.addItem(new SetlistItem(s, null, order++, null));
        }
        if (deleted) setlist.softDelete();
        em.persist(setlist);
        return setlist;
    }

    @Test
    void 참조_횟수_내림차순으로_집계되고_삭제콘티는_제외된다() {
        Song a = persistSong("자주쓰는곡");
        Song b = persistSong("가끔쓰는곡");
        Song c = persistSong("안쓰는곡");
        persistSong("아예없는곡"); // 콘티에 미포함 → 결과 없음

        persistSetlist(false, a, b);
        persistSetlist(false, a, c);
        persistSetlist(false, a);        // a: 활성 3회
        persistSetlist(true, b);         // 삭제 콘티의 b 는 제외
        em.flush();
        em.clear();

        var result = songRepository.findPopularSongs(PageRequest.of(0, 5));

        assertThat(result).extracting(PopularSongResponse::title)
                .containsExactly("자주쓰는곡", "가끔쓰는곡", "안쓰는곡");
        assertThat(result.get(0).usageCount()).isEqualTo(3);
        assertThat(result.get(1).usageCount()).isEqualTo(1); // b 는 삭제 콘티 제외 후 1회
        assertThat(result.get(2).usageCount()).isEqualTo(1);
    }

    @Test
    void limit_로_상위_N개만_반환한다() {
        Song a = persistSong("A");
        Song b = persistSong("B");
        persistSetlist(false, a, b);
        em.flush();
        em.clear();

        assertThat(songRepository.findPopularSongs(PageRequest.of(0, 1))).hasSize(1);
    }
}
