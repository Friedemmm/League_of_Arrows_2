package com.backend.LeagueOfArrows.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistanceCheckDTO {
    private BigDecimal distanciaRealM;
    private BigDecimal distanciaNormativaM;
    private Boolean cumpleNormativa;
}
