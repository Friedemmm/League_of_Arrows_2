package com.backend.LeagueOfArrows.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentalCorrelationDTO {
    private String zoneType;
    private String intensity;
    private Long roundsEvaluated;
    private BigDecimal avgPrecision;
    private Double correlationCoefficient;
}
