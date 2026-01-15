package com.elice.cinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MovieTicketReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieTicketReservationApplication.class, args);
    }

}
