package com.backend.LeagueOfArrows.dtos;

import lombok.Data;

@Data
public class ArcherPositionDTO {
    private Double lon;
    private Double lat;
    private Long targetId;
}
