package com.jeybell.sheetmusic.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeybell.sheetmusic.setlist.Setlist;
import com.jeybell.sheetmusic.setlist.SetlistRepository;
import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(AdminContentService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admincontent;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AdminContentServiceTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private SetlistRepository setlistRepository;
    @Autowired
    private AdminContentService service;

    @Test
    void 삭제된_곡_조회_후_복구() {
        Song active = new Song("살아있는곡", null, null, null, null);
        Song deleted = new Song("삭제된곡", "가수", null, null, null);
        em.persist(active);
        em.persist(deleted);
        deleted.softDelete();
        em.flush();
        em.clear();

        assertThat(service.getDeletedSongs()).extracting("title").containsExactly("삭제된곡");

        service.restoreSong(deleted.getSongId());
        em.flush();
        em.clear();

        assertThat(service.getDeletedSongs()).isEmpty();
        assertThat(songRepository.findById(deleted.getSongId()).orElseThrow().getDeletedAt()).isNull();
    }

    @Test
    void 삭제된_콘티_조회_후_복구() {
        Setlist deleted = new Setlist(LocalDate.of(2026, 7, 1), "삭제된콘티", null);
        em.persist(deleted);
        deleted.softDelete();
        em.flush();
        em.clear();

        assertThat(service.getDeletedSetlists()).extracting("title").containsExactly("삭제된콘티");

        service.restoreSetlist(deleted.getSetlistId());
        em.flush();
        em.clear();

        assertThat(service.getDeletedSetlists()).isEmpty();
        assertThat(setlistRepository.findById(deleted.getSetlistId()).orElseThrow().getDeletedAt()).isNull();
    }
}
