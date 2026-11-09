package com.milk.service;

import com.milk.model.Customer;
import com.milk.repo.CustomerRepository;
import com.milk.repo.PurchaseRepository;
import com.milk.repo.SaleRepository;
import com.milk.repo.WastageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static com.milk.service.Money.bd;
import static com.milk.service.Money.nz;
import static com.milk.service.Money.r2;

/** Milk stock: how many litres bought, sold, wasted and left. */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    public record Stock(BigDecimal purchased, BigDecimal sold, BigDecimal wasted, BigDecimal inStock,
                        BigDecimal invested, BigDecimal avgCost, BigDecimal stockValue,
                        BigDecimal dailyDemand, BigDecimal daysCover) {}

    public record DayRow(LocalDate date, BigDecimal bought, BigDecimal sold, BigDecimal wasted,
                         BigDecimal closing) {}

    private final PurchaseRepository purchases;
    private final SaleRepository sales;
    private final WastageRepository wastage;
    private final CustomerRepository customers;

    public InventoryService(PurchaseRepository purchases, SaleRepository sales,
                            WastageRepository wastage, CustomerRepository customers) {
        this.purchases = purchases;
        this.sales = sales;
        this.wastage = wastage;
        this.customers = customers;
    }

    public Stock stock() {
        BigDecimal bought = nz(purchases.sumQuantity());
        BigDecimal sold = nz(sales.sumQuantity());
        BigDecimal wasted = nz(wastage.sumQuantity());
        BigDecimal invested = nz(purchases.sumAmount());
        BigDecimal inStock = bought.subtract(sold).subtract(wasted);

        BigDecimal avgCost = bought.signum() > 0
                ? invested.divide(bought, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal stockValue = inStock.signum() > 0 ? inStock.multiply(avgCost) : BigDecimal.ZERO;

        BigDecimal demand = BigDecimal.ZERO;
        for (Customer c : customers.findByActiveTrueOrderByNameAsc()) {
            demand = demand.add(nz(c.getDailyQuantity()));
        }
        BigDecimal cover = demand.signum() > 0 && inStock.signum() > 0
                ? inStock.divide(demand, 1, RoundingMode.HALF_UP)
                : null;

        return new Stock(r2(bought), r2(sold), r2(wasted), r2(inStock), r2(invested),
                avgCost, r2(stockValue), r2(demand), cover);
    }

    /** Day-by-day movement with closing stock, newest first. */
    public List<DayRow> dailyRows(int limit) {
        TreeMap<LocalDate, BigDecimal[]> days = new TreeMap<>();
        for (Object[] r : purchases.dailyTotals()) slot(days, (LocalDate) r[0])[0] = bd(r[1]);
        for (Object[] r : sales.dailyTotals()) slot(days, (LocalDate) r[0])[1] = bd(r[1]);
        for (Object[] r : wastage.dailyTotals()) slot(days, (LocalDate) r[0])[2] = bd(r[1]);

        List<DayRow> rows = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (var e : days.entrySet()) {
            BigDecimal[] v = e.getValue();
            running = running.add(v[0]).subtract(v[1]).subtract(v[2]);
            rows.add(new DayRow(e.getKey(), r2(v[0]), r2(v[1]), r2(v[2]), r2(running)));
        }
        Collections.reverse(rows);
        return rows.size() > limit ? rows.subList(0, limit) : rows;
    }

    private static BigDecimal[] slot(TreeMap<LocalDate, BigDecimal[]> map, LocalDate date) {
        return map.computeIfAbsent(date,
                d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
    }
}
