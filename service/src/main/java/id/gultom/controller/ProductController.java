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
    private KafkaTemplate<String, ProductDto> kafkaProducer;

    @Autowired
    private KafkaProperties kafkaProperties;

    @PostMapping(value = {"", "/"})
    public ResponseEntity<Product> create(@Valid @RequestBody ProductDto productDto) {
        log.info("Product created: " + productDto.toString());
        Product product = new Product(productDto.getProductName());
        Product savedProduct = productRepo.save(product);
        kafkaProducer.send(kafkaProperties.getTopics().getProductCreated(), productDto);
        return ResponseEntity.ok(savedProduct);
    }

    @GetMapping(value = {"", "/"})
    public ResponseEntity<List<Product>> all() {
        List<Product> productList = productRepo.findAll();
        return ResponseEntity.ok(productList);
    }

}