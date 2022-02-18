package com.qh.crm.generator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author chengwei
 */
@SpringBootApplication
@MapperScan("com.qh.crm.generator.dao")
public class CommonGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonGeneratorApplication.class, args);
    }
}
