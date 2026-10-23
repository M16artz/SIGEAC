package ec.edu.unl.sec;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelProperties;
import ec.edu.unl.sec.evaluationmodel.application.ValidatedStrategyPort;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(EvaluationModelProperties.class)
@EnableScheduling
public class SecApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    @ConditionalOnMissingBean
    ValidatedStrategyPort validatedStrategies() {
        return java.util.Set::of;
    }
}
