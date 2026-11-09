package com.milk.web;

import com.milk.model.Sale;
import com.milk.repo.PurchaseRepository;
import com.milk.repo.SaleRepository;
import com.milk.service.AccountService;
import com.milk.service.EntryService;
import com.milk.service.InventoryService;
import com.milk.service.Money;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class HomeController {

    private final AccountService accounts;
    private final InventoryService inventory;
    private final EntryService entries;
    private final SaleRepository sales;
    private final PurchaseRepository purchases;

    public HomeController(AccountService accounts, InventoryService inventory, EntryService entries,
                          SaleRepository sales, PurchaseRepository purchases) {
        this.accounts = accounts;
        this.inventory = inventory;
        this.entries = entries;
        this.sales = sales;
        this.purchases = purchases;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now();
        List<Sale> todays = sales.findBySaleDateOrderByCustomerNameAscIdAsc(today);
        BigDecimal litres = todays.stream().map(Sale::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = todays.stream().map(Sale::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("today", today);
        model.addAttribute("stock", inventory.stock());
        model.addAttribute("summary", accounts.summary());
        model.addAttribute("todayLitres", litres);
        model.addAttribute("todayAmount", amount);
        model.addAttribute("todayBought", Money.nz(purchases.sumQuantityOn(today)));
        model.addAttribute("missingToday", entries.customersWithoutDelivery(today).size());
        return "index";
    }
}
