package org.kiwiproject.elucidation.data.home.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {

    private String description;
    private Device device;
    private String eventAction;
    private Map<String, Object> eventInfo;
    private int nextStepDelayInSeconds;

}
