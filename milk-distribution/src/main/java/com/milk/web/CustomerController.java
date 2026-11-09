package com.milk.web;

import com.milk.model.Customer;
import com.milk.model.Sale;
import com.milk.repo.CustomerRepository;
import com.milk.repo.SaleRepository;
import com.milk.service.AccountService;
import com.milk.service.AccountService.CustomerRow;
import com.milk.service.AccountService.LedgerEntry;
import com.milk.service.EntryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customers;
    private final SaleRepository sales;
    private final AccountService accounts;
    private final EntryService entries;

    public CustomerController(CustomerRepository customers, SaleRepository sales,
                              AccountService accounts, EntryService entries) {
        this.customers = customers;
        this.sales = sales;
        this.accounts = accounts;
        this.entries = entries;
    }

    // ------------------------------------------------------------------ list / create / edit

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        List<CustomerRow> rows = accounts.customerRows();
        if (q != null && !q.isBlank()) {
            String s = q.trim().toLowerCase();
            rows = rows.stream()
                    .filter(r -> has(r.customer().getName(), s)
                            || has(r.customer().getPhone(), s)
                            || has(r.customer().getLocation(), s))
                    .toList();
        }
        BigDecimal pending = rows.stream().map(CustomerRow::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal daily = rows.stream().filter(r -> r.customer().isActive())
                .map(r -> r.customer().getDailyQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("rows", rows);
        model.addAttribute("q", q);
        model.addAttribute("pendingTotal", pending);
        model.addAttribute("dailyTotal", daily);
        return "customers/list";
    }

    private static boolean has(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("customer", new Customer());
        return "customers/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("customer", find(id));
        return "customers/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("customer") Customer customer, BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "customers/form";
        }
        customers.save(customer);
        ra.addFlashAttribute("msg", "Customer saved.");
        return "redirect:/customers/" + customer.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        entries.deleteCustomer(id);
        ra.addFlashAttribute("msg", "Customer deleted.");
        return "redirect:/customers";
    }

    // ------------------------------------------------------------------ customer page (ledger)

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Customer c = find(id);
        List<LedgerEntry> ledger = accounts.customerLedger(id);
        BigDecimal billed = ledger.stream().map(LedgerEntry::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = ledger.stream().map(LedgerEntry::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("customer", c);
        model.addAttribute("ledger", ledger);
        model.addAttribute("billed", billed);
        model.addAttribute("paid", paid);
        model.addAttribute("balance", billed.subtract(paid));
        model.addAttribute("today", LocalDate.now());
        return "customers/detail";
    }

    @PostMapping("/{id}/sales")
    public String addSale(@PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate saleDate,
                          @RequestParam BigDecimal quantity,
                          @RequestParam(required = false) BigDecimal rate,
                          RedirectAttributes ra) {
        entries.addSale(id, saleDate, quantity, rate);
        ra.addFlashAttribute("msg", "Delivery added.");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/sales/{saleId}/delete")
    public String deleteSale(@PathVariable Long id, @PathVariable Long saleId, RedirectAttributes ra) {
        entries.deleteSale(saleId);
        ra.addFlashAttribute("msg", "Delivery deleted.");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/payments")
    public String addPayment(@PathVariable Long id,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String note,
                             RedirectAttributes ra) {
        entries.addCustomerPayment(id, paymentDate, amount, note);
        ra.addFlashAttribute("msg", "Payment recorded.");
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/payments/{paymentId}/delete")
    public String deletePayment(@PathVariable Long id, @PathVariable Long paymentId, RedirectAttributes ra) {
        entries.deleteCustomerPayment(paymentId);
        ra.addFlashAttribute("msg", "Payment deleted.");
        return "redirect:/customers/" + id;
    }

    // ------------------------------------------------------------------ daily deliveries

    @GetMapping("/daily")
    public String daily(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        LocalDate d = date == null ? LocalDate.now() : date;
        List<Sale> list = sales.findBySaleDateOrderByCustomerNameAscIdAsc(d);
        BigDecimal litres = list.stream().map(Sale::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = list.stream().map(Sale::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("date", d);
        model.addAttribute("sales", list);
        model.addAttribute("litres", litres);
        model.addAttribute("amount", amount);
        model.addAttribute("missing", entries.customersWithoutDelivery(d));
        return "customers/daily";
    }

    @PostMapping("/daily/generate")
    public String generate(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes ra) {
        int n = entries.generateDeliveries(date);
        ra.addFlashAttribute("msg", n + " deliveries recorded for " + date + ".");
        return "redirect:/customers/daily?date=" + date;
    }

    @PostMapping("/daily/{saleId}/quantity")
    public String updateQuantity(@PathVariable Long saleId, @RequestParam BigDecimal quantity,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 RedirectAttributes ra) {
        entries.updateSaleQuantity(saleId, quantity);
        ra.addFlashAttribute("msg", "Quantity updated.");
        return "redirect:/customers/daily?date=" + date;
    }

    @PostMapping("/daily/{saleId}/delete")
    public String deleteDaily(@PathVariable Long saleId,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              RedirectAttributes ra) {
        entries.deleteSale(saleId);
        ra.addFlashAttribute("msg", "Delivery removed.");
        return "redirect:/customers/daily?date=" + date;
    }

    // ------------------------------------------------------------------

    private Customer find(Long id) {
        return customers.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }
}
