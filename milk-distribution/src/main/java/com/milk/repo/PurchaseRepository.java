package com.milk.repo;

import com.milk.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findBySupplierIdOrderByPurchaseDateAscIdAsc(Long supplierId);

    List<Purchase> findTop15ByOrderByPurchaseDateDescIdDesc();

    boolean existsBySupplierId(Long supplierId);

    @Query("select sum(p.quantity) from Purchase p")
    BigDecimal sumQuantity();

    @Query("select sum(p.amount) from Purchase p")
    BigDecimal sumAmount();

    @Query("select sum(p.quantity) from Purchase p where p.purchaseDate = :d")
    BigDecimal sumQuantityOn(@Param("d") LocalDate d);

    @Query("select p.supplier.id, sum(p.amount) from Purchase p group by p.supplier.id")
    List<Object[]> amountBySupplier();

    @Query("select p.purchaseDate, sum(p.quantity), sum(p.amount) from Purchase p group by p.purchaseDate")
    List<Object[]> dailyTotals();
}
