package com.duolinfo.ia.proj;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjApplication {

    public static void main(String[] args) {
	    // Carrega o .env se ele existir na raiz
	    Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
	    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

	    SpringApplication.run(ProjApplication.class, args);
    }
}