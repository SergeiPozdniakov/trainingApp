
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;

@Configuration
@Profile("test")
public class TestConfig {

    @Bean
    public Clock clock() {
        // Возвращаем системные часы, но в тестах можно переопределить
        return Clock.systemDefaultZone();
    }
}