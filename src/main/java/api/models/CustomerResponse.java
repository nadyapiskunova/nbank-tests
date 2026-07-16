package api.models;

import lombok.*;

import java.util.List;
@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerResponse extends BaseModel {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String role;
    private List<AccountResponse> accounts;
}
