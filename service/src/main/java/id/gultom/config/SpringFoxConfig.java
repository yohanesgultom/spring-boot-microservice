package id.gultom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SpringFoxConfig {
    @Bean
    public Docket api() {
        final ApiInfo apiInfo = new ApiInfoBuilder()
                .title("Spring Boot REST API")
                .description("Spring Boot REST API with Kafka, MS-SQL and Couchbase")
                .version("1.0.0")
                .contact(new Contact("Yohanes Gultom", "https://yohanes.gultom.id", "yohanes.gultom@gmail.com"))
                .build();

        return new Docket(DocumentationType.SWAGGER_2)
                .tags(
                        new Tag("Health", "Health"),
                        new Tag("Product", "Product"),
                        new Tag("Supplier", "Supplier"),
                        new Tag("Customer", "Customer")
                )
                .select()
                .apis(RequestHandlerSelectors.basePackage("id.gultom"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo);
    }
}
