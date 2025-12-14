package org.example.model.parsed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParsedCity {
    private String description;
    private String region;
    private String area;
    private String ref;
    private String type;
}
