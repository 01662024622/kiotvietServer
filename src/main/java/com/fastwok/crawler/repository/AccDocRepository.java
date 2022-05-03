package com.fastwok.crawler.repository;

import com.fastwok.crawler.entities.AccDoc;
import com.fastwok.crawler.entities.UpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface AccDocRepository extends JpaRepository<AccDoc, Long> {
    @Query(value = "EXEC uspAbahaConvertHDBravo @Id  = ?1", nativeQuery = true)
    Object runExec(Long id);

    @Modifying
    @Transactional
    @Query(value = "EXEC usp_Connect_ton_Api_auto", nativeQuery = true)
    void runInventory();

    @Query(value = "SELECT new com.fastwok.crawler.entities.UpdateStatus(a.idk,a.Description,b.DocStatusName ) FROM AccDocHdr a " +
            "LEFT JOIN DocStatus b ON a.DocStatus = b.DocStatusKey WHERE a.idk = ?1")
    UpdateStatus updateStatus(Long id);

}
