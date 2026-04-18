package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AuthResponse;
import com.pocketfolio.backend.dto.LoginRequest;
import com.pocketfolio.backend.dto.RegisterRequest;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.repository.UserRepository;
import com.pocketfolio.backend.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 單元測試")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService service;

    @Captor private ArgumentCaptor<List<Category>> categoriesCaptor;

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setDisplayName("TestUser");
        req.setPassword("password123");
        return req;
    }

    private User savedUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("TestUser");
        return user;
    }

    @Nested
    @DisplayName("register（註冊）")
    class Register {

        @Test
        @DisplayName("成功註冊：儲存用戶、11 個預設類別、4 個預設帳戶，並回傳 JWT")
        void register_happyPath_savesUserCategoriesAccountsAndReturnsToken() {
            User user = savedUser();
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.existsByDisplayName(any())).willReturn(false);
            given(userRepository.save(any())).willReturn(user);
            given(passwordEncoder.encode(any())).willReturn("encodedPassword");
            given(jwtUtil.generateToken(any())).willReturn("jwt-token");

            AuthResponse response = service.register(registerRequest());

            then(userRepository).should().save(any());
            then(categoryRepository).should().saveAll(categoriesCaptor.capture());
            then(accountRepository).should().saveAll(any());

            List<Category> savedCategories = categoriesCaptor.getValue();
            assertThat(savedCategories).hasSize(11);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getUserId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Email 已存在：拋出 IllegalArgumentException，不儲存任何資料")
        void register_duplicateEmail_throws() {
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            assertThatThrownBy(() -> service.register(registerRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email");

            then(userRepository).should(never()).save(any());
            then(categoryRepository).should(never()).saveAll(any());
        }

        @Test
        @DisplayName("displayName 已存在：拋出 IllegalArgumentException，不儲存任何資料")
        void register_duplicateDisplayName_throws() {
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.existsByDisplayName("TestUser")).willReturn(true);

            assertThatThrownBy(() -> service.register(registerRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("名稱");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("密碼被加密後儲存，原始密碼不存入 DB")
        void register_passwordIsEncoded() {
            User user = savedUser();
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.existsByDisplayName(any())).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("hashed");
            given(userRepository.save(any())).willReturn(user);
            given(jwtUtil.generateToken(any())).willReturn("token");

            service.register(registerRequest());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashed");
        }
    }

    @Nested
    @DisplayName("login（登入）")
    class Login {

        @Test
        @DisplayName("成功登入：呼叫 AuthenticationManager 並回傳 JWT")
        void login_happyPath_returnsToken() {
            User user = savedUser();
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            given(authenticationManager.authenticate(any())).willReturn(authToken);
            given(userRepository.save(any())).willReturn(user);
            given(jwtUtil.generateToken(user)).willReturn("jwt-token");

            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("password123");

            AuthResponse response = service.login(req);

            then(authenticationManager).should().authenticate(any());
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("登入後更新 lastLoginAt")
        void login_updatesLastLoginAt() {
            User user = savedUser();
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            given(authenticationManager.authenticate(any())).willReturn(authToken);
            given(userRepository.save(any())).willReturn(user);
            given(jwtUtil.generateToken(any())).willReturn("token");

            LoginRequest req = new LoginRequest();
            req.setEmail("test@example.com");
            req.setPassword("password123");

            service.login(req);

            then(userRepository).should().save(user);
            assertThat(user.getLastLoginAt()).isNotNull();
        }
    }
}
