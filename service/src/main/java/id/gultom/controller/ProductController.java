package id.gultom.controller;

import id.gultom.config.KafkaProperties;
import id.gultom.dto.ProductDto;
import id.gultom.model.Product;
import id.gultom.repository.ProductRepository;
import id.gultom.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private KafkaTemplate<String, Product> kafkaProducer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @GetMapping
    public ResponseEntity<Page<Product>> index(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        Page<Product> productPage = productRepo.findAll(PageRequest.of(page-1, size));
        return ResponseEntity.ok(productPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> show(@PathVariable Long id) {
        Optional<Product> result = productRepo.findById(id);
        if (!result.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "product id not found " + id);
        }
        return ResponseEntity.ok(result.get());
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductDto productDto) {
        if (!supplierRepo.findById(productDto.getSupplierId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "supplier id not found " + productDto.getSupplierId());
        }
        Product product = new Product(productDto.getProductName(), productDto.getSupplierId());
        Product savedProduct = productRepo.save(product);
        log.info("Product created: " + productDto.toString());
        kafkaProducer.send(kafkaProperties.getTopics().getProductCreated(), savedProduct);
        return ResponseEntity.ok(savedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@Valid @RequestBody Product product, @PathVariable Long id) {
        if (product.getId() != id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url and body have different id");
        }
        if (!productRepo.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "product id not found " + id);
        }
        productRepo.save(product);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) throws URISyntaxException {
        Optional<Product> result = productRepo.findById(id);
        if (!result.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "product id not found " + id);
        }
        productRepo.delete(result.get());
        return ResponseEntity.noContent().build();
    }
}