package id.gultom.unit;

import com.couchbase.client.java.env.ClusterEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.gultom.config.CouchbaseConfig;
import id.gultom.config.KafkaProperties;
import id.gultom.dto.ProductDto;
import id.gultom.dto.SupplierDto;
import id.gultom.listener.ProductCreatedListener;
import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.couchbase.CustomerRepository;
import id.gultom.repository.couchbase.SupplierRepository;
import id.gultom.repository.mssql.ProductRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.ReactiveCouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Tag("unit")
@AutoConfigureMockMvc
public class ApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    MappingCouchbaseConverter mappingCouchbaseConverter;

    @MockBean
    CustomConversions customConversions;

    @MockBean
    @Qualifier("couchbaseTemplate")
    CouchbaseTemplate couchbaseTemplate;

    @MockBean
    ReactiveCouchbaseTemplate reactiveCouchbaseTemplate;

    @MockBean
    CouchbaseClientFactory couchbaseClientFactory;

    @MockBean
    ClusterEnvironment couchbaseCluster;

    @MockBean
    CouchbaseConfig couchbaseConfig;

    @MockBean
    ProductRepository mockProductRepo;

    @MockBean
    SupplierRepository mockSupplierRepo;

    @MockBean
    CustomerRepository mockCustomerRepo;

    @MockBean
    KafkaTemplate<String, Product> mockKafkaProducerProduct;

    @MockBean
    KafkaTemplate<String, Supplier> mockKafkaProducerSupplier;

    @MockBean
    ProductCreatedListener productCreatedListener;

    @MockBean(answer=Answers.RETURNS_DEEP_STUBS)
    KafkaProperties mockKafkaProperties;

    @Test
    public void shouldReturnDefaultMessage() throws Exception {
        String expectedContent = new JSONObject()
                .put("message", "Hello, World!")
                .toString();
        this.mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(expectedContent));
    }

    @Test
    public void shouldValidateProduct() throws Exception {
        String postBodyInvalid = new JSONObject()
                .put("id", 1)
                .toString();
        this.mockMvc
                .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(postBodyInvalid))
                .andDo(print())
                .andExpect(status().is(400))
                .andExpect(content().json("{\"productName\":\"must not be blank\"}"));
    }

    @Test
    public void shouldCreateProduct() throws Exception {
        Supplier savedSupplier = new Supplier();
        savedSupplier.setId("supplier-1");
        savedSupplier.setName("My supplier");
        savedSupplier.setBranches(Arrays.asList("Jakarta"));

        ProductDto productDto = new ProductDto("My Product", savedSupplier.getId());
        String productDtoJson = objectMapper.writeValueAsString(productDto);

        Product savedProduct = new Product(1l, productDto.getProductName(), productDto.getSupplierId());
        String savedProductJson = objectMapper.writeValueAsString(savedProduct);

        String topic = "product_created";
        Mockito.when(mockSupplierRepo.findById(productDto.getSupplierId())).thenReturn(Optional.of(savedSupplier));
        Mockito.when(mockProductRepo.save(any(Product.class))).thenReturn(savedProduct);
        Mockito.when(mockKafkaProperties.getTopics().getProductCreated()).thenReturn(topic);
        Mockito.when(mockKafkaProducerProduct.send(any(String.class), any(Product.class))).thenReturn(null);

        this.mockMvc
                .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(productDtoJson))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(savedProductJson));

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        Mockito.verify(mockProductRepo).save(argumentCaptor.capture());
        Product capturedArgument = argumentCaptor.getValue();
        assert capturedArgument.getProductName().equals(savedProduct.getProductName());

        Mockito.verify(mockKafkaProducerProduct).send(topic, savedProduct);
    }

    @Test
    public void supplierShouldBeCreated() throws Exception {
        SupplierDto supplierDto = new SupplierDto("My supplier", Arrays.asList("Bangkok", "Hanoi"));
        String supplierDtoJson = objectMapper.writeValueAsString(supplierDto);

        Supplier savedSupplier = new Supplier();
        savedSupplier.setName(supplierDto.getName());
        savedSupplier.setBranches(supplierDto.getBranches());
        String savedSupplierJson = objectMapper.writeValueAsString(savedSupplier);

        String topic = "supplier_created";
        Mockito.when(mockSupplierRepo.save(any(Supplier.class))).thenReturn(savedSupplier);
        Mockito.when(mockKafkaProperties.getTopics().getSupplierCreated()).thenReturn(topic);
        Mockito.when(mockKafkaProducerSupplier.send(any(String.class), any(Supplier.class))).thenReturn(null);

        this.mockMvc
                .perform(post("/suppliers").contentType(MediaType.APPLICATION_JSON).content(supplierDtoJson))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(savedSupplierJson));

        ArgumentCaptor<Supplier> argumentCaptor = ArgumentCaptor.forClass(Supplier.class);
        Mockito.verify(mockSupplierRepo).save(argumentCaptor.capture());
        Supplier capturedArgument = argumentCaptor.getValue();
        assert capturedArgument.getName().equals(savedSupplier.getName());

        Mockito.verify(mockKafkaProducerSupplier).send(topic, savedSupplier);
    }
}