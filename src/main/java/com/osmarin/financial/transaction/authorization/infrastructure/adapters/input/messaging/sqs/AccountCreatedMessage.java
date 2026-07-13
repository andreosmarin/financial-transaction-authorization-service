package com.osmarin.financial.transaction.authorization.infrastructure.adapters.input.messaging.sqs;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountCreatedMessage(
        AccountPayload account
) {

    public record AccountPayload(
            String id,
            String owner,
            @JsonProperty("created_at")
            String createdAt,
            String status
    ) {
    }
}