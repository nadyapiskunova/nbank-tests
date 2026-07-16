package api.models;

import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateProfileRequest extends BaseModel {
    private String name;
}
