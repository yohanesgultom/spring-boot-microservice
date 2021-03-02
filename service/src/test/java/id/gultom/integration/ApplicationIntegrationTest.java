package id.gultom.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.gultom.dto.ProductDto;
import id.gultom.dto.SupplierDto;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.ProductRepository;
import id.gultom.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@ActiveProfiles("integration-test")
@Slf4j
public class ApplicationIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    private static List<Supplier> newSuppliers = new ArrayList<>();

    @KafkaListener(topics = "${kafka.topics.supplier-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenSupplierCreated(ConsumerRecord<String, Supplier> record) {
        log.info("Test listener got message: " + record.toString());
        newSuppliers.add(record.value());
    }

    @BeforeEach
    public void resetDatabase() {
        productRepo.deleteAll();
        supplierRepo.deleteAll();
    }

    private Supplier createSupplier() {
        Supplier supplier = new Supplier();
        supplier.setName("My Supplier");
        supplier.setBranches(Arrays.asList("Bangkok", "Hanoi", "Jakarta"));
        return supplierRepo.save(supplier);
    }

    private List<Product> createProducts(String supplierId, int num) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            products.add(new Product("Product" + i, supplierId));
        }
        this.productRepo.saveAll(products);
        return products;
    }

    @Test
    public void indexShouldBeAvailable() {
        String resBody = this.restTemplate.getForObject("http://localhost:" + port + "/", String.class);
        assertThat(resBody).isEqualTo("{\"message\":\"Hello, World!\"}");
    }

    @Test
    public void shouldListProductByPage() throws Exception {
        // init data
        Supplier supplier = this.createSupplier();
        List<Product> products = this.createProducts(supplier.getId(), 11);

        // call
        String resBody = this.restTemplate.getForObject("http://localhost:" + port + "/products", String.class);
        JSONObject page = new JSONObject(resBody);
        JSONArray content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(10);
        assertThat((Integer) page.get("totalPages")).isEqualTo(2); // default page size = 10
        assertThat((Integer) page.get("totalElements")).isEqualTo(products.size());

        // page 2
        resBody = this.restTemplate.getForObject("http://localhost:" + port + "/products?page=2", String.class);
        page = new JSONObject(resBody);
        content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(1);
        assertThat((Integer) page.get("totalPages")).isEqualTo(2); // default page size = 10
        assertThat((Integer) page.get("totalElements")).isEqualTo(products.size());

        // size 5
        resBody = this.restTemplate.getForObject("http://localhost:" + port + "/products?size=5", String.class);
        page = new JSONObject(resBody);
        content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(5);
        assertThat((Integer) page.get("totalPages")).isEqualTo(3);
        assertThat((Integer) page.get("totalElements")).isEqualTo(products.size());
    }

    @Test
    public void shouldGetProduct() {
        Supplier supplier = this.createSupplier();
        List<Product> products = this.createProducts(supplier.getId(), 1);
        Long productId = products.get(0).getId();

        Product resProduct = this.restTemplate.getForObject("http://localhost:" + port + "/products/" + productId, Product.class);
        assertThat(products.get(0)).isEqualTo(resProduct);
    }

    @Test
    public void shouldCreateProduct() throws Exception {
        Supplier supplier = this.createSupplier();
        ProductDto productDto = new ProductDto("My Product", supplier.getId());
        HttpEntity<ProductDto> req = new HttpEntity<>(productDto);

        // validate response body
        String resBody = this.restTemplate.postForObject("http://localhost:" + port + "/products", req, String.class);
        Product actual = this.objectMapper.readValue(resBody, Product.class);
        assertThat(actual.getProductName()).isEqualTo(productDto.getProductName());

        // validate product exists in mssql
        Optional<Product> result = this.productRepo.findById(actual.getId());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(actual);

        // validate product added to supplier
        int timeout = 30;
        int i = 0;
        Supplier updatedSupplier = null;
        do {
            Thread.sleep(1000);
            updatedSupplier = this.supplierRepo.findById(supplier.getId()).get();
            i++;
        } while(updatedSupplier.getProducts().isEmpty() && i < timeout);
        assertThat(updatedSupplier.getProducts().isEmpty()).isFalse();
        assertThat(updatedSupplier.getProducts().get(0)).isEqualTo(actual.getProductName());
    }

    @Test
    public void shouldUpdateProduct() {
        Supplier supplier = this.createSupplier();
        List<Product> products = this.createProducts(supplier.getId(), 1);

        Product product = products.get(0);
        product.setProductName("Product X");
        HttpEntity<Product> req = new HttpEntity<>(product);
        this.restTemplate.put("http://localhost:" + port + "/products/" + product.getId(), req);
        Product updatedProduct = this.productRepo.findById(product.getId()).get();
        assertThat(updatedProduct).isEqualTo(product);
    }

    @Test
    public void shouldDeleteProduct() {
        Supplier supplier = this.createSupplier();
        List<Product> products = this.createProducts(supplier.getId(), 1);
        Long productId = products.get(0).getId();

        this.restTemplate.delete("http://localhost:" + port + "/products/" + productId);
        Optional<Product> result = this.productRepo.findById(productId);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void shouldCreateSupplier() throws Exception {
        SupplierDto supplierDto = new SupplierDto("My Supplier", Arrays.asList("London", "New York"));
        HttpEntity<SupplierDto> req = new HttpEntity<>(supplierDto);
        String resBody = this.restTemplate.postForObject("http://localhost:" + port + "/suppliers", req, String.class);

        // validate response body
        Supplier actual = this.objectMapper.readValue(resBody, Supplier.class);
        assertThat(actual.getName()).isEqualTo(supplierDto.getName());
        assertThat(actual.getBranches().toString()).isEqualTo(supplierDto.getBranches().toString());

        // validate supplier exists in couchbase
        Optional<Supplier> result = this.supplierRepo.findById(actual.getId());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(actual);

        // validate kafka message
        int timeout = 30;
        int i = 0;
        while (newSuppliers.isEmpty() && i < timeout) {
            Thread.sleep(1000);
            i++;
        }
        assertThat(newSuppliers.isEmpty()).isFalse();
        assertThat(newSuppliers.get(0)).isEqualTo(actual);
    }
}
