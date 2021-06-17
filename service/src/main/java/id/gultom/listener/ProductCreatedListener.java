package id.gultom.listener;

import id.gultom.model.Product;
import id.gultom.model.Supplier;
import id.gultom.repository.couchbase.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCreatedListener {

    @Autowired
    private SupplierRepository supplierRepo;

    /**
     * Add new product to supplier document
     * @param record
     */
    @KafkaListener(topics = "${kafka.topics.product-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ConsumerRecord<String, Product> record) {
        try {
            log.info("Message received in listener class: " + record.toString());
            Product product = record.value();
            Supplier supplier = supplierRepo.findById(product.getSupplierId()).get();
            if (!supplier.getProducts().contains(product.getProductName())) {
                supplier.getProducts().add(product.getProductName());
                supplierRepo.save(supplier);
                log.info("Supplier saved");
            }
        } catch (Exception e) {
            log.error("error", e);
        }
    }
}