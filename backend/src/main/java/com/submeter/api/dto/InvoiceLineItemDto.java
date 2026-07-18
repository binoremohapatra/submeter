package com.submeter.api.dto;

import com.submeter.entity.InvoiceLineItem;
import com.submeter.entity.enums.PricingModel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

public class InvoiceLineItemDto {
    private final UUID id;
    private final String description;
    private final long quantity;
    private final long unitAmount;
    private final long amount;
    private final PricingModel pricingModel;
    private final Map<String, Object> tierDetail;

    public InvoiceLineItemDto(UUID id, String description, long quantity, long unitAmount, long amount, PricingModel pricingModel, Map<String, Object> tierDetail) {
        this.id = id;
        this.description = description;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.amount = amount;
        this.pricingModel = pricingModel;
        this.tierDetail = tierDetail;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getUnitAmount() {
        return unitAmount;
    }

    public long getAmount() {
        return amount;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public Map<String, Object> getTierDetail() {
        return tierDetail;
    }

    public static InvoiceLineItemDto fromEntity(InvoiceLineItem item) {
        return InvoiceLineItemDto.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitAmount(item.getUnitAmount())
                .amount(item.getAmount())
                .pricingModel(item.getPricingModel())
                .tierDetail(item.getTierDetail())
                .build();
    }

    // Builder
    public static InvoiceLineItemDtoBuilder builder() {
        return new InvoiceLineItemDtoBuilder();
    }

    public static class InvoiceLineItemDtoBuilder {
        private UUID id;
        private String description;
        private long quantity;
        private long unitAmount;
        private long amount;
        private PricingModel pricingModel;
        private Map<String, Object> tierDetail;

        public InvoiceLineItemDtoBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public InvoiceLineItemDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public InvoiceLineItemDtoBuilder quantity(long quantity) {
            this.quantity = quantity;
            return this;
        }

        public InvoiceLineItemDtoBuilder unitAmount(long unitAmount) {
            this.unitAmount = unitAmount;
            return this;
        }

        public InvoiceLineItemDtoBuilder amount(long amount) {
            this.amount = amount;
            return this;
        }

        public InvoiceLineItemDtoBuilder pricingModel(PricingModel pricingModel) {
            this.pricingModel = pricingModel;
            return this;
        }

        public InvoiceLineItemDtoBuilder tierDetail(Map<String, Object> tierDetail) {
            this.tierDetail = tierDetail;
            return this;
        }

        public InvoiceLineItemDto build() {
            return new InvoiceLineItemDto(id, description, quantity, unitAmount, amount, pricingModel, tierDetail);
        }
    }
}
