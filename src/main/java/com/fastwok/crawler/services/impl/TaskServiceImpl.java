package com.fastwok.crawler.services.impl;

import com.fastwok.crawler.entities.AccDoc;
import com.fastwok.crawler.entities.Customer;
import com.fastwok.crawler.entities.Item;
import com.fastwok.crawler.entities.UpdateStatus;
import com.fastwok.crawler.repository.AccDocRepository;
import com.fastwok.crawler.repository.AccDocSaleRepository;
import com.fastwok.crawler.repository.CustomerRepository;
import com.fastwok.crawler.repository.ItemRepository;
import com.fastwok.crawler.services.isservice.TaskService;
import com.fastwok.crawler.util.AccdocUtil;
import com.fastwok.crawler.util.BodyRequest;
import com.fastwok.crawler.util.CustomerUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Autowired
    AccDocRepository accDocRepository;
    @Autowired
    AccDocSaleRepository accDocSaleRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    ItemRepository itemRepository;
    String AUTHEN = "";
    private final String CLIENT_SECRET = "9BE94DC179BB890F4AB1DC7EFF16F819B10C11C5";
    private final String CLIENT_ID = "2c181bb5-10a9-4063-8a94-9e89f20564f0";
    private final String URL_TOKEN = "https://id.kiotviet.vn/connect/token";
    private String URL_API = "https://public.kiotapi.com/";
    private final String CUSTOMER = "customers";
    private final String SKU = "products";
    private final String ACCDOC = "invoices";
    private String CheckHour = "";


    @Override
    public void getData() throws UnirestException {


        Calendar date = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        DateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH");
        String today = dateFormat.format(date.getTime());
        String hour = hourFormat.format(date.getTime());
        date.add(Calendar.MINUTE,-5);
        String today1 = dateFormat.format(date.getTime());


        if (!hour.equals(CheckHour)) {
            String body = BodyRequest.GetbodyAuth(CLIENT_ID, CLIENT_SECRET);
            HttpResponse<JsonNode> authed = OAuth2(body);
            JSONObject res = new JSONObject(authed.getBody());
            JSONObject jsonObject = res.getJSONObject("object");
            if (!jsonObject.has("access_token")) return;
            AUTHEN = "Bearer " + jsonObject.getString("access_token");
            CheckHour = hour;
        }
        crawlCustomer(today1);
        crawlAccDoc(today1,today);
        crawlItem();
    }

    public void crawlItem() {
        List<Item> items = itemRepository.getData();
        if (!(items == null || items.isEmpty())) {
            items.forEach((element) -> {
                try {
                    HttpResponse<JsonNode> authen = Api(URL_API + SKU + "/code/" + element.getCode().trim());
                    JSONObject res = new JSONObject(authen.getBody());
                    JSONObject jsonObject = res.getJSONObject("object");
                    element.setKiot_Id(jsonObject.getLong("id")+"");
                    itemRepository.save(element);
                } catch (UnirestException e) {
                    if (URL_API.equals("https://public.kiotapi.com/"))
                        URL_API = "https://public.kiotapi2.com/";
                    else URL_API = "https://public.kiotapi.com/";
                    log.info(e.toString());
                }
            });
            accDocRepository.runInventory();
        }
    }

    public void crawlCustomer(String today1) throws UnirestException {
        String paramCustomer = "?lastModifiedFrom=" + today1 + "&orderBy=createdDate&orderDirection=desc&pageSize=100";
        HttpResponse<JsonNode> authen = Api(URL_API + CUSTOMER + paramCustomer);
        JSONObject res = new JSONObject(authen.getBody());
        JSONObject jsonObject = res.getJSONObject("object");
        if (!jsonObject.has("data")) return;
        List<Customer> customers = CustomerUtil.convert(jsonObject.getJSONArray("data"));
        if (customers.isEmpty()) return;
        customers.forEach(customer -> {
            List<Customer> checkKiotId = customerRepository.findCustomerByKiotId(customer.getKiot_Id());
            if (checkKiotId.size() > 0) return;
            Customer checkCode = customerRepository.findCustomerByCode(customer.getCode());
            if (checkCode != null) {
                if (checkCode.getPersonTel().equals(customer.getPersonTel())) {
                    checkCode.setKiot_Id(customer.getKiot_Id());
                    customer = checkCode;
                }
                if (checkCode.getTel().equals(customer.getTel())) {
                    checkCode.setKiot_Id(customer.getKiot_Id());
                    customer = checkCode;
                }
            }
            customerRepository.save(customer);
        });
    }

    public void crawlAccDoc(String today1, String today) throws UnirestException {
        String param = "?format=json&fromPurchaseDate=" + today1 + "T00:00:00&toPurchaseDate=" + today + "&orderBy=id&orderDirection=desc&pageSize=100";
        HttpResponse<JsonNode> authen = Api(URL_API + ACCDOC + param);
        JSONObject res = new JSONObject(authen.getBody());
        JSONObject jsonObject = res.getJSONObject("object");
        if (!jsonObject.has("data")) return;
        log.info(jsonObject.toString());
        List<AccDoc> accdocs = AccdocUtil.convert(jsonObject.getJSONArray("data"));
        if (accdocs.isEmpty()) return;
        AtomicInteger i= new AtomicInteger();
        accdocs.forEach(accdoc -> {
            Optional<AccDoc> checkId = accDocRepository.findById(accdoc.getId());
            if (checkId.isPresent()) return;
            i.addAndGet(1);
            accDocRepository.save(accdoc);
            accDocSaleRepository.saveAll(accdoc.getAccDocSales());
            accDocRepository.runExec(accdoc.getId());
            UpdateStatus updateStatus = accDocRepository.updateStatus(accdoc.getId());
            String updateStatusBody = BodyRequest.UpdateAccdoc(updateStatus.getDescription(), updateStatus.getStatus());
            try {
                Put(URL_API + ACCDOC + "/" + updateStatus.getId(), updateStatusBody);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        });
        if (i.get()>0)
            accDocRepository.runInventory();
    }

    private HttpResponse<JsonNode> OAuth2(String body)
            throws UnirestException {
        Date date = new Date();
        long timeMilli = date.getTime();
        return Unirest.post(URL_TOKEN)
                .header("Accept", "*/*")
                .header("x-fw", String.valueOf(timeMilli))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .body(body)
                .asJson();
    }

    private HttpResponse<JsonNode> Api(String url)
            throws UnirestException {
        Date date = new Date();
        long timeMilli = date.getTime();
        return Unirest.get(url)
                .header("Accept", "*/*")
                .header("Authorization", AUTHEN)
                .header("Retailer", "earldom")
                .header("x-fw", String.valueOf(timeMilli))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/json")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .asJson();
    }

    private void Put(String url, String body)
            throws UnirestException {
        Date date = new Date();
        long timeMilli = date.getTime();
        Unirest.put(url)
                .header("Accept", "*/*")
                .header("Authorization", AUTHEN)
                .header("Retailer", "earldom")
                .header("x-fw", String.valueOf(timeMilli))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Content-Type", "application/json")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .body(body)
                .asJson();
    }

}
