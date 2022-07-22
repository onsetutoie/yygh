package com.atguigu.yygh.cmn.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@MapperScan("com.atguigu.yygh.cmn.mapper")
public class CmnConfig {
}
