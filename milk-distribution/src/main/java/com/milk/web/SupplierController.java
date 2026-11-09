package com.milk.web;

import com.milk.model.Supplier;
import com.milk.repo.SupplierRepository;
import com.milk.service.AccountService;
import com.milk.service.AccountService.LedgerEntry;
import com.milk.service.AccountService.SupplierRow;
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
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierRepository suppliers;
    private final AccountService accounts;
    private final EntryService entries;

    public SupplierController(SupplierRepository suppliers, AccountService accounts, EntryService entries) {
        this.suppliers = suppliers;
        this.accounts = accounts;
        this.entries = entries;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, Model model) {
        List<SupplierRow> rows = accounts.supplierRows();
        if (q != null && !q.isBlank()) {
            String s = q.trim().toLowerCase();
            rows = rows.stream()
                    .filter(r -> has(r.supplier().getName(), s)
                            || has(r.supplier().getPhone(), s)
                            || has(r.supplier().getLocation(), s))
                    .toList();
        }
        BigDecimal pending = rows.stream().map(SupplierRow::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchased = rows.stream().map(SupplierRow::purchased).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("rows", rows);
        model.addAttribute("q", q);
        model.addAttribute("pendingTotal", pending);
        model.addAttribute("purchasedTotal", purchased);
        return "suppliers/list";
    }

    private static boolean has(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    @GetMapping("/new")
    public String create(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "suppliers/form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", find(id));
        return "suppliers/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("supplier") Supplier supplier, BindingResult result,
                       RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "suppliers/form";
        }
        suppliers.save(supplier);
        ra.addFlashAttribute("msg", "Supplier saved.");
        return "redirect:/suppliers/" + supplier.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        entries.deleteSupplier(id);
        ra.addFlashAttribute("msg", "Supplier deleted.");
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Supplier s = find(id);
        List<LedgerEntry> ledger = accounts.supplierLedger(id);
        BigDecimal bought = ledger.stream().map(LedgerEntry::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = ledger.stream().map(LedgerEntry::credit).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("supplier", s);
        model.addAttribute("ledger", ledger);
        model.addAttribute("bought", bought);
        model.addAttribute("paid", paid);
        model.addAttribute("balance", bought.subtract(paid));
        model.addAttribute("today", LocalDate.now());
        return "suppliers/detail";
    }

    @PostMapping("/{id}/purchases")
    public String addPurchase(@PathVariable Long id,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                              @RequestParam BigDecimal quantity,
                              @RequestParam(required = false) BigDecimal rate,
                              RedirectAttributes ra) {
        entries.addPurchase(id, purchaseDate, quantity, rate);
        ra.addFlashAttribute("msg", "Purchase added.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/{id}/purchases/{purchaseId}/delete")
    public String deletePurchase(@PathVariable Long id, @PathVariable Long purchaseId, RedirectAttributes ra) {
        entries.deletePurchase(purchaseId);
        ra.addFlashAttribute("msg", "Purchase deleted.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/{id}/payments")
    public String addPayment(@PathVariable Long id,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String note,
                             RedirectAttributes ra) {
        entries.addSupplierPayment(id, paymentDate, amount, note);
        ra.addFlashAttribute("msg", "Payment recorded.");
        return "redirect:/suppliers/" + id;
    }

    @PostMapping("/{id}/payments/{paymentId}/delete")
    public String deletePayment(@PathVariable Long id, @PathVariable Long paymentId, RedirectAttributes ra) {
        entries.deleteSupplierPayment(paymentId);
        ra.addFlashAttribute("msg", "Payment deleted.");
        return "redirect:/suppliers/" + id;
    }

    private Supplier find(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }
}
