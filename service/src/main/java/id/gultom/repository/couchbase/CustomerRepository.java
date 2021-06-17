package id.gultom.repository.couchbase;

import id.gultom.model.Customer;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

public interface CustomerRepository extends CouchbaseRepository<Customer, String> {
}
