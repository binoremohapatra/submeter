package com.submeter.api;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.CustomerCreateRequest;
import com.submeter.api.dto.CustomerResponse;
import com.submeter.entity.Customer;
import com.submeter.entity.enums.Role;
import com.submeter.repository.CustomerRepository;
import com.submeter.security.UserPrincipal;
import com.submeter.security.rbac.RequiresRole;
import com.submeter.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepo;

    @GetMapping
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CursorPageResponse<CustomerResponse>> listCustomers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search
    ) {
        int safeLimit = Math.min(limit, 100);
        return ResponseEntity.ok(customerService.listCustomers(principal.getOrgId(), cursor, safeLimit, search));
    }

    @GetMapping("/{id}")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<CustomerResponse> getCustomer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Customer customer = customerRepo.findByIdWithSubscriptionsAndOrganizationIdAndDeletedAtIsNull(id, principal.getOrgId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Customer not found"));
        return ResponseEntity.ok(CustomerResponse.fromEntity(customer));
    }

    @PutMapping("/{id}")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<CustomerResponse> updateCustomer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody CustomerCreateRequest req) {
        Customer customer = customerRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(id, principal.getOrgId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Customer not found"));
        customer.setName(req.getName());
        customer.setEmail(req.getEmail());
        customer = customerRepo.save(customer);
        return ResponseEntity.ok(CustomerResponse.fromEntity(customer));
    }

    @DeleteMapping("/{id}")
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<Void> deleteCustomer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        Customer customer = customerRepo.findByIdAndOrganizationIdAndDeletedAtIsNull(id, principal.getOrgId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Customer not found"));
        customer.softDelete();
        customerRepo.save(customer);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @RequiresRole(minimum = Role.ADMIN)
    public ResponseEntity<CustomerResponse> createCustomer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(principal.getOrgId(), principal.getUserId(), req));
    }

    @GetMapping("/export")
    @RequiresRole(minimum = Role.MEMBER)
    public ResponseEntity<byte[]> exportCustomers(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] csv = customerService.exportCsv(principal.getOrgId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
