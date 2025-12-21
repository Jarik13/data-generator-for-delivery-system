package org.example.model.parsed;

import lombok.Data;

@Data
public class ParsedStreet {
    private String ref;
    private String name;
    private Integer cityId;
    private String cityName;
}