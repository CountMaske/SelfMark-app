package com.nortidart.selfmark.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortidart.selfmark.plugin.entity.UserPlugin;
import com.nortidart.selfmark.plugin.mapper.PluginMapper;
import com.nortidart.selfmark.plugin.mapper.UserPluginMapper;
import com.nortidart.selfmark.plugin.service.DefaultPluginConfigFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "management.health.defaults.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PluginCatalogIntegrationTests {
    @Container static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("selfmark").withUsername("selfmark").withPassword("selfmark");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PluginMapper pluginMapper;
    @MockitoSpyBean UserPluginMapper userPluginMapper;
    @Autowired DefaultPluginConfigFactory defaultConfigFactory;
    private final List<Long> users = new ArrayList<>();

    @AfterEach
    void cleanOwnUsers() {
        for (Long userId : users) {
            jdbc.update("DELETE FROM user_plugin WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM user WHERE id = ?", userId);
        }
    }

    @Test
    void productionSeedAndUninstalledMarketAreComplete() throws Exception {
        assertThat(jdbc.queryForList("SELECT code, name, sort FROM plugin ORDER BY sort, id"))
                .containsExactly(Map.of("code", "quadrant-board", "name", "画板", "sort", 1),
                        Map.of("code", "pomodoro", "name", "番茄钟", "sort", 2));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE mobile = '13800138001'", Integer.class))
                .isZero();
        var user = register("13800404001");
        var market = market(user.token());
        assertThat(market.size()).isEqualTo(2);
        for (JsonNode entry : market) {
            assertThat(entry.get("installed").booleanValue()).isFalse();
            for (String field : List.of("userPluginId", "enabled", "config", "description")) {
                assertThat(entry.has(field)).isTrue();
                assertThat(entry.get(field).isNull()).isTrue();
            }
        }
        mockMvc.perform(get("/api/plugins")).andExpect(status().isUnauthorized());
    }

    @Test
    void joinedStateAndEntityJsonRoundTripAreIsolated() throws Exception {
        var a = register("13800404002");
        var b = register("13800404003");
        var config = objectMapper.createObjectNode();
        config.put("label", "中文");
        config.put("integer", 9007199254740993L);
        config.put("decimal", new BigDecimal("0.125"));
        config.put("preciseDecimal", "0.123456789012345678");
        config.putArray("nested").add(true).addNull().addObject().put("foo", 1);
        var instance = new UserPlugin();
        instance.setUserId(a.id());
        instance.setPluginId(pluginId("quadrant-board"));
        instance.setConfig(config);
        instance.setEnabled(false);
        assertThat(userPluginMapper.insert(instance)).isEqualTo(1);
        var stored = userPluginMapper.selectById(instance.getId());
        assertThat(stored.getConfig()).isEqualTo(config);
        assertThat(stored.getCreatedAt()).isNotNull();
        var rows = pluginMapper.selectMarket(a.id());
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().isInstalled()).isTrue();
        assertThat(rows.getFirst().getEnabled()).isFalse();
        assertThat(rows.getFirst().getConfig()).isEqualTo(config);
        assertThat(rows.get(1).isInstalled()).isFalse();
        var first = market(a.token()).get(0);
        assertThat(first.get("userPluginId").longValue()).isEqualTo(instance.getId());
        assertThat(first.get("config").isObject()).isTrue();
        assertThat(first.at("/config/label").asText()).isEqualTo("中文");
        assertThat(first.at("/config/integer").longValue()).isEqualTo(9007199254740993L);
        assertThat(first.at("/config/preciseDecimal").asText()).isEqualTo("0.123456789012345678");
        for (JsonNode entry : market(b.token())) {
            assertThat(entry.get("installed").booleanValue()).isFalse();
            assertThat(entry.get("config").isNull()).isTrue();
        }
    }

    @Test
    void equalSortUsesIdOrderAndEmptyCatalogIsAnEmptyArray() throws Exception {
        var user = register("13800404004");
        Long boardId = pluginId("quadrant-board");
        try {
            jdbc.update("UPDATE plugin SET sort = 2 WHERE id = ?", boardId);
            var expectedIds = jdbc.queryForList("SELECT id FROM plugin ORDER BY id", Long.class);
            var market = market(user.token());
            assertThat(market.get(0).get("id").longValue()).isEqualTo(expectedIds.get(0));
            assertThat(market.get(1).get("id").longValue()).isEqualTo(expectedIds.get(1));
        } finally {
            jdbc.update("UPDATE plugin SET sort = 1 WHERE id = ?", boardId);
        }
        // Roll back catalog deletion so other tests keep the production seed.
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
        new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM plugin");
            assertThat(pluginMapper.selectMarket(user.id())).isEmpty();
            try {
                assertThat(market(user.token()).isEmpty()).isTrue();
            } catch (Exception exception) {
                throw new AssertionError("Empty catalog HTTP query failed", exception);
            }
            status.setRollbackOnly();
        });
    }

    @Test
    void actualDatabaseEnforcesIndexesNullabilityAndChecks() throws Exception {
        var a = register("13800404005");
        var b = register("13800404006");
        Long board = pluginId("quadrant-board");
        Long pomodoro = pluginId("pomodoro");
        assertThat(jdbc.queryForList("""
                SELECT INDEX_NAME FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'user_plugin'
                """, String.class)).contains("PRIMARY", "uk_user_plugin", "idx_user");
        assertThat(jdbc.queryForList("""
                SELECT COLUMN_NAME FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'user_plugin'
                  AND index_name = 'uk_user_plugin' ORDER BY SEQ_IN_INDEX
                """, String.class)).containsExactly("user_id", "plugin_id");
        assertThat(jdbc.queryForObject("""
                SELECT MIN(NON_UNIQUE) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'user_plugin'
                  AND index_name = 'uk_user_plugin'
                """, Integer.class)).isZero();
        var ddl = jdbc.queryForMap("SHOW CREATE TABLE user_plugin").get("Create Table").toString();
        assertThat(ddl).contains("chk_user_plugin_config_object", "chk_user_plugin_enabled");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO plugin(code,name) VALUES ('quadrant-board','duplicate')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM plugin WHERE code = 'Quadrant-Board'", Integer.class))
                .isZero();
        insert(a.id(), board, "{}", 0);
        assertThatThrownBy(() -> insert(a.id(), board, "{}", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
        insert(b.id(), board, "{}", 1);
        for (String invalid : List.of("null", "[]", "1", "true", "\"string\"")) {
            assertSqlRejected(() -> insert(a.id(), pomodoro, invalid, 1), 3819);
        }
        assertSqlRejected(() -> insert(a.id(), pomodoro, "{invalid", 1), 3140);
        assertSqlRejected(() -> insert(a.id(), pomodoro, null, 1), 1048);
        assertSqlRejected(() -> insert(a.id(), pomodoro, "{}", 2), 3819);
        assertSqlRejected(() -> insert(a.id(), pomodoro, "{}", null), 1048);
        assertSqlRejected(() -> insert(null, pomodoro, "{}", 1), 1048);
        assertSqlRejected(() -> insert(a.id(), null, "{}", 1), 1048);
        jdbc.update("INSERT INTO user_plugin(user_id,plugin_id,config) VALUES (?,?,?)",
                a.id(), pomodoro, "{\"nested\":[null,true]}");
        assertThat(jdbc.queryForObject("SELECT enabled FROM user_plugin WHERE user_id=? AND plugin_id=?",
                Integer.class, a.id(), pomodoro)).isEqualTo(1);
    }

    private void insert(Long user, Long plugin, String config, Integer enabled) {
        jdbc.update("INSERT INTO user_plugin(user_id,plugin_id,config,enabled) VALUES (?,?,?,?)",
                user, plugin, config, enabled);
    }

    @Test
    void installsBothPluginsWithServerDefaultsAndMarketDatabaseConsistency() throws Exception {
        var user = register("13800505001");
        var other = register("13800505002");
        for (String code : List.of("quadrant-board", "pomodoro")) {
            var result = mockMvc.perform(post("/api/plugins/{code}/install", code)
                            .param("userId", Long.toString(other.id()))
                            .header("Authorization", "Bearer " + user.token())
                            .contentType("application/json")
                            .content("{\"enabled\":false,\"config\":{},\"userId\":" + other.id() + "}"))
                    .andExpect(status().isOk()).andReturn();
            var response = response(result);
            assertThat(response.get("code").asInt()).isEqualTo(200);
            assertThat(response.get("msg").asText()).isEqualTo("success");
            var data = response.get("data");
            assertThat(data.size()).isEqualTo(6);
            assertThat(data.get("pluginId").asLong()).isEqualTo(pluginId(code));
            assertThat(data.get("code").asText()).isEqualTo(code);
            assertThat(data.get("name").asText()).isEqualTo(code.equals("pomodoro") ? "番茄钟" : "画板");
            assertThat(data.get("enabled").asBoolean()).isTrue();
            assertThat(data.get("config")).isEqualTo(defaultConfigFactory.create(code));
            var stored = userPluginMapper.selectById(data.get("id").asLong());
            assertThat(stored.getUserId()).isEqualTo(user.id());
            assertThat(stored.getPluginId()).isEqualTo(pluginId(code));
            assertThat(stored.getConfig()).isEqualTo(data.get("config"));
            assertThat(stored.getEnabled()).isTrue();
            var entry = market(user.token()).get(code.equals("pomodoro") ? 1 : 0);
            assertThat(entry.get("installed").asBoolean()).isTrue();
            assertThat(entry.get("userPluginId")).isEqualTo(data.get("id"));
            assertThat(entry.get("config")).isEqualTo(data.get("config"));
            assertThat(entry.get("enabled")).isEqualTo(data.get("enabled"));
        }
        for (JsonNode entry : market(other.token())) {
            assertThat(entry.get("installed").asBoolean()).isFalse();
        }
    }

    @Test
    void unknownAndDifferentCaseCodesAre404WithoutInstalling() throws Exception {
        var user = register("13800505003");
        for (String code : List.of("unknown", "Quadrant-Board", "POMODORO", "pomodoro ")) {
            mockMvc.perform(post("/api/plugins/{code}/install", code)
                            .header("Authorization", "Bearer " + user.token()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(404))
                    .andExpect(jsonPath("$.msg").value("插件不存在"))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
        assertThat(instanceCount(user.id())).isZero();
    }

    @Test
    void enabledAndDisabledDuplicatesPreserveOriginalInstance() throws Exception {
        var user = register("13800505004");
        long id = response(install(user.token(), "pomodoro")).at("/data/id").asLong();
        for (boolean enabled : List.of(true, false)) {
            jdbc.update("UPDATE user_plugin SET config=?, enabled=? WHERE id=?",
                    "{\"label\":\"自定义\",\"nested\":[null,true]}", enabled, id);
            var before = jdbc.queryForMap("SELECT * FROM user_plugin WHERE id=?", id);
            mockMvc.perform(post("/api/plugins/pomodoro/install")
                            .header("Authorization", "Bearer " + user.token()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(409))
                    .andExpect(jsonPath("$.msg").value("插件已安装"))
                    .andExpect(jsonPath("$.data").isEmpty());
            assertThat(jdbc.queryForMap("SELECT * FROM user_plugin WHERE id=?", id)).isEqualTo(before);
            assertThat(instanceCount(user.id())).isEqualTo(1);
        }
    }

    @Test
    void registeredCodeWithoutDefaultsFailsWithNoResidue() throws Exception {
        var user = register("13800505005");
        jdbc.update("INSERT INTO plugin(code,name,sort) VALUES ('unconfigured-test','未配置',99)");
        try {
            mockMvc.perform(post("/api/plugins/unconfigured-test/install")
                            .header("Authorization", "Bearer " + user.token()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.msg").value("服务器内部错误"));
            assertThat(instanceCount(user.id())).isZero();
        } finally {
            jdbc.update("DELETE FROM plugin WHERE code='unconfigured-test'");
        }
    }

    @Test
    void responseReadbackFailureRollsBackRealInsert() throws Exception {
        var user = register("13800505006");
        doThrow(new IllegalStateException("Simulated readback failure"))
                .when(userPluginMapper).selectById(anyLong());
        mockMvc.perform(post("/api/plugins/pomodoro/install")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.msg").value("服务器内部错误"));
        assertThat(instanceCount(user.id())).isZero();
    }

    @Test
    void concurrentInstallationHasOneSuccessOneConflictAndOneRow() throws Exception {
        var user = register("13800505007");
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<JsonNode> request = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                var result = mockMvc.perform(post("/api/plugins/quadrant-board/install")
                                .header("Authorization", "Bearer " + user.token())).andReturn();
                var body = response(result);
                assertThat(body.get("code").asInt()).isEqualTo(result.getResponse().getStatus());
                return body;
            };
            var a = executor.submit(request);
            var b = executor.submit(request);
            var responses = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
            assertThat(responses.stream().map(body -> body.get("code").asInt()).toList())
                    .containsExactlyInAnyOrder(200, 409);
            var success = responses.stream().filter(body -> body.get("code").asInt() == 200).findFirst().orElseThrow();
            var conflict = responses.stream().filter(body -> body.get("code").asInt() == 409).findFirst().orElseThrow();
            assertThat(conflict.get("msg").asText()).isEqualTo("插件已安装");
            assertThat(conflict.get("data").isNull()).isTrue();
            assertThat(instanceCount(user.id())).isEqualTo(1);
            var stored = userPluginMapper.selectById(success.at("/data/id").asLong());
            assertThat(stored.getConfig()).isEqualTo(defaultConfigFactory.create("quadrant-board"));
            assertThat(stored.getEnabled()).isTrue();
        }
    }

    private int instanceCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_plugin WHERE user_id=?", Integer.class, userId);
    }

    private MvcResult install(String token, String code) throws Exception {
        return mockMvc.perform(post("/api/plugins/{code}/install", code)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
    }

    private JsonNode response(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertSqlRejected(Runnable insert, int mysqlError) {
        assertThatThrownBy(insert::run).isInstanceOfSatisfying(DataAccessException.class, exception -> {
            assertThat(exception.getMostSpecificCause()).isInstanceOfSatisfying(java.sql.SQLException.class,
                    sql -> assertThat(sql.getErrorCode()).isEqualTo(mysqlError));
        });
    }

    private Long pluginId(String code) {
        return jdbc.queryForObject("SELECT id FROM plugin WHERE code = ?", Long.class, code);
    }

    private TestUser register(String mobile) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("mobile", mobile,
                                "password", "Password123", "username", "Catalog Test"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        var data = objectMapper.readTree(body).get("data");
        long id = data.get("id").longValue();
        users.add(id);
        return new TestUser(id, data.get("token").asText());
    }

    private JsonNode market(String token) throws Exception {
        String body = mockMvc.perform(get("/api/plugins").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("data");
    }

    private record TestUser(long id, String token) { }
}
