package com.fortitudetec.elucidation.data.appliance.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class Event {

    private String uuid;
    private String action;
    private Map<String, Object> value;
    private Long iotLookup;

}
