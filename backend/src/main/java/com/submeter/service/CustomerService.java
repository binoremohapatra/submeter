package com.submeter.service;

import com.submeter.api.dto.CursorPageResponse;
import com.submeter.api.dto.CustomerCreateRequest;
import com.submeter.api.dto.CustomerResponse;
import com.submeter.entity.Customer;
import com.submeter.entity.Organization;
import com.submeter.repository.CustomerRepository;
import com.submeter.repository.OrganizationRepository;
import com.submeter.repository.UserRepository;
import com.submeter.util.CursorUtil;
import com.submeter.entity.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepo;
    private final OrganizationRepository orgRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CursorPageResponse<CustomerResponse> listCustomers(UUID orgId, String cursor, int limit, String search) {
        // Fetch limit + 1 to determine if there is a next page
        int fetchSize = Math.min(limit, 100) + 1;
        PageRequest pageRequest = PageRequest.of(0, fetchSize);

        List<Customer> customers;
        if (cursor == null || cursor.isBlank()) {
            customers = customerRepo.findFirstPage(orgId, pageRequest);
        } else {
            CursorUtil.Cursor parsed = CursorUtil.parse(cursor);
            if (parsed == null) {
                throw new IllegalArgumentException("Invalid cursor");
            }
            customers = customerRepo.findNextPage(orgId, parsed.createdAt(), parsed.id(), pageRequest);
        }

        boolean hasNext = customers.size() > limit;
        if (hasNext) {
            customers = customers.subList(0, limit);
        }

        String nextCursor = null;
        if (hasNext && !customers.isEmpty()) {
            Customer last = customers.get(customers.size() - 1);
            nextCursor = CursorUtil.encode(last.getCreatedAt(), last.getId());
        }

        List<CustomerResponse> dtos = customers.stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());

        return new CursorPageResponse<>(dtos, nextCursor, hasNext);
    }

    @Transactional
    public CustomerResponse createCustomer(UUID orgId, UUID actorId, CustomerCreateRequest req) {
        if (customerRepo.existsByEmailAndOrganizationIdAndDeletedAtIsNull(req.getEmail(), orgId)) {
            throw new IllegalArgumentException("Customer with this email already exists.");
        }

        Organization org = orgRepo.getReferenceById(orgId);

        Customer customer = Customer.builder()
                .organization(org)
                .name(req.getName())
                .email(req.getEmail())
                .build();

        customer = customerRepo.save(customer);

        auditService.record(
                org,
                userRepo.getReferenceById(actorId),
                "customer",
                customer.getId(),
                AuditAction.CREATE,
                java.util.Map.of("state", req)
        );

        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID orgId) {
        List<Customer> customers = customerRepo.findAllByOrganizationIdAndDeletedAtIsNull(orgId);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Name,Email,Created At\n");
        for (Customer c : customers) {
            sb.append(c.getId()).append(",")
              .append("\"").append(c.getName().replace("\"", "\"\"")).append("\",")
              .append("\"").append(c.getEmail()).append("\",")
              .append(c.getCreatedAt()).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
