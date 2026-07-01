package com.jeybell.sheetmusic.setlist;

import static org.assertj.core.api.Assertions.assertThat;

import com.jeybell.sheetmusic.setlist.dto.SetlistListRow;
import com.jeybell.sheetmusic.setlist.dto.SetlistResponse;
import com.jeybell.sheetmusic.song.Song;
import com.jeybell.sheetmusic.song.SongLink;
import com.jeybell.sheetmusic.song.SongSheet;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * #85 콘티 목록 조회 성능 개선 검증.
 * 경량 프로젝션 조회가 Song EAGER tags/links 서브셀렉트를 발생시키지 않고(단일 쿼리),
 * 콘티 단위 그룹핑/정렬이 올바른지 확인한다.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:setlist;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class SetlistListQueryTest {

    @Autowired
    private SetlistRepository setlistRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 목록조회는_단일쿼리로_그룹핑되고_정렬된다() {
        // given: 곡 A(태그·링크 보유) + 악보, 곡 B
        Song songA = new Song("은혜", "예배자", null, null, null);
        songA.updateTags(List.of("찬양", "빠른곡"));
        songA.addLink(new SongLink(songA, "YouTube", "https://youtu.be/abc", 0));
        SongSheet sheetA = new SongSheet("G", "원키", null);
        songA.addSheet(sheetA);
        entityManager.persist(songA);

        Song songB = new Song("주 은혜임을", "회중", null, null, null);
        entityManager.persist(songB);

        // 콘티1(오늘): 아이템 2개 (순서 역순으로 추가해도 orderNo asc 로 정렬되어야 함)
        Setlist today = new Setlist(LocalDate.of(2026, 7, 1), "주일예배", "주일 콘티", null);
        today.addItem(new SetlistItem(songB, null, 2, "2번곡"));
        today.addItem(new SetlistItem(songA, sheetA, 1, "1번곡"));
        entityManager.persist(today);

        // 콘티2(어제): 아이템 없음 → 빈 items 로 조회되어야 함
        Setlist yesterday = new Setlist(LocalDate.of(2026, 6, 30), "새벽예배", "새벽 콘티", null);
        entityManager.persist(yesterday);

        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        // when
        List<SetlistListRow> rows = setlistRepository.findAllActiveForList();
        List<SetlistResponse> result = group(rows);

        // then: 서브셀렉트 없이 단일 쿼리
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);

        // then: 최신순 정렬 (오늘 콘티가 먼저)
        assertThat(result).hasSize(2);
        SetlistResponse first = result.get(0);
        assertThat(first.title()).isEqualTo("주일 콘티");
        assertThat(first.items()).hasSize(2);
        // orderNo asc 정렬
        assertThat(first.items().get(0).orderNo()).isEqualTo(1);
        assertThat(first.items().get(0).songTitle()).isEqualTo("은혜");
        assertThat(first.items().get(0).sheetKey()).isEqualTo("G");
        assertThat(first.items().get(0).versionName()).isEqualTo("원키");
        assertThat(first.items().get(1).orderNo()).isEqualTo(2);
        assertThat(first.items().get(1).songTitle()).isEqualTo("주 은혜임을");
        assertThat(first.items().get(1).songSheetId()).isNull();

        // then: 아이템 없는 콘티는 빈 리스트
        SetlistResponse second = result.get(1);
        assertThat(second.title()).isEqualTo("새벽 콘티");
        assertThat(second.items()).isEmpty();
    }

    private List<SetlistResponse> group(List<SetlistListRow> rows) {
        Map<Long, List<SetlistListRow>> grouped = new LinkedHashMap<>();
        for (SetlistListRow row : rows) {
            grouped.computeIfAbsent(row.setlistId(), key -> new ArrayList<>()).add(row);
        }
        return grouped.values().stream().map(SetlistResponse::fromRows).toList();
    }
}
