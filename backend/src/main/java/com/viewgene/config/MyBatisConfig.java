package com.viewgene.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.persistence.enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.viewgene.infrastructure.persistence.mapper")
public class MyBatisConfig {
}
