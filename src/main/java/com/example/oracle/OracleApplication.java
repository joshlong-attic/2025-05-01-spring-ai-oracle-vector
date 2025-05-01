package com.example.oracle;

import oracle.sql.VECTOR;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class OracleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleApplication.class, args);
    }

    private VECTOR toVECTOR(final float[] floatList) throws SQLException {
        final double[] doubles = new double[floatList.length];
        int i = 0;
        for (double d : floatList) {
            doubles[i++] = d;
        }

        return VECTOR.ofFloat64Values(doubles);
    }


    @Bean
    ApplicationRunner vectorFun
            (JdbcClient db, DataSource ds, EmbeddingModel embeddingModel) {
        return args -> {
            var embedding = embeddingModel.embed("Pooch Palace");
            var vector = toVECTOR(embedding);
//            db.sql("insert into m_test( embedding ) values (  ?)")
//                    .param(  toVECTOR( embedding))
//                    .update();

            try (var ps = ds.getConnection().prepareStatement("insert into m_test( embedding ) values (?)")) {
                ps.setObject(1, vector);
                ps.executeUpdate();
            }

            System.out.println("written!");

        };
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

@Controller
@ResponseBody
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
                .computeIfAbsent(user,  x  -> PromptChatMemoryAdvisor.builder(
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