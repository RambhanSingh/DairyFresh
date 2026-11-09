package com.milk.repo;

import com.milk.model.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplierIdOrderByPaymentDateAscIdAsc(Long supplierId);

    List<SupplierPayment> findTop15ByOrderByPaymentDateDescIdDesc();

    boolean existsBySupplierId(Long supplierId);

    @Query("select sum(p.amount) from SupplierPayment p")
    BigDecimal sumAmount();

    @Query("select p.supplier.id, sum(p.amount) from SupplierPayment p group by p.supplier.id")
    List<Object[]> amountBySupplier();
}
