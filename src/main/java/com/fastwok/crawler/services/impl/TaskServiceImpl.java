package com.fastwok.crawler.services.impl;

import com.fastwok.crawler.entities.AccDoc;
import com.fastwok.crawler.entities.Customer;
import com.fastwok.crawler.entities.UpdateStatus;
import com.fastwok.crawler.repository.AccDocRepository;
import com.fastwok.crawler.repository.AccDocSaleRepository;
import com.fastwok.crawler.repository.CustomerRepository;
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

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {
    @Autowired
    AccDocRepository accDocRepository;
    @Autowired
    AccDocSaleRepository accDocSaleRepository;
    @Autowired
    CustomerRepository customerRepository;
    String AUTHEN = "";
    private String CLIENT_SECRET = "9BE94DC179BB890F4AB1DC7EFF16F819B10C11C5";
    private String CLIENT_ID = "2c181bb5-10a9-4063-8a94-9e89f20564f0";
    private String URL_TOKEN = "https://id.kiotviet.vn/connect/token";
    private String URL_API = "https://public.kiotapi.com/";
    private String CUSTOMER = "customers";
    private String SKU = "products";
    private String ACCDOC = "invoices";
    private String CheckHour = "";
    private String TOKEN = "1233453567";


    @Override
    public void getData() throws UnirestException {

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String today = dateFormat.format(date);

        DateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH");
        String hour = hourFormat.format(date);

        if (!hour.equals(CheckHour)) {
            String body = BodyRequest.GetbodyAuth(CLIENT_ID, CLIENT_SECRET);
            HttpResponse<JsonNode> authen = OAuth2(body);
            JSONObject res = new JSONObject(authen.getBody());
            JSONObject jsonObject = res.getJSONObject("object");
            if (!jsonObject.has("access_token")) return;
            AUTHEN = "Bearer " + jsonObject.getString("access_token");
            CheckHour = hour;
        }
        crawlCustomer(today);
        crawlAccdoc(today);
    }

    public void crawlCustomer(String today) throws UnirestException {
        String paramCustomer = "?lastModifiedFrom=" + today + "&orderBy=modifiedDate&orderDirection=desc";
//        String paramCustomer = "?lastModifiedFrom=2022-07-12&orderBy=modifiedDate&orderDirection=desc";
        HttpResponse<JsonNode> authen = Api(URL_API + CUSTOMER + paramCustomer);
        JSONObject res = new JSONObject(authen.getBody());
        JSONObject jsonObject = res.getJSONObject("object");
        if (!jsonObject.has("data")) return;
        List<Customer> customers = CustomerUtil.convert(jsonObject.getJSONArray("data"));
        if (customers.isEmpty()) return;
        customers.forEach(customer -> {
            List<Customer> checkKiotId = customerRepository.findCustomerByKiotId(customer.getKiot_Id());
            if (checkKiotId != null) return;
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

    public void crawlAccdoc(String today) throws UnirestException {
//        String param = "?format=json&fromPurchaseDate=2022-07-12T00:00:00&toPurchaseDate=2022-07-12T23:59:00&orderBy=id&orderDirection=desc&pageSize=100";
        String param = "?format=json&fromPurchaseDate=" + today + "T00:00:00&toPurchaseDate=" + today + "T23:59:00&orderBy=id&orderDirection=desc&pageSize=100";
        HttpResponse<JsonNode> authen = Api(URL_API + ACCDOC + param);
        JSONObject res = new JSONObject(authen.getBody());
        JSONObject jsonObject = res.getJSONObject("object");
        if (!jsonObject.has("data")) return;
        log.info(jsonObject.toString());
        List<AccDoc> accdocs = AccdocUtil.convert(jsonObject.getJSONArray("data"));
        if (accdocs.isEmpty()) return;
        accdocs.forEach(accdoc -> {
            Optional<AccDoc> checkId = accDocRepository.findById(accdoc.getId());
            if (checkId.isPresent()) return;
            accDocRepository.save(accdoc);
            accDocSaleRepository.saveAll(accdoc.getAccDocSales());
            accDocRepository.runExec(accdoc.getId());
            accDocRepository.runInventory();
            UpdateStatus updateStatus = accDocRepository.updateStatus(accdoc.getId());
            String updateStatusBody = BodyRequest.UpdateAccdoc(updateStatus.getDescription(), updateStatus.getStatus());
            try {
                PUT(URL_API + ACCDOC + "/" + updateStatus.getId(), updateStatusBody);
            } catch (UnirestException e) {
                e.printStackTrace();
            }
        });
    }
//    public void getData(String projectId) throws UnirestException {
//        HttpResponse<JsonNode> response = getListTask(string, projectId);
//        JSONObject res = new JSONObject(response.getBody());
//        JSONArray jsonArray = res.getJSONObject("object").getJSONArray("result");
//        int total = jsonArray.length();
//        for (int n = 0; n < total; n++) {
//            JSONArray tasks = jsonArray.getJSONObject(n).getJSONArray("tasks");
//            for (int y = 0; y < tasks.length(); y++) {
//                List<TaskUser> taskUsers = new ArrayList<>();
//                List<SubTask> subTasks = new ArrayList<>();
//                String id = tasks.getJSONObject(y).get("_id").toString();
//                Task getTask = taskRepository.getById(id);
//                JSONObject jsonValues = new JSONObject(getDataDetail(id).getBody()).getJSONObject("result");
//                if (getTask == null) {
//                    getTask = new Task();
//                } else if (jsonValues.getLong("modifiedDate") < getTask.getModifiedDate()) return;
//
//                getTask = TaskUtil.convertToTask(jsonValues, getTask);
//                taskUsers = TaskUtil.getListUser(jsonValues);
//                subTasks = TaskUtil.getListSubTask(jsonValues);
//                getTask.setProject_id(projectId);
//                taskRepository.save(getTask);
//                if (taskUsers != null) {
//                    taskUsers.forEach(element -> {
//                        TaskUser taskUser = taskUserRepository.checkExist(element.getTaskId(), element.getUser());
//                        if (taskUser == null) {
//                            taskUserRepository.save(element);
//                        }
//                    });
//                }
//                if (subTasks != null) {
//                    subTasks.forEach(element -> {
//                        subTaskRepository.save(element);
//                    });
//                }
//            }
//
//        }
//    }

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

    private HttpResponse<JsonNode> PUT(String url, String body)
            throws UnirestException {
        Date date = new Date();
        long timeMilli = date.getTime();
        return Unirest.put(url)
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
