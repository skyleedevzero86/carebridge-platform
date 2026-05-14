package com.sleekydz86.carebridge.backend;

import com.sleekydz86.carebridge.backend.testsupport.RedisStubConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(RedisStubConfiguration.class)
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }
}
