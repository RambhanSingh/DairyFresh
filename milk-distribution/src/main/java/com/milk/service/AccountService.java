package com.milk.service;

import com.milk.model.*;
import com.milk.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.milk.service.Money.nz;
import static com.milk.service.Money.r2;

/** Money calculations: customer / supplier balances, ledgers and the account summary. */
@Service
@Transactional(readOnly = true)
public class AccountService {

    public record Summary(BigDecimal invested, BigDecimal paidToSuppliers, BigDecimal pendingToSuppliers,
                          BigDecimal totalSales, BigDecimal received, BigDecimal pendingCollection,
                          BigDecimal netCash, BigDecimal estimatedProfit) {}

    public record CustomerRow(Customer customer, BigDecimal billed, BigDecimal paid, BigDecimal balance) {}

    public record SupplierRow(Supplier supplier, BigDecimal purchased, BigDecimal paid, BigDecimal balance) {}

    /** type is "BILL" (milk delivered / milk bought) or "PAYMENT". */
    public record LedgerEntry(String type, Long id, LocalDate date, String description,
                              BigDecimal debit, BigDecimal credit, BigDecimal balance) {}

    private record Raw(String type, Long id, LocalDate date, String description,
                       BigDecimal debit, BigDecimal credit) {}

    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CustomerPaymentRepository customerPayments;
    private final SupplierPaymentRepository supplierPayments;
    private final WastageRepository wastage;

    public AccountService(CustomerRepository customers, SupplierRepository suppliers, SaleRepository sales,
                          PurchaseRepository purchases, CustomerPaymentRepository customerPayments,
                          SupplierPaymentRepository supplierPayments, WastageRepository wastage) {
        this.customers = customers;
        this.suppliers = suppliers;
        this.sales = sales;
        this.purchases = purchases;
        this.customerPayments = customerPayments;
        this.supplierPayments = supplierPayments;
        this.wastage = wastage;
    }

    // ------------------------------------------------------------------ summary

    public Summary summary() {
        BigDecimal invested = nz(purchases.sumAmount());
        BigDecimal paidToSuppliers = nz(supplierPayments.sumAmount());
        BigDecimal totalSales = nz(sales.sumAmount());
        BigDecimal received = nz(customerPayments.sumAmount());

        BigDecimal boughtL = nz(purchases.sumQuantity());
        BigDecimal soldL = nz(sales.sumQuantity());
        BigDecimal wastedL = nz(wastage.sumQuantity());
        BigDecimal avgCost = boughtL.signum() > 0
                ? invested.divide(boughtL, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        // profit = sales - cost of the milk that was sold or wasted (at average purchase cost)
        BigDecimal profit = totalSales.subtract(soldL.add(wastedL).multiply(avgCost));

        return new Summary(
                r2(invested),
                r2(paidToSuppliers),
                r2(invested.subtract(paidToSuppliers)),
                r2(totalSales),
                r2(received),
                r2(totalSales.subtract(received)),
                r2(received.subtract(paidToSuppliers)),
                r2(profit));
    }

    // ------------------------------------------------------------------ balances per person

    public List<CustomerRow> customerRows() {
        Map<Long, BigDecimal> billed = Money.toMap(sales.amountByCustomer());
        Map<Long, BigDecimal> paid = Money.toMap(customerPayments.amountByCustomer());
        return customers.findAllByOrderByNameAsc().stream().map(c -> {
            BigDecimal b = billed.getOrDefault(c.getId(), BigDecimal.ZERO);
            BigDecimal p = paid.getOrDefault(c.getId(), BigDecimal.ZERO);
            return new CustomerRow(c, r2(b), r2(p), r2(b.subtract(p)));
        }).toList();
    }

    public List<SupplierRow> supplierRows() {
        Map<Long, BigDecimal> bought = Money.toMap(purchases.amountBySupplier());
        Map<Long, BigDecimal> paid = Money.toMap(supplierPayments.amountBySupplier());
        return suppliers.findAllByOrderByNameAsc().stream().map(s -> {
            BigDecimal b = bought.getOrDefault(s.getId(), BigDecimal.ZERO);
            BigDecimal p = paid.getOrDefault(s.getId(), BigDecimal.ZERO);
            return new SupplierRow(s, r2(b), r2(p), r2(b.subtract(p)));
        }).toList();
    }

    /** Customers who still owe money, biggest first. */
    public List<CustomerRow> pendingCustomers() {
        return customerRows().stream()
                .filter(r -> r.balance().signum() > 0)
                .sorted(Comparator.comparing(CustomerRow::balance).reversed())
                .toList();
    }

    /** Suppliers we still have to pay, biggest first. */
    public List<SupplierRow> pendingSuppliers() {
        return supplierRows().stream()
                .filter(r -> r.balance().signum() > 0)
                .sorted(Comparator.comparing(SupplierRow::balance).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ ledgers (newest first)

    public List<LedgerEntry> customerLedger(Long customerId) {
        List<Raw> raws = new ArrayList<>();
        for (Sale s : sales.findByCustomerIdOrderBySaleDateAscIdAsc(customerId)) {
            raws.add(new Raw("BILL", s.getId(), s.getSaleDate(),
                    r2(s.getQuantity()) + " L x Rs " + r2(s.getRate()),
                    s.getAmount(), BigDecimal.ZERO));
        }
        for (CustomerPayment p : customerPayments.findByCustomerIdOrderByPaymentDateAscIdAsc(customerId)) {
            raws.add(new Raw("PAYMENT", p.getId(), p.getPaymentDate(),
                    describe("Payment received", p.getNote()),
                    BigDecimal.ZERO, p.getAmount()));
        }
        return buildLedger(raws);
    }

    public List<LedgerEntry> supplierLedger(Long supplierId) {
        List<Raw> raws = new ArrayList<>();
        for (Purchase p : purchases.findBySupplierIdOrderByPurchaseDateAscIdAsc(supplierId)) {
            raws.add(new Raw("BILL", p.getId(), p.getPurchaseDate(),
                    r2(p.getQuantity()) + " L x Rs " + r2(p.getRate()),
                    p.getAmount(), BigDecimal.ZERO));
        }
        for (SupplierPayment p : supplierPayments.findBySupplierIdOrderByPaymentDateAscIdAsc(supplierId)) {
            raws.add(new Raw("PAYMENT", p.getId(), p.getPaymentDate(),
                    describe("Payment made", p.getNote()),
                    BigDecimal.ZERO, p.getAmount()));
        }
        return buildLedger(raws);
    }

    private static String describe(String base, String note) {
        return note == null || note.isBlank() ? base : base + " - " + note;
    }

    private static List<LedgerEntry> buildLedger(List<Raw> raws) {
        raws.sort(Comparator.comparing(Raw::date).thenComparing(Raw::type).thenComparing(Raw::id));
        List<LedgerEntry> result = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (Raw r : raws) {
            running = running.add(r.debit()).subtract(r.credit());
            result.add(new LedgerEntry(r.type(), r.id(), r.date(), r.description(),
                    r.debit(), r.credit(), running));
        }
        Collections.reverse(result);
        return result;
    }
}
