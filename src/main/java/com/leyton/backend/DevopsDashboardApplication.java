package com.leyton.backend;

import com.leyton.backend.services.CronService;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.ParseException;

@SpringBootApplication
@EnableScheduling
public class DevopsDashboardApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsDashboardApplication.class);

    @Autowired
    private CronService cronService;

    public static void main(String[] args) {
        SpringApplication.run(DevopsDashboardApplication.class, args);
    }


    //@Scheduled(cron = "*/60 7,8,9,10,11,12,13,14,15,16,17,18,19,20 * * * ?")
    public void cron() throws GitLabApiException, ParseException {
        this.cronService.startCron();
    }


    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
                this.cronService.startCron();
        };
    }
}


