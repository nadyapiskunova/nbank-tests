package api.models;

import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginUserResponse extends BaseModel{
    private String username;
    private String role;
}
