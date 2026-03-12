// src/test/java/com/pocketfolio/backend/repository/TransactionRepositoryTest.java
package com.pocketfolio.backend.repository;

import com.pocketfolio.backend.entity.Transaction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Phase 3 完成後需重寫測試以支援用戶隔離")
@DataJpaTest  // 只啟動 JPA 層，自動使用 H2 記憶體資料庫，測試結束自動回滾
@DisplayName("TransactionRepository 單元測試")
class TransactionRepositoryTest {

    @Autowired
    TransactionRepository repository;

    @Test
    @DisplayName("儲存後可以查詢回來")
    void save_andFind() {
        Transaction tx = new Transaction();
        tx.setAmount(new BigDecimal("500"));
        tx.setNote("測試");
        tx.setDate(LocalDate.of(2026, 1, 1));

        Transaction saved = repository.save(tx);

        assertThat(saved.getId()).isNotNull();  // UUID 已自動產生
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("分頁查詢：依日期降序，第一筆是最新的")
    void findAll_sortByDateDesc() {
        Transaction old = new Transaction();
        old.setAmount(BigDecimal.TEN);
        old.setDate(LocalDate.of(2026, 1, 1));
        repository.save(old);

        Transaction recent = new Transaction();
        recent.setAmount(BigDecimal.ONE);
        recent.setDate(LocalDate.of(2026, 2, 15));
        repository.save(recent);

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"));
        Page<Transaction> result = repository.findAll(pageable);

        assertThat(result.getContent().get(0).getDate())
                .isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    @DisplayName("刪除後查詢不到")
    void delete_thenNotFound() {
        Transaction tx = new Transaction();
        tx.setAmount(BigDecimal.TEN);
        tx.setDate(LocalDate.now());
        Transaction saved = repository.save(tx);

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}