package id.gultom.repository;


import id.gultom.model.Supplier;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SupplierRepository extends CrudRepository<Supplier, String> {
    List<Supplier> findByName(String name);
}
