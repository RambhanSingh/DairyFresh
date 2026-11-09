package com.milk.repo;

import com.milk.model.Wastage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface WastageRepository extends JpaRepository<Wastage, Long> {

    List<Wastage> findTop15ByOrderByWasteDateDescIdDesc();

    @Query("select sum(w.quantity) from Wastage w")
    BigDecimal sumQuantity();

    @Query("select w.wasteDate, sum(w.quantity) from Wastage w group by w.wasteDate")
    List<Object[]> dailyTotals();
}
