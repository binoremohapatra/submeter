package com.submeter.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class PlanTierDto {

    /** Upper bound of the tier. Null means infinity (last tier). */
    @PositiveOrZero
    private Long upTo;

    /** Fixed fee applied to this tier (in paisa). */
    @NotNull
    @PositiveOrZero
    private Long flatFee;

    /** Per-unit fee for usage in this tier (in paisa). */
    @NotNull
    @PositiveOrZero
    private Long unitAmount;

    // Getters and Setters
    public Long getUpTo() {
        return upTo;
    }

    public void setUpTo(Long upTo) {
        this.upTo = upTo;
    }

    public Long getFlatFee() {
        return flatFee;
    }

    public void setFlatFee(Long flatFee) {
        this.flatFee = flatFee;
    }

    public Long getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(Long unitAmount) {
        this.unitAmount = unitAmount;
    }

    // Builder
    public static PlanTierDtoBuilder builder() {
        return new PlanTierDtoBuilder();
    }

    public static class PlanTierDtoBuilder {
        private Long upTo;
        private Long flatFee;
        private Long unitAmount;

        public PlanTierDtoBuilder upTo(Long upTo) {
            this.upTo = upTo;
            return this;
        }

        public PlanTierDtoBuilder flatFee(Long flatFee) {
            this.flatFee = flatFee;
            return this;
        }

        public PlanTierDtoBuilder unitAmount(Long unitAmount) {
            this.unitAmount = unitAmount;
            return this;
        }

        public PlanTierDto build() {
            PlanTierDto dto = new PlanTierDto();
            dto.setUpTo(upTo);
            dto.setFlatFee(flatFee);
            dto.setUnitAmount(unitAmount);
            return dto;
        }
    }
}
