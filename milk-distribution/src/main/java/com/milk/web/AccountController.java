package com.milk.web;

import com.milk.repo.CustomerPaymentRepository;
import com.milk.repo.SupplierPaymentRepository;
import com.milk.service.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accounts;
    private final CustomerPaymentRepository customerPayments;
    private final SupplierPaymentRepository supplierPayments;

    public AccountController(AccountService accounts, CustomerPaymentRepository customerPayments,
                             SupplierPaymentRepository supplierPayments) {
        this.accounts = accounts;
        this.customerPayments = customerPayments;
        this.supplierPayments = supplierPayments;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("summary", accounts.summary());
        model.addAttribute("pendingCustomers", accounts.pendingCustomers());
        model.addAttribute("pendingSuppliers", accounts.pendingSuppliers());
        model.addAttribute("recentReceived", customerPayments.findTop15ByOrderByPaymentDateDescIdDesc());
        model.addAttribute("recentPaid", supplierPayments.findTop15ByOrderByPaymentDateDescIdDesc());
        return "accounts";
    }
}
