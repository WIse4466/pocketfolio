package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.CategoryRequest;
import com.pocketfolio.backend.dto.CategoryResponse;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.exception.ResourceNotFoundException;
import com.pocketfolio.backend.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 單元測試")
class CategoryServiceTest {

    @Mock private CategoryRepository repository;

    @InjectMocks private CategoryService service;

    @Captor private ArgumentCaptor<Category> categoryCaptor;

    static final UUID CURRENT_USER_ID = UUID.randomUUID();
    static final UUID OTHER_USER_ID   = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        User user = new User();
        user.setId(CURRENT_USER_ID);
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User userWith(UUID id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Category categoryWith(UUID id, UUID ownerId) {
        Category c = new Category();
        c.setId(id);
        c.setName("類別-" + id.toString().substring(0, 4));
        c.setType(CategoryType.EXPENSE);
        c.setUser(userWith(ownerId));
        return c;
    }

    private CategoryRequest categoryRequest() {
        CategoryRequest req = new CategoryRequest();
        req.setName("餐飲");
        req.setType(CategoryType.EXPENSE);
        req.setDescription("每日飲食");
        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createCategory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createCategory（建立類別）")
    class CreateCategory {

        @Test
        @DisplayName("正確將 Request 欄位映射到 Entity 並儲存")
        void create_happyPath_savesWithCorrectFields() {
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "餐飲")).willReturn(false);
            Category saved = categoryWith(UUID.randomUUID(), CURRENT_USER_ID);
            saved.setName("餐飲");
            given(repository.save(any())).willReturn(saved);

            service.createCategory(categoryRequest());

            then(repository).should().save(categoryCaptor.capture());
            Category captured = categoryCaptor.getValue();
            assertThat(captured.getName()).isEqualTo("餐飲");
            assertThat(captured.getType()).isEqualTo(CategoryType.EXPENSE);
            assertThat(captured.getUser().getId()).isEqualTo(CURRENT_USER_ID);
        }

        @Test
        @DisplayName("類別名稱重複：拋出 IllegalArgumentException 且不儲存")
        void create_duplicateName_throws() {
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "餐飲")).willReturn(true);

            assertThatThrownBy(() -> service.createCategory(categoryRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("餐飲");

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getCategory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCategory（查詢單筆）")
    class GetCategory {

        @Test
        @DisplayName("查詢自己的類別，回傳正確 Response")
        void getOne_ownedByCurrentUser_returnsResponse() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(categoryWith(id, CURRENT_USER_ID)));

            CategoryResponse response = service.getCategory(id);

            assertThat(response.getId()).isEqualTo(id);
        }

        @Test
        @DisplayName("查詢他人的類別：拋出 ResourceNotFoundException")
        void getOne_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(categoryWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.getCategory(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("查詢不存在的類別：拋出 ResourceNotFoundException")
        void getOne_notFound_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCategory(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllCategories / getCategoriesByType
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllCategories / getCategoriesByType（查詢列表）")
    class GetCategories {

        @Test
        @DisplayName("查詢所有類別：只帶入當前用戶 ID")
        void getAll_queriesCurrentUserOnly() {
            given(repository.findByUserId(CURRENT_USER_ID)).willReturn(List.of());

            service.getAllCategories();

            then(repository).should().findByUserId(CURRENT_USER_ID);
            then(repository).should(never()).findAll();
        }

        @Test
        @DisplayName("依類型查詢：傳入 userId 與 type")
        void getByType_passesUserIdAndType() {
            given(repository.findByUserIdAndType(CURRENT_USER_ID, CategoryType.INCOME))
                    .willReturn(List.of());

            service.getCategoriesByType(CategoryType.INCOME);

            then(repository).should().findByUserIdAndType(CURRENT_USER_ID, CategoryType.INCOME);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateCategory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateCategory（更新）")
    class UpdateCategory {

        @Test
        @DisplayName("正常更新欄位並儲存")
        void update_happyPath_updatesFields() {
            UUID id = UUID.randomUUID();
            Category existing = categoryWith(id, CURRENT_USER_ID);
            existing.setName("舊名稱");
            given(repository.findById(id)).willReturn(Optional.of(existing));
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "餐飲")).willReturn(false);
            Category saved = categoryWith(id, CURRENT_USER_ID);
            saved.setName("餐飲");
            given(repository.save(any())).willReturn(saved);

            service.updateCategory(id, categoryRequest());

            then(repository).should().save(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().getName()).isEqualTo("餐飲");
        }

        @Test
        @DisplayName("改名為已存在名稱：拋出 IllegalArgumentException 且不儲存")
        void update_duplicateName_throws() {
            UUID id = UUID.randomUUID();
            Category existing = categoryWith(id, CURRENT_USER_ID);
            existing.setName("舊名稱");
            given(repository.findById(id)).willReturn(Optional.of(existing));
            given(repository.existsByUserIdAndName(CURRENT_USER_ID, "餐飲")).willReturn(true);

            assertThatThrownBy(() -> service.updateCategory(id, categoryRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("餐飲");

            then(repository).should(never()).save(any());
        }

        @Test
        @DisplayName("更新他人的類別：拋出 ResourceNotFoundException 且不儲存")
        void update_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(categoryWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.updateCategory(id, categoryRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteCategory
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCategory（刪除）")
    class DeleteCategory {

        @Test
        @DisplayName("成功刪除自己的類別")
        void delete_happyPath_deletesById() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(categoryWith(id, CURRENT_USER_ID)));

            service.deleteCategory(id);

            then(repository).should().deleteById(id);
        }

        @Test
        @DisplayName("刪除他人類別：拋出 ResourceNotFoundException 且不執行刪除")
        void delete_ownedByOtherUser_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.of(categoryWith(id, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.deleteCategory(id))
                    .isInstanceOf(ResourceNotFoundException.class);

            then(repository).should(never()).deleteById(any());
        }

        @Test
        @DisplayName("刪除不存在類別：拋出 ResourceNotFoundException")
        void delete_notFound_throws() {
            UUID id = UUID.randomUUID();
            given(repository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCategory(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
