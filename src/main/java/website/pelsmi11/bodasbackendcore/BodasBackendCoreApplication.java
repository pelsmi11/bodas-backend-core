package website.pelsmi11.bodasbackendcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BodasBackendCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BodasBackendCoreApplication.class, args);
    }

}
