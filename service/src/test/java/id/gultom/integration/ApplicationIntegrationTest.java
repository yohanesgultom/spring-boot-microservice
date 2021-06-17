package id.gultom.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.gultom.dto.CustomerDto;
import id.gultom.dto.ProductDto;
import id.gultom.dto.SupplierDto;
import id.gultom.model.Customer;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.couchbase.CustomerRepository;
import id.gultom.repository.mssql.ProductRepository;
import id.gultom.repository.couchbase.SupplierRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

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

    @Autowired
    private CustomerRepository customerRepo;

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
        customerRepo.deleteAll();
    }

    private List<Supplier> createSuppliers(int num) {
        Iterable<Supplier> supplierIterable = null;
        List<Supplier> suppliers = new ArrayList<>();
        try {
            for (int i = 1; i <= num; i++) {
                Supplier supplier = new Supplier();
                supplier.setName("Supplier " + i);
                supplier.setBranches(Arrays.asList("Bangkok", "Hanoi", "Jakarta"));
                suppliers.add(supplier);
            }
            supplierIterable = this.supplierRepo.saveAll(suppliers);
            Thread.sleep(1000); // Wait until data properly saved
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        List<Supplier> supplierList = new ArrayList<>();
        supplierIterable.forEach(supplierList::add);
        return supplierList;
    }

    private Supplier createSupplier() {
        return this.createSuppliers(1).iterator().next();
    }

    private List<Product> createProducts(String supplierId, int num) {
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= num; i++) {
            products.add(new Product("Product " + i, supplierId));
        }
        Iterable<Product> productIterable = this.productRepo.saveAll(products);
        List<Product> productList = new ArrayList<>();
        productIterable.forEach(productList::add);
        return productList;
    }

    @Test
    public void indexShouldBeAvailable() {
        String resBody = this.restTemplate.getForObject("http://localhost:" + port + "/", String.class);
        assertThat(resBody).isEqualTo("{\"message\":\"Hello, World!\"}");
    }

    // products

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

    // Suppliers

    @Test
    public void shouldListSupplierByPage() throws Exception {
        // init data
        List<Supplier> suppliers = this.createSuppliers(11);

        // call
        String resBody = this.restTemplate.getForObject("http://localhost:" + port + "/suppliers", String.class);
        log.info(resBody);
        JSONObject page = new JSONObject(resBody);
        JSONArray content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(10);
        assertThat((Integer) page.get("totalPages")).isEqualTo(2); // default page size = 10
        assertThat((Integer) page.get("totalElements")).isEqualTo(suppliers.size());

        // page 2
        resBody = this.restTemplate.getForObject("http://localhost:" + port + "/suppliers?page=2", String.class);
        page = new JSONObject(resBody);
        content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(1);
        assertThat((Integer) page.get("totalPages")).isEqualTo(2); // default page size = 10
        assertThat((Integer) page.get("totalElements")).isEqualTo(suppliers.size());

        // size 5
        resBody = this.restTemplate.getForObject("http://localhost:" + port + "/suppliers?size=5", String.class);
        page = new JSONObject(resBody);
        content = page.getJSONArray("content");
        assertThat(content.length()).isEqualTo(5);
        assertThat((Integer) page.get("totalPages")).isEqualTo(3);
        assertThat((Integer) page.get("totalElements")).isEqualTo(suppliers.size());
    }

    @Test
    public void shouldGetSupplier() {
        Supplier supplier = this.createSupplier();
        Supplier resSupplier = this.restTemplate.getForObject("http://localhost:" + port + "/suppliers/" + supplier.getId(), Supplier.class);
        assertThat(supplier).isEqualTo(resSupplier);
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

    @Test
    public void shouldUpdateSupplier() {
        Supplier supplier = this.createSupplier();
        supplier.setName("Supplier X");
        HttpEntity<Supplier> req = new HttpEntity<>(supplier);
//        this.restTemplate.put("http://localhost:" + port + "/suppliers/" + supplier.getId(), req);
        ResponseEntity<String> response = this.restTemplate.exchange("http://localhost:" + port + "/suppliers/{id}", HttpMethod.PUT, req, String.class, supplier.getId());
        log.info("shouldUpdateSupplier response.status: " + response.getStatusCode().toString());
        log.info("shouldUpdateSupplier response.body: " + response.getBody());
        Supplier updatedSupplier = this.supplierRepo.findById(supplier.getId()).get();
        assertThat(updatedSupplier).isEqualTo(supplier);
    }

    @Test
    public void shouldDeleteSupplier() {
        Supplier supplier = this.createSupplier();
        this.restTemplate.delete("http://localhost:" + port + "/suppliers/" + supplier.getId());
        Optional<Supplier> result = this.supplierRepo.findById(supplier.getId());
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void shouldCreateCustomer() throws Exception {
        CustomerDto dto = new CustomerDto("My Customer");
        HttpEntity<CustomerDto> req = new HttpEntity<>(dto);
        String resBody = this.restTemplate.postForObject("http://localhost:" + port + "/customers", req, String.class);

        // validate response body
        Customer actual = this.objectMapper.readValue(resBody, Customer.class);
        assertThat(actual.getName()).isEqualTo(dto.getName());

        // validate customer exists in couchbase
        Date now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        Optional<Customer> optional = this.customerRepo.findById(actual.getId());
        assertThat(optional.isPresent()).isTrue();
        Customer result = optional.get();
        assertThat(result.getName()).isEqualTo(dto.getName());
        assertThat(result.getCreatedAt()).isBeforeOrEqualTo(now);
        assertThat(result.getUpdatedAt()).isBeforeOrEqualTo(now);
    }
}
