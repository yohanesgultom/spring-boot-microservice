package id.gultom.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.gultom.dto.ProductDto;
import id.gultom.dto.SupplierDto;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.ProductRepository;
import id.gultom.repository.SupplierRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
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

    @Test
    public void indexShouldBeAvailable() throws Exception {
        String resBody = this.restTemplate.getForObject("http://localhost:" + port + "/", String.class);
//        log.info("resBody: " + resBody);
        assertThat(resBody).isEqualTo("{\"message\":\"Hello, World!\"}");
    }

    @Test
    public void shouldBeAbleToCreateProduct() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setName("My Supplier");
        supplier.setBranches(Arrays.asList("Bangkok", "Hanoi", "Jakarta"));
        Supplier savedSupplier = supplierRepo.save(supplier);

        ProductDto productDto = new ProductDto("My Product", savedSupplier.getId());
        HttpEntity<ProductDto> req = new HttpEntity<>(productDto);

        // validate response body
        String resBody = this.restTemplate.postForObject("http://localhost:" + port + "/products", req, String.class);
//        log.info("resBody: " + resBody);
        Product actual = this.objectMapper.readValue(resBody, Product.class);
        assertThat(actual.getProductName()).isEqualTo(productDto.getProductName());

        // validate product exists in mssql
        Optional<Product> result = productRepo.findById(actual.getId());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(actual);

        // validate product added to supplier
        int timeout = 30;
        int i = 0;
        Supplier updatedSupplier = null;
        do {
            Thread.sleep(1000);
            updatedSupplier = supplierRepo.findById(savedSupplier.getId()).get();
            i++;
        } while(updatedSupplier.getProducts().isEmpty() && i < timeout);
        assertThat(updatedSupplier.getProducts().size()).isEqualTo(1);
        assertThat(updatedSupplier.getProducts().get(0)).isEqualTo(actual.getProductName());
    }

    @Test
    public void shouldBeAbleToCreateSupplier() throws Exception {
        SupplierDto supplierDto = new SupplierDto("My Supplier", Arrays.asList("London", "New York"));
        HttpEntity<SupplierDto> req = new HttpEntity<>(supplierDto);
        String resBody = this.restTemplate.postForObject("http://localhost:" + port + "/suppliers", req, String.class);

        // validate response body
        Supplier actual = this.objectMapper.readValue(resBody, Supplier.class);
        assertThat(actual.getName()).isEqualTo(supplierDto.getName());
        assertThat(actual.getBranches().toString()).isEqualTo(supplierDto.getBranches().toString());

        // validate supplier exists in couchbase
        Optional<Supplier> result = supplierRepo.findById(actual.getId());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo(actual);
    }
}
