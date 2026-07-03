package com.jeybell.sheetmusic.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jeybell.sheetmusic.auth.User;
import com.jeybell.sheetmusic.auth.UserRepository;
import com.jeybell.sheetmusic.auth.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(AdminUserService.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:adminuser;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class AdminUserServiceTest {

    @Autowired
    private EntityManager em;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminUserService service;

    private User user(String username) {
        User u = new User(username, "hash");
        em.persist(u);
        return u;
    }

    @Test
    void 신규_사용자_기본_역할은_USER() {
        User u = user("alice");
        em.flush();
        assertThat(u.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void 관리자_지정과_해제() {
        User admin = user("jeybell");
        User target = user("bob");
        admin.changeRole(UserRole.ADMIN);
        em.flush();
        em.clear();

        service.changeRole(target.getUserId(), UserRole.ADMIN, "jeybell");
        em.flush();
        em.clear();

        assertThat(userRepository.findById(target.getUserId()).orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 본인_권한은_변경할_수_없다() {
        User admin = user("jeybell");
        admin.changeRole(UserRole.ADMIN);
        em.flush();

        assertThatThrownBy(() -> service.changeRole(admin.getUserId(), UserRole.USER, "jeybell"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 본인_계정은_삭제할_수_없다() {
        User admin = user("jeybell");
        admin.changeRole(UserRole.ADMIN);
        em.flush();

        assertThatThrownBy(() -> service.deleteUser(admin.getUserId(), "jeybell"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 다른_사용자_삭제() {
        User admin = user("jeybell");
        User target = user("bob");
        em.flush();
        em.clear();

        service.deleteUser(target.getUserId(), "jeybell");
        em.flush();

        assertThat(userRepository.findById(target.getUserId())).isEmpty();
    }
}
