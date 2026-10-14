package com.nortidart.selfmark.plugin.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nortidart.selfmark.auth.mapper.UserMapper;
import com.nortidart.selfmark.auth.security.JwtUtil;
import com.nortidart.selfmark.auth.service.TokenBlacklistService;
import com.nortidart.selfmark.plugin.mapper.PluginMapper;
import com.nortidart.selfmark.plugin.mapper.UserPluginMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class UserPluginControllerTests {
    @MockitoBean UserMapper userMapper;
    @MockitoBean PluginMapper pluginMapper;
    @MockitoBean UserPluginMapper userPluginMapper;
    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[{}]", "\"string\"", "1", "true", "false"})
    void nonObjectRootsAre400(String body) throws Exception {
        mockMvc.perform(put("/api/user-plugins/11/config").contentType("application/json")
                        .header("Authorization", token()).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("config 必须是 JSON 对象"))
                .andExpect(jsonPath("$.data").isEmpty());
        verifyNoInteractions(pluginMapper, userPluginMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{invalid"})
    void missingAndMalformedBodyAre400(String body) throws Exception {
        mockMvc.perform(put("/api/user-plugins/11/config").contentType("application/json")
                        .header("Authorization", token()).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("请求体格式错误"));
        verifyNoInteractions(pluginMapper, userPluginMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "0", "-1", "-9223372036854775809"})
    void invalidInstanceIdsAre400(String id) throws Exception {
        mockMvc.perform(put("/api/user-plugins/{id}/config", id).contentType("application/json")
                        .header("Authorization", token()).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("id 必须是正整数"));
        verifyNoInteractions(pluginMapper, userPluginMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/x-www-form-urlencoded", "application/xml"})
    void unsupportedMediaTypesHaveUnified415(String type) throws Exception {
        mockMvc.perform(put("/api/user-plugins/11/config").contentType(type)
                        .header("Authorization", token()).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.msg").value("仅支持 application/json"))
                .andExpect(jsonPath("$.data").isEmpty());
        verifyNoInteractions(pluginMapper, userPluginMapper);
    }

    @Test
    void missingTokenIs401BeforeParsingJson() throws Exception {
        mockMvc.perform(put("/api/user-plugins/11/config").contentType("application/json").content("{invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        verifyNoInteractions(pluginMapper, userPluginMapper);
    }

    private String token() {
        return "Bearer " + jwtUtil.issue(42L, "USER");
    }
}
