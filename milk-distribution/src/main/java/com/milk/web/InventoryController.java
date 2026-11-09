package com.milk.web;

import com.milk.repo.PurchaseRepository;
import com.milk.repo.SupplierRepository;
import com.milk.repo.WastageRepository;
import com.milk.service.EntryService;
import com.milk.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventory;
    private final EntryService entries;
    private final SupplierRepository suppliers;
    private final PurchaseRepository purchases;
    private final WastageRepository wastage;

    public InventoryController(InventoryService inventory, EntryService entries, SupplierRepository suppliers,
                               PurchaseRepository purchases, WastageRepository wastage) {
        this.inventory = inventory;
        this.entries = entries;
        this.suppliers = suppliers;
        this.purchases = purchases;
        this.wastage = wastage;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("stock", inventory.stock());
        model.addAttribute("days", inventory.dailyRows(30));
        model.addAttribute("suppliers", suppliers.findAllByOrderByNameAsc());
        model.addAttribute("recentPurchases", purchases.findTop15ByOrderByPurchaseDateDescIdDesc());
        model.addAttribute("wastageList", wastage.findTop15ByOrderByWasteDateDescIdDesc());
        model.addAttribute("today", LocalDate.now());
        return "inventory";
    }

    @PostMapping("/purchases")
    public String addPurchase(@RequestParam Long supplierId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                              @RequestParam BigDecimal quantity,
                              @RequestParam(required = false) BigDecimal rate,
                              RedirectAttributes ra) {
        entries.addPurchase(supplierId, purchaseDate, quantity, rate);
        ra.addFlashAttribute("msg", "Purchase added to stock.");
        return "redirect:/inventory";
    }

    @PostMapping("/purchases/{id}/delete")
    public String deletePurchase(@PathVariable Long id, RedirectAttributes ra) {
        entries.deletePurchase(id);
        ra.addFlashAttribute("msg", "Purchase deleted.");
        return "redirect:/inventory";
    }

    @PostMapping("/wastage")
    public String addWastage(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate wasteDate,
                             @RequestParam BigDecimal quantity,
                             @RequestParam(required = false) String reason,
                             RedirectAttributes ra) {
        entries.addWastage(wasteDate, quantity, reason);
        ra.addFlashAttribute("msg", "Wastage recorded.");
        return "redirect:/inventory";
    }

    @PostMapping("/wastage/{id}/delete")
    public String deleteWastage(@PathVariable Long id, RedirectAttributes ra) {
        entries.deleteWastage(id);
        ra.addFlashAttribute("msg", "Wastage entry deleted.");
        return "redirect:/inventory";
    }
}
