package com.sedroad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelProfile {
    private Integer speed;
    private Integer stamina;
    private Integer budget;
    private Integer photo;
    private Integer tradition;
}

