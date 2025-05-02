package com.example.oracle;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class OracleApplication {

    public static void main(String[] args) {
        SpringApplication.run(OracleApplication.class, args);
    }

    @Bean
    JdbcChatMemoryRepository jdbcChatMemoryRepository(DataSource dataSource) {
        return JdbcChatMemoryRepository
                .builder()
                .jdbcTemplate(new JdbcTemplate(dataSource))
                .build();
    }

    @Bean
    ApplicationRunner dogumentInitializer(JdbcClient db, VectorStore vectorStore, DogRepository repository) {
        return args -> {

            if (db.sql("select count(*) from SPRING_AI_VECTORS").query(Integer.class).single() != 0)
                return;

            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s,  description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });
        };
    }

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

//    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    DogAssistantController(ChatClient.Builder ai, VectorStore vectorStore/*, JdbcChatMemoryRepository jdbcChatMemoryRepository*/) {
//        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.ai = ai
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();

    }


    @PostMapping("/{user}/inquire")
    String inquire(@PathVariable String user, @RequestParam String question) {
        var memoryAdvisor = this.memory
                .computeIfAbsent(user, x -> PromptChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                        .build())
                        .build());
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
