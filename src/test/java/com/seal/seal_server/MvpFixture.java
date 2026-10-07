package com.seal.seal_server;
import org.springframework.boot.SpringApplication;

/** Ephemeral local server for the desktop integration probe; never shipped in the app jar. */
public class MvpFixture {
    public static void main(String[] args) {
        SpringApplication.run(SealServerApplication.class,
                "--server.port=18081", "--spring.config.import=",
                "--spring.datasource.url=jdbc:h2:mem:desktop;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa",
                "--spring.datasource.password=", "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--seal.admin.username=mvp-test-admin", "--seal.admin.password=mvp-test-password");
    }
}
