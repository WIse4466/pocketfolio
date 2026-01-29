# Day 1
## 在 start.spring.io 使用 Spring Initializr 產生框架
- 設定：
  - Project: Maven

  - Language: Java

  - Spring Boot: 選 3.5.10

  - Group: com.pocketfolio

  - Artifact: backend

  - Packaging: Jar

  - Java: 17

  - Configuration: Yaml

- Dependencies：

  - Spring Web (為了寫 Controller)

  - Spring Data JPA (為了寫 Repository/資料庫)

  - PostgreSQL Driver (連資料庫用)

  - Lombok (產生 getter/setter)

  - Validation (驗證輸入資料用)

- 解壓縮完後可以看一下 pom.xml (Maven專案的核心配置檔案，Project Object Model)
  1. 定義專案基本資訊: 專案的組織名稱、專案名稱、版本等
  2. 管理依賴套件: 聲明專案需要哪些外部函式庫、Maven會自動下載這些套件和其他相依的套件
  3. 設定建置（Build）流程: 如何編譯、測試、打包專案，使用哪些Maven插件
  4. 設定Java版本

## 建立 backend 資料夾和 Transaction.java

- @Entity: 告訴 Spring 這是一個要存入資料庫的物件
- @Table(name = "transactions"): 指定資料庫裡的表名叫做"transactions"，如果不寫會用class的名稱作為資料表名稱
- @Getter @Setter: Lombok 自動產生 get() set()
- 欄位: id, amount(金額), note(備註), date
- @Id: 標示主鍵
- @Column(nullable = false): 標示欄位不能為空
- @GeneratedValue(strategy = GenerationType.UUID): id 自動產生 UUID 格式

## 寫 docker-compose.yaml

- 目前先設定 services db 的部分，及 volumes
- 把名為 postgres_data 的 volume 掛載到容器內的 /var/lib/postgresql/data 路徑
- 在最外層宣告 volume
  - 告訴 Docker Compose 要使用一個名為 postgres_data 的 volume
  - 如果此 volume 不存在，Docker 會自動建立，如果已經存在就自動建立
  
## 設定 Spring Boot 連線 (application.yaml)
- datasource: 資料庫連接設定
  - jdbc:postgresql:// - 使用 JDBC 協議連接 PostgreSQL
  - localhost - 資料庫主機位址（本機）
  - 5432 - PostgresSQL 的 port (對應docker-compose.yml中的ports)
  - pocketfolio - 資料庫名稱 (對應docker-compose.yml中的POSTGRES_DB)
- driver-class-name: org.postgresql.Driver
  - JDBC 驅動程式的類別名稱
  - 告訴 Spring Boot 使用 PostgreSQL 的驅動程式
  - 通常 Spring Boot 會自動偵測，但明確寫出更清楚
- JPA/Hibernate 設定
  
| 選項 | 說明 | 適用場景 |
|------|------|----------|
| update | 啟動時檢查 Entity，若資料表不存在則建立；若表結構改變則更新（**不會刪除既有資料**） | 開發階段 |
| create | 每次啟動都會刪除舊表並重新建立（**資料會遺失**） | 測試環境 |
| create-drop | 啟動時建立資料表，關閉應用程式時刪除資料表 | 單元測試 |
| validate | 僅檢查 Entity 與資料表結構是否一致，不會修改資料庫 | 生產環境 |
| none | 不進行任何資料庫結構操作 | 生產環境 |
    
- show-sql: true,properties.hibernate.format_sql: true
  - 在 console 顯示 Hibernate 執行的 SQL 語句
  - SQL 語句格式化和縮排

- JPA 與 Hibernate 的關係
  - JPA (Java Persistence API) 是介面
  - Hibernate 是實作
  - JPA 定義了如何將 Java 物件對應到資料庫表 (ORM-Object-Relational Mapping)
    - 定義註解：@Entity, @Table, @Id, @Column 等
    - 定義介面：EntityManager, EntityManagerFactory 等
    - 定義查詢語言：JPQL (Java Persistence Query Language)
  - Hibernate 最流行的 JPA 實作
    - 自動生成 SQL
    - 管理資料庫連接
    - 處理快取 (Cache)
