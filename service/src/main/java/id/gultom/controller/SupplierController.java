package id.gultom.controller;

import id.gultom.config.KafkaProperties;
import id.gultom.dto.SupplierDto;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

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

    @GetMapping
    public ResponseEntity<Page<Supplier>> index(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Supplier> supplierPage = this.supplierRepo.findAll(PageRequest.of(page-1, size));
        return ResponseEntity.ok(supplierPage);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> show(@PathVariable String id) {
        Optional<Supplier> result = this.supplierRepo.findById(id);
        if (!result.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "supplier id not found " + id);
        }
        return ResponseEntity.ok(result.get());
    }

    @PostMapping
    public ResponseEntity<Supplier> create(@Valid @RequestBody SupplierDto supplierDto) {
        Supplier supplier = new Supplier();
        supplier.setName(supplierDto.getName());
        supplier.setBranches(supplierDto.getBranches());
        Supplier storedSupplier = this.supplierRepo.save(supplier);
        this.kafkaProducer.send(this.kafkaProperties.getTopics().getSupplierCreated(), storedSupplier);
        return ResponseEntity.ok(storedSupplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(@Valid @RequestBody Supplier supplier, @PathVariable String id) {
        if (!supplier.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url and body have different id");
        }
        if (!this.supplierRepo.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "supplier id not found " + id);
        }
        Supplier updatedSupplier = this.supplierRepo.save(supplier);
        return ResponseEntity.ok(updatedSupplier);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) throws URISyntaxException {
        Optional<Supplier> result = this.supplierRepo.findById(id);
        if (!result.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "supplier id not found " + id);
        }
        this.supplierRepo.delete(result.get());
        return ResponseEntity.noContent().build();
    }
}
