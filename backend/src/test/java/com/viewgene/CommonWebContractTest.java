package com.viewgene;

import com.viewgene.common.api.ApiResponse;
import com.viewgene.common.exception.BusinessException;
import com.viewgene.common.web.GlobalExceptionHandler;
import com.viewgene.common.web.RequestIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CommonWebContractTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ContractController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void successResponseAndHeaderShareTheRequestId() throws Exception {
        mockMvc.perform(get("/test/success").header(RequestIdFilter.HEADER_NAME, "request_12345678"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "request_12345678"))
                .andExpect(jsonPath("$.requestId").value("request_12345678"))
                .andExpect(jsonPath("$.data.value").value("ok"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void businessErrorUsesStableSafeShape() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TEST_CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("测试冲突"))
                .andExpect(jsonPath("$.error.fieldErrors").isArray())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("C:\\\\private\\\\secret.sql"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("SELECT *"))));
    }

    @RestController
    static class ContractController {
        @GetMapping("/test/success")
        ApiResponse<Map<String, String>> success() {
            return ApiResponse.success(Map.of("value", "ok"));
        }

        @GetMapping("/test/business-error")
        void businessError() {
            throw new BusinessException("TEST_CONFLICT", "测试冲突", HttpStatus.CONFLICT);
        }

        @GetMapping("/test/unexpected-error")
        void unexpectedError() {
            throw new IllegalStateException("SELECT * failed at C:\\private\\secret.sql");
        }
    }
}
