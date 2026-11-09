package com.milk.repo;

import com.milk.model.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, Long> {

    List<CustomerPayment> findByCustomerIdOrderByPaymentDateAscIdAsc(Long customerId);

    List<CustomerPayment> findTop15ByOrderByPaymentDateDescIdDesc();

    boolean existsByCustomerId(Long customerId);

    @Query("select sum(p.amount) from CustomerPayment p")
    BigDecimal sumAmount();

    @Query("select p.customer.id, sum(p.amount) from CustomerPayment p group by p.customer.id")
    List<Object[]> amountByCustomer();
}
