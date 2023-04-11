package org.kiwiproject.elucidation.data.home.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Device {

    public enum DeviceType {
        APPLIANCE, CAMERA, DOORBELL, LIGHT, THERMOSTAT
    }

    private Long id;
    private String name;
    private DeviceType deviceType;
    private Long deviceTypeId;

}
