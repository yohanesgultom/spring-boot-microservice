package id.gultom.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.Field;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;

import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.springframework.data.couchbase.core.mapping.id.GenerationStrategy.UNIQUE;

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Customer {

    @Id
    @NotNull
    @GeneratedValue(strategy = UNIQUE)
    @ApiModelProperty(value = "Customer ID", example = "315284cf-3922-4df6-9c68-f11ea7c1f656")
    private String id;

    @Field
    @NotNull
    @ApiModelProperty(value = "Customer name", example = "Jane Doe")
    private String name;

    @Field
    @NotNull
    private Date createdAt;

    @Field
    private Date updatedAt;

    public Customer(String name) {
        Date now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
        this.name = name;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
