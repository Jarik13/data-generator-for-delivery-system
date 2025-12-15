package org.example.model.parsed;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ParsedPack {
    private String ref;
    private String description;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
}