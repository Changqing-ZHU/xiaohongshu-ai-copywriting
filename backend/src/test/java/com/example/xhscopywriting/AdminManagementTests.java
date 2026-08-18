package com.example.xhscopywriting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.xhscopywriting.model.Generation;
import com.example.xhscopywriting.repository.AdminRepository;
import com.example.xhscopywriting.repository.GenerationRepository;
import com.example.xhscopywriting.repository.UserRepository;
import com.example.xhscopywriting.security.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties =
        "security.jwt.secret=test-jwt-secret-with-at-least-32-characters")
class AdminManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void dashboardStatisticsAreCorrect() throws Exception {
        TestAuthentication.Identity admin = admin();
        long usersBefore = adminRepository.countUsers();
        long generationsBefore = adminRepository.countGenerations();
        long todayBefore = adminRepository.countTodayGenerations();
        long activeBefore = adminRepository.countTodayActiveUsers();
        TestAuthentication.Identity userA = user();
        TestAuthentication.Identity userB = user();

        insertGeneration(userA.user().getId(), LocalDateTime.now(), "A today");
        insertGeneration(userB.user().getId(), LocalDateTime.now(), "B today");
        insertGeneration(userA.user().getId(), LocalDateTime.now().minusDays(1), "A yesterday");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, admin.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(usersBefore + 2))
                .andExpect(jsonPath("$.totalGenerations").value(generationsBefore + 3))
                .andExpect(jsonPath("$.todayGenerations").value(todayBefore + 2))
                .andExpect(jsonPath("$.todayActiveUsers").value(activeBefore + 2));
    }

    @Test
    void userListReturnsUsernameRoleAndCreatedAt() throws Exception {
        TestAuthentication.Identity admin = admin();
        TestAuthentication.Identity user = user();

        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, admin.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == '" + user.user().getUsername()
                        + "')].role").value("USER"))
                .andExpect(jsonPath("$[?(@.username == '" + user.user().getUsername()
                        + "')].createdAt").isNotEmpty());
    }

    @Test
    void generationListReturnsOwnerImageTitleTimeAndStatus() throws Exception {
        TestAuthentication.Identity admin = admin();
        TestAuthentication.Identity user = user();
        Long generationId = insertGeneration(
                user.user().getId(),
                LocalDateTime.now(),
                "Admin visible title");

        mockMvc.perform(get("/api/admin/generations")
                        .header(HttpHeaders.AUTHORIZATION, admin.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + generationId
                        + ")].username").value(user.user().getUsername()))
                .andExpect(jsonPath("$[?(@.id == " + generationId
                        + ")].title").value("Admin visible title"))
                .andExpect(jsonPath("$[?(@.id == " + generationId
                        + ")].status").value("COMPLETED"))
                .andExpect(jsonPath("$[?(@.id == " + generationId
                        + ")].imageUrl").value("/api/generations/" + generationId + "/image"))
                .andExpect(jsonPath("$[?(@.id == " + generationId
                        + ")].createdAt").isNotEmpty());
    }

    private TestAuthentication.Identity admin() {
        return TestAuthentication.createUser(userRepository, jwtTokenProvider, "ADMIN");
    }

    private TestAuthentication.Identity user() {
        return TestAuthentication.createUser(userRepository, jwtTokenProvider);
    }

    private Long insertGeneration(Long userId, LocalDateTime createdAt, String title) {
        LocalDateTime timestamp = createdAt.withNano(0);
        Generation generation = new Generation();
        generation.setUserId(userId);
        generation.setStatus("COMPLETED");
        generation.setOriginalFileName("admin-test.jpg");
        generation.setStoredFileName("admin-test-stored.jpg");
        generation.setImagePath("uploads/admin-test-stored.jpg");
        generation.setImageContentType("image/jpeg");
        generation.setImageSize(2048L);
        generation.setTitle(title);
        generation.setCreatedAt(timestamp);
        generation.setUpdatedAt(timestamp);
        return generationRepository.insert(generation);
    }
}
