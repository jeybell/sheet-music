package com.jeybell.sheetmusic.song;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeybell.sheetmusic.setlist.Setlist;
import com.jeybell.sheetmusic.setlist.SetlistItem;
import com.jeybell.sheetmusic.song.dto.SongMergeResponse;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 곡 병합 검증: 악보 이동 + 키/파일 중복 제거 + 콘티 참조 재지정 + 원본 soft delete.
 */
@DataJpaTest
@Import(SongMergeService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:merge;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SongMergeServiceTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private SongMergeService mergeService;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private SongSheetRepository songSheetRepository;

    private Song song(String title) {
        Song s = new Song(title, null, null, null, null);
        em.persist(s);
        return s;
    }

    private SongSheet sheet(Song song, String key, String... fileNames) {
        SongSheet sh = new SongSheet(key, null, null);
        song.addSheet(sh);
        em.persist(sh);
        for (String fn : fileNames) {
            em.persist(new SongFile(sh, fn, "stored-" + fn, "path/" + fn, "application/pdf", 1L));
        }
        return sh;
    }

    @Test
    void 악보를_옮기고_키_파일_동일_중복은_제거한다() {
        Song target = song("곡");
        sheet(target, "G", "a.pdf");

        Song source = song("곡");
        sheet(source, "A", "b.pdf");     // 고유 → 유지
        sheet(source, "G", "a.pdf");     // target 의 G/a.pdf 와 동일 → 제거 대상

        em.flush();
        em.clear();

        SongMergeResponse res = mergeService.merge(target.getSongId(), List.of(source.getSongId()), true);

        assertThat(res.mergedSongCount()).isEqualTo(1);
        assertThat(res.movedSheetCount()).isEqualTo(2);
        assertThat(res.removedDuplicateSheetCount()).isEqualTo(1);

        em.flush();
        em.clear();

        List<SongSheet> targetSheets =
                songSheetRepository.findAllBySongSongIdAndDeletedAtIsNullOrderBySongSheetIdAsc(target.getSongId());
        assertThat(targetSheets).extracting(SongSheet::getSheetKey).containsExactlyInAnyOrder("G", "A");
        assertThat(songRepository.findActiveBySongIdWithSheets(source.getSongId())).isEmpty();
    }

    @Test
    void 콘티_아이템이_대상곡으로_재지정된다() {
        Song target = song("곡");
        sheet(target, "G", "a.pdf");

        Song source = song("곡");
        SongSheet sourceSheet = sheet(source, "A", "b.pdf");

        Setlist setlist = new Setlist(LocalDate.of(2026, 7, 1), "콘티", null);
        setlist.addItem(new SetlistItem(source, sourceSheet, 1, null));
        em.persist(setlist);

        em.flush();
        em.clear();

        mergeService.merge(target.getSongId(), List.of(source.getSongId()), true);

        em.flush();
        em.clear();

        SetlistItem item = em.createQuery("select i from SetlistItem i", SetlistItem.class)
                .getResultList().get(0);
        assertThat(item.getSong().getSongId()).isEqualTo(target.getSongId());
        // A/b.pdf 는 고유하므로 중복 제거되지 않고 그대로 대상곡 소속으로 유지
        assertThat(item.getSongSheet().getSongSheetId()).isEqualTo(sourceSheet.getSongSheetId());
    }
}
