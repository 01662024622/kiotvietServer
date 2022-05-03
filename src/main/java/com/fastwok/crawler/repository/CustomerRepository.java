package com.fastwok.crawler.repository;

import com.fastwok.crawler.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    @Query("SELECT c FROM Customer c where c.Kiot_Id=?1")
    Customer findCustomerByKiotId(String kiotId);

    @Query("SELECT c FROM Customer c where c.Code=?1")
    Customer findCustomerByCode(String code);
}
