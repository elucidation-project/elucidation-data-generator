package com.fortitudetec.elucidation.data.thermostat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Thermostat {

    private Long id;
    private String name;
    private String brand;
    private String location;
    private Double currentTemp;

}
