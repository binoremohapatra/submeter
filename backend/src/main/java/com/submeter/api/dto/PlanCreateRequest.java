package com.submeter.api.dto;

import com.submeter.entity.enums.BillingInterval;
import com.submeter.entity.enums.PricingModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class PlanCreateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private PricingModel pricingModel;

    @NotNull
    private BillingInterval billingInterval;

    @PositiveOrZero
    private Long flatAmount;

    @PositiveOrZero
    private Integer trialDays = 0;

    @Valid
    private List<PlanTierDto> tiers;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PricingModel getPricingModel() {
        return pricingModel;
    }

    public void setPricingModel(PricingModel pricingModel) {
        this.pricingModel = pricingModel;
    }

    public BillingInterval getBillingInterval() {
        return billingInterval;
    }

    public void setBillingInterval(BillingInterval billingInterval) {
        this.billingInterval = billingInterval;
    }

    public Long getFlatAmount() {
        return flatAmount;
    }

    public void setFlatAmount(Long flatAmount) {
        this.flatAmount = flatAmount;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        this.trialDays = trialDays;
    }

    public List<PlanTierDto> getTiers() {
        return tiers;
    }

    public void setTiers(List<PlanTierDto> tiers) {
        this.tiers = tiers;
    }
}
