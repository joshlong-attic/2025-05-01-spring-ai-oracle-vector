package com.example.oracle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.driver.OracleDriver;
import oracle.sql.VECTOR;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.sql.SQLException;

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


    DataSource better(@Value("${spring.datasource.url}") String url,
                      @Value("${spring.datasource.username}") String username,
                      @Value("${spring.datasource.password}") String pw) throws Exception {


        String DB_URL = url; // "jdbc:oracle:thin:@myhost:1521/orclservicename";
        String DB_USER = username;
        String DB_PASSWORD = pw;
        String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.pool.OracleDataSource";

        // Get the PoolDataSource for UCP
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();

        // Set the connection factory first before all other properties
        pds.setConnectionFactoryClassName(CONN_FACTORY_CLASS_NAME);
        pds.setURL(DB_URL);
        pds.setUser(DB_USER);
        pds.setPassword(DB_PASSWORD);
        pds.setConnectionPoolName("JDBC_UCP_POOL");
        pds.setConnectionProperty("oracle.jdbc.vectorDefaultGetObjectType", "int[]");
        // Default is 0. Set the initial number of connections to be created
        // when UCP is started.
        pds.setInitialPoolSize(5);

        // Default is 0. Set the minimum number of connections
        // that is maintained by UCP at runtime.
        pds.setMinPoolSize(5);

        // Default is Integer.MAX_VALUE (2147483647). Set the maximum number of
        // connections allowed on the connection pool.
        pds.setMaxPoolSize(20);

        // Default is 30secs. Set the frequency in seconds to enforce the timeout
        // properties. Applies to inactiveConnectionTimeout(int secs),
        // AbandonedConnectionTimeout(secs)& TimeToLiveConnectionTimeout(int secs).
        // Range of valid values is 0 to Integer.MAX_VALUE. .
        pds.setTimeoutCheckInterval(5);

        // Default is 0. Set the maximum time, in seconds, that a
        // connection remains available in the connection pool.
        pds.setInactiveConnectionTimeout(10);

        return pds;
    }

    //@Bean
    DataSource pool(
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
    ApplicationRunner vectorFun(
            JdbcClient jdbcClient,
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String pw
    ) {
        return args -> {
            var ds = better(url, username, pw);
            jdbcClient
                    .sql("""
                                create table if not exists m_test (
                                    id number(10) primary key,
                                    embedding vector(3, INT8 )
                                )   
                            
                            """)
                    .update();
            jdbcClient.sql(" delete from m_test ").update();
            System.out.println("there are " + jdbcClient.sql("select count(*) from m_test ").query(Integer.class).single() + " rows.");
            this.doInsert(ds.getConnection());
            System.out.println("written!");
        };
    }


    private String vectorString(int[] vector) {
        var sb = new StringBuilder();
        sb.append("[");
        for (var i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private void doInsert(java.sql.Connection connection) throws SQLException {
        var ctr = 0;
        var insertSql = "INSERT INTO M_TEST(ID,EMBEDDING) VALUES( ?, ?)";
        var vectors = new int[][]{{1, 2, 3}, {1, 7, 4}, {5, 5, 7}};
        try (var statement = connection.createStatement()) {
            for (var vector : vectors) {
                var sql = "   insert into m_test (id, embedding) values ( " + (ctr++) + ", " + vectorString(vector) + ")";
                System.out.println("sql: " + sql);
                statement.executeUpdate(sql);
            }
//            for (var vector : vectors) {
//                statement.setInt(1, ctr++);
//                statement.setObject(2, VECTOR.ofInt8Values(vector), OracleType.VECTOR);
//                statement.executeUpdate();
//            }
//            insertStatement.executeBatch();
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