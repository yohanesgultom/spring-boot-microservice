package id.gultom.controller;

import id.gultom.config.KafkaProperties;
import id.gultom.dto.SupplierDto;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private KafkaTemplate<String, Supplier> kafkaProducer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @GetMapping("")
    public ResponseEntity<List<Supplier>> index() {
        List<Supplier> docs = new ArrayList<>();
        Iterator<Supplier> it = supplierRepo.findAll().iterator();
        while(it.hasNext()) {
            docs.add(it.next());
        }
        return ResponseEntity.ok(docs);
    }

    @PostMapping("")
    public ResponseEntity<Supplier> create(@Valid @RequestBody SupplierDto supplierDto) {
        Supplier supplier = new Supplier();
        supplier.setName(supplierDto.getName());
        supplier.setBranches(supplierDto.getBranches());
        Supplier storedSupplier = supplierRepo.save(supplier);
        kafkaProducer.send(kafkaProperties.getTopics().getSupplierCreated(), storedSupplier);
        return ResponseEntity.ok(storedSupplier);
    }
}
