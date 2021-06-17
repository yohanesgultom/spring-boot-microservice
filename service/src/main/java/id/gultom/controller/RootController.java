package id.gultom.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@Api(tags = "Health")
@RequestMapping("/")
public class RootController {
    @GetMapping("")
    public Map<String, Object> index() {
        return Map.of("message", "Hello, World!");
    }
}
