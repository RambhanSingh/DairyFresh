package com.milk.repo;

import com.milk.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByCustomerIdOrderBySaleDateAscIdAsc(Long customerId);

    List<Sale> findBySaleDateOrderByCustomerNameAscIdAsc(LocalDate saleDate);

    boolean existsByCustomerIdAndSaleDate(Long customerId, LocalDate saleDate);

    boolean existsByCustomerId(Long customerId);

    @Query("select sum(s.quantity) from Sale s")
    BigDecimal sumQuantity();

    @Query("select sum(s.amount) from Sale s")
    BigDecimal sumAmount();

    @Query("select s.customer.id, sum(s.amount) from Sale s group by s.customer.id")
    List<Object[]> amountByCustomer();

    @Query("select s.saleDate, sum(s.quantity), sum(s.amount) from Sale s group by s.saleDate")
    List<Object[]> dailyTotals();
}
