package id.gultom.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.gultom.config.KafkaProperties;
import id.gultom.dto.ProductDto;
import id.gultom.listener.ProductCreatedListener;
import id.gultom.model.Product;
import id.gultom.repository.ProductRepository;
import id.gultom.repository.SupplierRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    ProductRepository mockProductRepo;

    @MockBean
    SupplierRepository mockSupplierRepo;

    @MockBean
    KafkaTemplate<String, Product> mockKafkaProducer;

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
        ProductDto productDto = new ProductDto("My Product", "supplier-1");
        String productDtoJson = objectMapper.writeValueAsString(productDto);

        Product savedProduct = new Product(1l, productDto.getProductName(), productDto.getSupplierId());
        String savedProductJson = objectMapper.writeValueAsString(savedProduct);

        Mockito.when(mockProductRepo.save(any(Product.class))).thenReturn(savedProduct);
        Mockito.when(mockKafkaProperties.getTopics().getProductCreated()).thenReturn("product_created");
        Mockito.when(mockKafkaProducer.send(any(String.class), any(Product.class))).thenReturn(null);

        this.mockMvc
                .perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(productDtoJson))
                .andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(savedProductJson))
                .andExpect(status().is(200));

        ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);
        Mockito.verify(mockProductRepo).save(argumentCaptor.capture());
        Product capturedArgument = argumentCaptor.getValue();
        assert capturedArgument.getProductName().equals(savedProduct.getProductName());

        Mockito.verify(mockKafkaProducer).send("product_created", savedProduct);
    }

}