package id.gultom.repository;


import id.gultom.model.Supplier;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

public interface SupplierRepository extends CouchbaseRepository<Supplier, String> {

}
