package com.submeter.service;

import com.submeter.api.dto.CustomerResponse;
import com.submeter.api.dto.InvoiceResponse;
import com.submeter.api.dto.PlanResponse;
import com.submeter.api.dto.SubscriptionResponse;
import com.submeter.entity.Organization;
import com.submeter.repository.CustomerRepository;
import com.submeter.repository.InvoiceRepository;
import com.submeter.repository.PlanRepository;
import com.submeter.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final CustomerRepository customerRepo;
    private final InvoiceRepository invoiceRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PlanRepository planRepo;

    public static class GlobalSearchResponse {
        private List<CustomerResponse> customers;
        private List<InvoiceResponse> invoices;
        private List<SubscriptionResponse> subscriptions;
        private List<PlanResponse> plans;

        public GlobalSearchResponse(List<CustomerResponse> customers, List<InvoiceResponse> invoices, List<SubscriptionResponse> subscriptions, List<PlanResponse> plans) {
            this.customers = customers;
            this.invoices = invoices;
            this.subscriptions = subscriptions;
            this.plans = plans;
        }

        // Getters and Setters
        public List<CustomerResponse> getCustomers() {
            return customers;
        }

        public void setCustomers(List<CustomerResponse> customers) {
            this.customers = customers;
        }

        public List<InvoiceResponse> getInvoices() {
            return invoices;
        }

        public void setInvoices(List<InvoiceResponse> invoices) {
            this.invoices = invoices;
        }

        public List<SubscriptionResponse> getSubscriptions() {
            return subscriptions;
        }

        public void setSubscriptions(List<SubscriptionResponse> subscriptions) {
            this.subscriptions = subscriptions;
        }

        public List<PlanResponse> getPlans() {
            return plans;
        }

        public void setPlans(List<PlanResponse> plans) {
            this.plans = plans;
        }

        // Builder
        public static GlobalSearchResponseBuilder builder() {
            return new GlobalSearchResponseBuilder();
        }

        public static class GlobalSearchResponseBuilder {
            private List<CustomerResponse> customers;
            private List<InvoiceResponse> invoices;
            private List<SubscriptionResponse> subscriptions;
            private List<PlanResponse> plans;

            public GlobalSearchResponseBuilder customers(List<CustomerResponse> customers) {
                this.customers = customers;
                return this;
            }

            public GlobalSearchResponseBuilder invoices(List<InvoiceResponse> invoices) {
                this.invoices = invoices;
                return this;
            }

            public GlobalSearchResponseBuilder subscriptions(List<SubscriptionResponse> subscriptions) {
                this.subscriptions = subscriptions;
                return this;
            }

            public GlobalSearchResponseBuilder plans(List<PlanResponse> plans) {
                this.plans = plans;
                return this;
            }

            public GlobalSearchResponse build() {
                return new GlobalSearchResponse(customers, invoices, subscriptions, plans);
            }
        }
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse searchGlobal(UUID orgId, String query) {
        Pageable limit5 = PageRequest.of(0, 5);

        var customers = customerRepo.searchGlobal(orgId, query, limit5)
                .stream().map(CustomerResponse::fromEntity).collect(Collectors.toList());

        var invoices = invoiceRepo.searchGlobal(orgId, query, limit5)
                .stream().map(InvoiceResponse::fromEntity).collect(Collectors.toList());

        var subscriptions = subscriptionRepo.searchGlobal(orgId, query, limit5)
                .stream().map(SubscriptionResponse::fromEntity).collect(Collectors.toList());

        var plans = planRepo.searchGlobal(orgId, query, limit5)
                .stream().map(PlanResponse::fromEntity).collect(Collectors.toList());

        return GlobalSearchResponse.builder()
                .customers(customers)
                .invoices(invoices)
                .subscriptions(subscriptions)
                .plans(plans)
                .build();
    }
}
