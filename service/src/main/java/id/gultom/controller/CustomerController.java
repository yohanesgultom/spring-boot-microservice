package id.gultom.controller;

import id.gultom.dto.CustomerDto;
import id.gultom.model.Customer;
import id.gultom.repository.couchbase.CustomerRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@Slf4j
@Api(tags = "Customer")
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepo;

    @PostMapping
    @ApiOperation(value = "Create Customer", nickname = "customers-create")
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerDto dto) {
        Customer customer = new Customer(dto.getName());
        Customer savedCustomer = this.customerRepo.save(customer);
        log.info("Customer created: " + savedCustomer.toString());
        return ResponseEntity.ok(savedCustomer);
    }
}
