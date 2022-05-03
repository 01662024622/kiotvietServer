package com.fastwok.crawler.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "AbahaAccdoc")
public class AccDoc {
    @Id
    private Long Id;
    private String Kiot_Id="9999999999";
    private String CustomerCode;
    @Column(length = 2000)
    private String Description="";
    @Transient
    private String StatusValue;
    private Long DiscountRate=0L;
    private Long Payment;
    private Long Total;
    @Transient
    private List<AccDocSale> accDocSales;
}
