package org.example.model.parsed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedDistrict {
    private String ref;
    private String name;
    private String regionRef;
    private String regionName;
}
