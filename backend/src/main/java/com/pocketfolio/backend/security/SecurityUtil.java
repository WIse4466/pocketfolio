package com.pocketfolio.backend.security;

import com.pocketfolio.backend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtil {

    // 取得當前登入的用戶
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("用戶未登入");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        }

        throw new IllegalStateException("無法取得當前用戶");
    }

    // 取得當前用戶 ID
    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    // 取得當前用戶 Email
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}
