package cn.edu.techgroup.outsourcing;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@MapperScan(
        basePackages = "cn.edu.techgroup.outsourcing.modules",
        annotationClass = Mapper.class)
@ConfigurationPropertiesScan
@SpringBootApplication
public class TechRequestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechRequestApplication.class, args);
    }
}
