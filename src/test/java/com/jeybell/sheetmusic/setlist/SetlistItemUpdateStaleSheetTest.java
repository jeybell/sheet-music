package com.jeybell.sheetmusic.setlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jeybell.sheetmusic.setlist.dto.SetlistItemResponse;
import com.jeybell.sheetmusic.setlist.dto.SetlistItemUpdateRequest;
import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongSheet;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 스테일 song_sheet_id 참조(아이템의 song 과 다른 곡 소속, 또는 soft-delete 된 시트) 상태에서도
 * 악보 값을 바꾸지 않는 한 updateItem(memo/연주키/youtube 인라인 수정)이 성공해야 한다.
 * 곡 병합/중복 정리(#104) 후 콘티 아이템이 편집 불가가 되던 회귀를 방지한다.
 */
@DataJpaTest
@Import(SetlistItemService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:stale;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SetlistItemUpdateStaleSheetTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private SetlistItemService service;

    private Song song(String title) {
        Song s = new Song(title, null, null, null, null);
        em.persist(s);
        return s;
    }

    private SongSheet sheet(Song song, String key) {
        SongSheet sh = new SongSheet(key, null, null);
        song.addSheet(sh);
        em.persist(sh);
        return sh;
    }

    private Long persistItem(Song song, SongSheet sheet) {
        Setlist setlist = new Setlist(LocalDate.of(2026, 7, 1), "여름캠프", null);
        SetlistItem item = new SetlistItem(song, sheet, 1, "old");
        setlist.addItem(item);
        em.persist(setlist);
        em.flush();
        Long id = item.getSetlistItemId();
        em.clear();
        return id;
    }

    @Test
    void 다른곡_소속_스테일시트여도_현재값_재전송이면_수정_성공() {
        Song a = song("승리하였네");
        Song b = song("다른 곡");
        SongSheet bSheet = sheet(b, "G");           // B 곡 소속 → 아이템(song=A) 기준 스테일
        Long itemId = persistItem(a, bSheet);

        SetlistItemResponse res = service.updateItem(itemId,
                new SetlistItemUpdateRequest(bSheet.getSongSheetId(), 1, "새 메모", "A", null));

        assertThat(res.memo()).isEqualTo("새 메모");
        assertThat(res.performanceKey()).isEqualTo("A");
        assertThat(res.songSheetId()).isEqualTo(bSheet.getSongSheetId());
    }

    @Test
    void soft삭제된_시트여도_현재값_재전송이면_수정_성공() {
        Song a = song("갈보리 산 위에");
        SongSheet aSheet = sheet(a, "A");
        aSheet.softDelete();                        // 아이템이 가리키는 시트가 soft-delete 됨
        Long itemId = persistItem(a, aSheet);

        SetlistItemResponse res = service.updateItem(itemId,
                new SetlistItemUpdateRequest(aSheet.getSongSheetId(), 1, "새 메모", null, null));

        assertThat(res.memo()).isEqualTo("새 메모");
    }

    @Test
    void 다른곡_시트로_실제_변경하려_하면_400_검증오류() {
        Song a = song("곡A");
        SongSheet aSheet = sheet(a, "C");           // 아이템의 현재(정상) 시트
        Song b = song("곡B");
        SongSheet bSheet = sheet(b, "G");           // 바꾸려는 대상: 다른 곡 소속
        Long itemId = persistItem(a, aSheet);

        // 현재값(aSheet)과 다른 값(bSheet)으로 변경 시도 → 소유권 검증 → IllegalArgumentException(=400)
        assertThatThrownBy(() -> service.updateItem(itemId,
                new SetlistItemUpdateRequest(bSheet.getSongSheetId(), 1, "메모", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 정상_시트_변경은_성공() {
        Song a = song("곡A");
        SongSheet c = sheet(a, "C");
        SongSheet g = sheet(a, "G");                // 같은 곡의 다른 활성 시트로 변경
        Long itemId = persistItem(a, c);

        SetlistItemResponse res = service.updateItem(itemId,
                new SetlistItemUpdateRequest(g.getSongSheetId(), 1, "메모", null, null));

        assertThat(res.songSheetId()).isEqualTo(g.getSongSheetId());
    }

    @Test
    void 시트를_null로_해제하는_변경도_성공() {
        Song a = song("곡A");
        SongSheet c = sheet(a, "C");
        Long itemId = persistItem(a, c);

        SetlistItemResponse res = service.updateItem(itemId,
                new SetlistItemUpdateRequest(null, 1, "메모", null, null));

        assertThat(res.songSheetId()).isNull();
    }
}
