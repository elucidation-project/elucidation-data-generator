package org.kiwiproject.elucidation.data.home.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    private Long id;
    private String name;
    private String stepJson;

}
