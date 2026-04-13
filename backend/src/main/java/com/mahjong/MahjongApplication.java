package com.mahjong;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.mahjong.mapper")
@EnableScheduling
public class MahjongApplication {

  public static void main(String[] args) {
    SpringApplication.run(MahjongApplication.class, args);
  }
}
