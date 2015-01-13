package org.oiavorskyi.axondemo.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CargoTrackingCommandMessage {

    public final String commandId;

    @JsonProperty( required = true )
    public final String cargoId;

    @JsonProperty( required = true )
    public final String correlationId;

    @JsonProperty( required = true )
    public final String timestamp;

    @JsonCreator
    public CargoTrackingCommandMessage(
            @JsonProperty( value = "commandId", required = true ) String commandId,
            @JsonProperty( value = "cargoId", required = true ) String cargoId,
            @JsonProperty( value = "correlationId", required = true ) String correlationId,
            @JsonProperty( value = "timestamp", required = true ) String timestamp
    ) {
        this.commandId = commandId;
        this.cargoId = cargoId;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CargoTrackingCommandMessage{");
        sb.append("commandId='").append(commandId).append('\'');
        sb.append(", cargoId='").append(cargoId).append('\'');
        sb.append(", correlationId='").append(correlationId).append('\'');
        sb.append(", timestamp='").append(timestamp).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
