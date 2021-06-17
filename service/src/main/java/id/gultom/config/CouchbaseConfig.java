package id.gultom.config;

import id.gultom.model.Customer;
import id.gultom.model.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.SimpleCouchbaseClientFactory;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;
import org.springframework.data.couchbase.repository.config.RepositoryOperationsMapping;

@Slf4j
@Configuration
@EnableCouchbaseRepositories(basePackages = "id.gultom.repository.couchbase")
public class CouchbaseConfig extends AbstractCouchbaseConfiguration {
    @Value("${spring.couchbase.connection-string}")
    private String connectionString;

    @Value("${spring.couchbase.username}")
    private String username;

    @Value("${spring.couchbase.password}")
    private String password;

    // this bucket will be used by models/repositories
    // that is not mapped to custom couchbase templates
    @Value("${spring.data.couchbase.bucket-name:Default}")
    private String defaultBucketName;

    @Autowired
    private MappingCouchbaseConverter mappingCouchbaseConverter;

    @Override
    public String getConnectionString() {
        return this.connectionString;
    }

    @Override
    public String getUserName() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getBucketName() {
        return this.defaultBucketName;
    }

    @Override
    protected void configureRepositoryOperationsMapping(RepositoryOperationsMapping mapping) {
        // only model that is mapped to custom template will be mapped to non-default bucket
        mapping.mapEntity(Supplier.class, supplierTemplate());
        mapping.mapEntity(Customer.class, customerTemplate());
    }

    @Bean
    public CouchbaseTemplate supplierTemplate() {
        // define the custom template bucket name
        return customCouchbaseTemplate(customCouchbaseClientFactory("Supplier"));
    }

    @Bean
    public CouchbaseTemplate customerTemplate() {
        // define the custom template bucket name
        return customCouchbaseTemplate(customCouchbaseClientFactory("Customer"));
    }

    // do not use couchbaseTemplate for the name of this method, otherwise the value of that been
    // will be used instead of the result from this call (the client factory arg is different)
    public CouchbaseTemplate customCouchbaseTemplate(CouchbaseClientFactory couchbaseClientFactory) {
        return new CouchbaseTemplate(couchbaseClientFactory, mappingCouchbaseConverter);
    }

    // do not use couchbaseClientFactory for the name of this method, otherwise the value of that bean will
    // will be used instead of this call being made ( bucketname is an arg here, instead of using bucketName() )
    public CouchbaseClientFactory customCouchbaseClientFactory(String bucketName) {
        return new SimpleCouchbaseClientFactory(getConnectionString(), authenticator(), bucketName);
    }
}
