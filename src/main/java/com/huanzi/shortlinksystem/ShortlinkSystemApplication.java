package com.huanzi.shortlinksystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.huanzi.shortlinksystem.mapper")
public class ShortlinkSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortlinkSystemApplication.class, args);
    }

}
