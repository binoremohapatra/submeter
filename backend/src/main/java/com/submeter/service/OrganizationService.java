package com.submeter.service;

import com.submeter.entity.Organization;
import com.submeter.entity.User;
import com.submeter.entity.enums.AuditAction;
import com.submeter.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public record OrganizationSettingsDto(
            String name,
            String logoUrl,
            String timezone,
            String currency,
            String supportEmail,
            Long defaultTaxRate,
            String invoicePrefix,
            String invoiceFooter,
            String companyWebsite,
            String companyAddress
    ) {}

    @Transactional
    public Organization updateOrganizationSettings(UUID orgId, User actor, OrganizationSettingsDto dto) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        Map<String, Object> oldValue = new HashMap<>();
        Map<String, Object> newValue = new HashMap<>();

        if (dto.name() != null && !dto.name().equals(org.getName())) {
            oldValue.put("name", org.getName());
            newValue.put("name", dto.name());
            org.setName(dto.name());
        }
        if (dto.logoUrl() != null && !dto.logoUrl().equals(org.getLogoUrl())) {
            oldValue.put("logoUrl", org.getLogoUrl());
            newValue.put("logoUrl", dto.logoUrl());
            org.setLogoUrl(dto.logoUrl());
        }
        if (dto.supportEmail() != null && !dto.supportEmail().equals(org.getSupportEmail())) {
            oldValue.put("supportEmail", org.getSupportEmail());
            newValue.put("supportEmail", dto.supportEmail());
            org.setSupportEmail(dto.supportEmail());
        }
        if (dto.timezone() != null && !dto.timezone().equals(org.getTimezone())) {
            oldValue.put("timezone", org.getTimezone());
            newValue.put("timezone", dto.timezone());
            org.setTimezone(dto.timezone());
        }
        if (dto.currency() != null && !dto.currency().equals(org.getCurrency())) {
            oldValue.put("currency", org.getCurrency());
            newValue.put("currency", dto.currency());
            org.setCurrency(dto.currency());
        }
        if (dto.defaultTaxRate() != null && !dto.defaultTaxRate().equals(org.getDefaultTaxRate())) {
            oldValue.put("defaultTaxRate", org.getDefaultTaxRate());
            newValue.put("defaultTaxRate", dto.defaultTaxRate());
            org.setDefaultTaxRate(dto.defaultTaxRate());
        }
        if (dto.invoicePrefix() != null && !dto.invoicePrefix().equals(org.getInvoicePrefix())) {
            oldValue.put("invoicePrefix", org.getInvoicePrefix());
            newValue.put("invoicePrefix", dto.invoicePrefix());
            org.setInvoicePrefix(dto.invoicePrefix());
        }
        if (dto.invoiceFooter() != null && !dto.invoiceFooter().equals(org.getInvoiceFooter())) {
            oldValue.put("invoiceFooter", org.getInvoiceFooter());
            newValue.put("invoiceFooter", dto.invoiceFooter());
            org.setInvoiceFooter(dto.invoiceFooter());
        }
        if (dto.companyWebsite() != null && !dto.companyWebsite().equals(org.getCompanyWebsite())) {
            oldValue.put("companyWebsite", org.getCompanyWebsite());
            newValue.put("companyWebsite", dto.companyWebsite());
            org.setCompanyWebsite(dto.companyWebsite());
        }
        if (dto.companyAddress() != null && !dto.companyAddress().equals(org.getCompanyAddress())) {
            oldValue.put("companyAddress", org.getCompanyAddress());
            newValue.put("companyAddress", dto.companyAddress());
            org.setCompanyAddress(dto.companyAddress());
        }

        if (!newValue.isEmpty()) {
            org = organizationRepository.save(org);
            auditService.recordExtended(org, actor, "organization", org.getId(), AuditAction.UPDATE,
                    oldValue, newValue, "organization", org.getName(), true, 0L);
        }

        return org;
    }
}
