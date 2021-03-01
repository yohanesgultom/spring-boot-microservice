package id.gultom.controller;

import id.gultom.config.KafkaProperties;
import id.gultom.dto.ProductDto;
import id.gultom.model.Product;
import id.gultom.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private KafkaTemplate<String, Product> kafkaProducer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @PostMapping(value = {"", "/"})
    public ResponseEntity<Product> create(@Valid @RequestBody ProductDto productDto) {
        Product savedProduct = productRepo.save(new Product(productDto.getProductName(), productDto.getSupplierId()));
        log.info("Product created: " + productDto.toString());
        kafkaProducer.send(kafkaProperties.getTopics().getProductCreated(), savedProduct);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping(value = {"", "/"})
    public ResponseEntity<List<Product>> all() {
        List<Product> productList = productRepo.findAll();
        return ResponseEntity.ok(productList);
    }

}