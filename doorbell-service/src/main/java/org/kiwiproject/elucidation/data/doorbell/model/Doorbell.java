package org.kiwiproject.elucidation.data.doorbell.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doorbell {

    private Long id;
    private String name;
    private String brand;

}
