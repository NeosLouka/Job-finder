package org.example;


import org.example.ATS_source.greenhouse.boardTokens.GreenhouseTools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.List;
import java.util.Map;


@EnableAsync
@SpringBootApplication
public class Main{

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

        GreenhouseTools greenhouseTools = context.getBean(GreenhouseTools.class);

        Map<String, List<String>> companies = greenhouseTools.getCompanies();

        for (String company : companies.keySet()) {

            List<String> tokens = companies.get(company);

            System.out.println(company + " -> " + tokens);
        }
    }
}
