package org.kiwiproject.elucidation.data.light.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartLight {

    public enum State {
        ON, OFF
    }

    public enum Color {
        SOFT_WHITE, BRIGHT_WHITE, DAY_LIGHT, BLUE, RED, YELLOW, ORANGE, GREEN, PURPLE
    }

    private Long id;
    private String name;
    private String brand;
    private String location;

    @Builder.Default
    private State state = State.OFF;

    @Builder.Default
    private Color color = Color.SOFT_WHITE;

    @Builder.Default
    private int brightness = 100;

}
