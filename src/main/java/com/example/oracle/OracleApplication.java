package com.example.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.OracleType;
import oracle.jdbc.driver.OracleDriver;
import oracle.sql.VECTOR;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class OracleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleApplication.class, args);
    }

    private VECTOR toVECTOR(float[] floatList) throws SQLException {
        final double[] doubles = new double[floatList.length];
        int i = 0;
        for (double d : floatList) {
            doubles[i++] = d;
        }
        return VECTOR.ofFloat64Values(doubles);
    }


    @Bean
    DataSource hikari(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String pw

    ) {
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(pw);
        config.setDriverClassName(OracleDriver.class.getName());
//        config.addDataSourceProperty("oracle.jdbc.vectorDefaultGetObjectType", "Array");
        return new HikariDataSource(config);
    }

    @Bean
    ApplicationRunner vectorFun(JdbcClient jdbcClient,
                                @Value("${spring.datasource.url}") String url,
                                @Value("${spring.datasource.username}") String username,
                                @Value("${spring.datasource.password}") String pw
            /*      JdbcClient db, EmbeddingModel embeddingModel*/) {
        return args -> {
            var ds = hikari(url, username, pw);

            jdbcClient.sql("""
                                create table if not exists m_test (
                                    embedding vector(3,FLOAT64)
                                ) 
                            """)
                    .update();

//            var embedding = embeddingModel.embed("Pooch Palace");
             insertVectorWithBatchAPI(ds.getConnection());
/*

            var vector = toVECTOR(f);
            try (var conn = ds.getConnection()) {
                try (var ps = conn.prepareStatement("insert into m_test( embedding ) values (?)")) {
                    ps.setObject(1, vector, OracleType.VECTOR_FLOAT64);
                    ps.executeUpdate();
                }
            }*/

            System.out.println("written!");

        };
    }

    private void insertVectorWithBatchAPI(Connection connection) throws SQLException {
        String insertSql = "INSERT INTO M_TEST (EMBEDDING) VALUES (  ?)";

        float[][] vectors = {{1.1f, 2.2f, 3.3f}, {1.3f, 7.2f, 4.3f}, {5.9f, 5.2f, 7.3f}};
        System.out.println("SQL DML: " + insertSql);
        System.out.println("VECTORs to be inserted as a batch: " + Arrays.toString(vectors[0]) + ", "
                + Arrays.toString(vectors[1]) + ", " + Arrays.toString(vectors[2]));
        try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            for (float[] vector : vectors) {
                insertStatement.setObject(1, vector, OracleType.VECTOR_FLOAT64);
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

/*
    @Bean
    ApplicationRunner runner(EmbeddingModel[] embeddingModel, DogRepository repository, VectorStore vector) {
        return args -> repository.findAll().forEach(dog -> {
            System.out.println("there are " + embeddingModel.length + " embedding models");

            var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                    dog.id(), dog.name(), dog.description()
            ));
            try {
                vector.add(List.of(dogument));
            } catch (Throwable throwable) {
                System.out.println("errr: " + throwable);
            }
        });
    }*/

/*
    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel model) {
        return SimpleVectorStore.builder(model).build();
    }
*/

}
/*
 @Controller
 @REsponseBody
class DogAssistantController {

    private final String systemPrompt = """
            You are an AI powered assistant to help people adopt a dog from the adoption\s
            agency named Pooch Palace with locations in Antwerp, Seoul, Tokyo, Singapore, Paris,\s
            Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
            will be presented below. If there is no information, then return a polite response suggesting we\s
            don't have any dogs available.
            """;

    private final Map<String, PromptChatMemoryAdvisor> memory = new ConcurrentHashMap<>();


    private final ChatClient ai;

    DogAssistantController(ChatClient.Builder ai, VectorStore vectorStore) {
        this.ai = ai
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    @PostMapping("/{user}/inquire")
    String inquire(@PathVariable String user, @RequestParam String question) {
        var memoryAdvisor = this.memory
                .computeIfAbsent(user, x -> PromptChatMemoryAdvisor.builder(
                        MessageWindowChatMemory.builder().build()).build());
        return this.ai
                .prompt()
                .system(systemPrompt)
                .advisors(memoryAdvisor)
                .user(question)
                .call()
                .content();
    }
}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String owner, String description) {
}

 */