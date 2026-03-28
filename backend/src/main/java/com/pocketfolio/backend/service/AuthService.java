package com.pocketfolio.backend.service;

import com.pocketfolio.backend.dto.AuthResponse;
import com.pocketfolio.backend.dto.LoginRequest;
import com.pocketfolio.backend.dto.RegisterRequest;
import com.pocketfolio.backend.entity.Account;
import com.pocketfolio.backend.entity.AccountType;
import com.pocketfolio.backend.entity.Category;
import com.pocketfolio.backend.entity.CategoryType;
import com.pocketfolio.backend.entity.User;
import com.pocketfolio.backend.repository.AccountRepository;
import com.pocketfolio.backend.repository.CategoryRepository;
import com.pocketfolio.backend.repository.UserRepository;
import com.pocketfolio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // 註冊
    public AuthResponse register(RegisterRequest request) {

        // 檢查 email 是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email 已被註冊");
        }

        // 檢查 displayname 是否已存在
        if (userRepository.existsByDisplayName(request.getDisplayName())) {
            throw new IllegalArgumentException("顯示名稱已被使用");
        }

        // 建立新用戶
        User user = new User();
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // 建立預設類別
        List<String> expenseNames = List.of("餐飲", "交通", "娛樂", "購物", "住房", "醫療", "教育");
        List<String> incomeNames = List.of("薪資", "獎金", "投資收益", "其他收入");
        List<Category> defaultCategories = new java.util.ArrayList<>();
        for (String name : expenseNames) {
            Category c = new Category();
            c.setName(name);
            c.setType(CategoryType.EXPENSE);
            c.setUser(savedUser);
            defaultCategories.add(c);
        }
        for (String name : incomeNames) {
            Category c = new Category();
            c.setName(name);
            c.setType(CategoryType.INCOME);
            c.setUser(savedUser);
            defaultCategories.add(c);
        }
        categoryRepository.saveAll(defaultCategories);

        // 建立預設帳戶
        record DefaultAccount(String name, AccountType type) {}
        List<DefaultAccount> defaultAccountDefs = List.of(
                new DefaultAccount("現金", AccountType.CASH),
                new DefaultAccount("銀行帳戶", AccountType.BANK),
                new DefaultAccount("信用卡", AccountType.CREDIT_CARD),
                new DefaultAccount("投資帳戶", AccountType.INVESTMENT)
        );
        List<Account> defaultAccounts = new java.util.ArrayList<>();
        for (DefaultAccount def : defaultAccountDefs) {
            Account a = new Account();
            a.setName(def.name());
            a.setType(def.type());
            a.setInitialBalance(BigDecimal.ZERO);
            a.setUser(savedUser);
            defaultAccounts.add(a);
        }
        accountRepository.saveAll(defaultAccounts);

        // 產生 JWT Token
        String token = jwtUtil.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .displayName(savedUser.getDisplayName())
                .build();
    }

    // 登入
    public AuthResponse login(LoginRequest request) {
        // Spring Security 驗證
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();

        // 更新最後登入時間
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 產生 JWT Token
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }
}
