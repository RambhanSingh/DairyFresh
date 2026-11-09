package com.milk.service;

import com.milk.model.*;
import com.milk.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** All the "add / change / delete an entry" operations, with validation. */
@Service
@Transactional
public class EntryService {

    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;
    private final CustomerPaymentRepository customerPayments;
    private final SupplierPaymentRepository supplierPayments;
    private final WastageRepository wastage;

    public EntryService(CustomerRepository customers, SupplierRepository suppliers, SaleRepository sales,
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

    // ------------------------------------------------------------------ deliveries (sales)

    public Sale addSale(Long customerId, LocalDate date, BigDecimal quantity, BigDecimal rate) {
        Customer c = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        Sale s = new Sale();
        s.setCustomer(c);
        s.setSaleDate(date == null ? LocalDate.now() : date);
        s.setQuantity(positive(quantity, "Quantity"));
        s.setRate(rate == null ? c.getRatePerLitre() : nonNegative(rate, "Rate"));
        s.recalc();
        return sales.save(s);
    }

    /** Creates a delivery (daily quantity x rate) for every active customer that has none on that date. */
    public int generateDeliveries(LocalDate date) {
        int created = 0;
        for (Customer c : customersWithoutDelivery(date)) {
            Sale s = new Sale();
            s.setCustomer(c);
            s.setSaleDate(date);
            s.setQuantity(c.getDailyQuantity());
            s.setRate(c.getRatePerLitre());
            s.recalc();
            sales.save(s);
            created++;
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<Customer> customersWithoutDelivery(LocalDate date) {
        return customers.findByActiveTrueOrderByNameAsc().stream()
                .filter(c -> c.getDailyQuantity() != null && c.getDailyQuantity().signum() > 0)
                .filter(c -> !sales.existsByCustomerIdAndSaleDate(c.getId(), date))
                .toList();
    }

    /** Changes the quantity of a delivery. Quantity 0 removes the delivery. */
    public void updateSaleQuantity(Long saleId, BigDecimal quantity) {
        if (quantity == null || quantity.signum() < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        Sale s = sales.findById(saleId).orElseThrow(() -> new IllegalArgumentException("Delivery not found"));
        if (quantity.signum() == 0) {
            sales.delete(s);
            return;
        }
        s.setQuantity(quantity.setScale(2, RoundingMode.HALF_UP));
        s.recalc();
        sales.save(s);
    }

    public void deleteSale(Long saleId) {
        sales.deleteById(saleId);
    }

    // ------------------------------------------------------------------ customer payments

    public CustomerPayment addCustomerPayment(Long customerId, LocalDate date, BigDecimal amount, String note) {
        Customer c = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        CustomerPayment p = new CustomerPayment();
        p.setCustomer(c);
        p.setPaymentDate(date == null ? LocalDate.now() : date);
        p.setAmount(positive(amount, "Amount"));
        p.setNote(clean(note));
        return customerPayments.save(p);
    }

    public void deleteCustomerPayment(Long id) {
        customerPayments.deleteById(id);
    }

    // ------------------------------------------------------------------ purchases (milk bought)

    public Purchase addPurchase(Long supplierId, LocalDate date, BigDecimal quantity, BigDecimal rate) {
        Supplier sup = suppliers.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Choose a supplier"));
        BigDecimal useRate = rate != null ? rate : sup.getRatePerLitre();
        Purchase p = new Purchase();
        p.setSupplier(sup);
        p.setPurchaseDate(date == null ? LocalDate.now() : date);
        p.setQuantity(positive(quantity, "Quantity"));
        p.setRate(positive(useRate, "Rate"));
        p.recalc();
        return purchases.save(p);
    }

    public void deletePurchase(Long id) {
        purchases.deleteById(id);
    }

    // ------------------------------------------------------------------ supplier payments

    public SupplierPayment addSupplierPayment(Long supplierId, LocalDate date, BigDecimal amount, String note) {
        Supplier sup = suppliers.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        SupplierPayment p = new SupplierPayment();
        p.setSupplier(sup);
        p.setPaymentDate(date == null ? LocalDate.now() : date);
        p.setAmount(positive(amount, "Amount"));
        p.setNote(clean(note));
        return supplierPayments.save(p);
    }

    public void deleteSupplierPayment(Long id) {
        supplierPayments.deleteById(id);
    }

    // ------------------------------------------------------------------ wastage

    public Wastage addWastage(LocalDate date, BigDecimal quantity, String reason) {
        Wastage w = new Wastage();
        w.setWasteDate(date == null ? LocalDate.now() : date);
        w.setQuantity(positive(quantity, "Quantity"));
        w.setReason(clean(reason));
        return wastage.save(w);
    }

    public void deleteWastage(Long id) {
        wastage.deleteById(id);
    }

    // ------------------------------------------------------------------ deleting people

    public void deleteCustomer(Long id) {
        if (sales.existsByCustomerId(id) || customerPayments.existsByCustomerId(id)) {
            throw new IllegalArgumentException(
                    "This customer has deliveries or payments, so it cannot be deleted. Mark the customer as inactive instead.");
        }
        customers.deleteById(id);
    }

    public void deleteSupplier(Long id) {
        if (purchases.existsBySupplierId(id) || supplierPayments.existsBySupplierId(id)) {
            throw new IllegalArgumentException(
                    "This supplier has purchases or payments, so it cannot be deleted.");
        }
        suppliers.deleteById(id);
    }

    // ------------------------------------------------------------------ helpers

    private static BigDecimal positive(BigDecimal v, String label) {
        if (v == null || v.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than 0");
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nonNegative(BigDecimal v, String label) {
        if (v == null || v.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
