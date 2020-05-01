package com.fortitudetec.elucidation.data.appliance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appliance {

    public enum State {
        ON, OFF
    }

    private Long id;
    private String name;
    private String brand;
    private String location;
    private State state;

}
