package com.fastwok.crawler.util;

import com.fastwok.crawler.entities.AccDoc;
import com.fastwok.crawler.entities.AccDocSale;
import com.fastwok.crawler.entities.Customer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccdocUtil {
    private static Map<Long, Long> inventoryMap;

    static {
        inventoryMap = new HashMap<>();
        inventoryMap.put(63506L, 219321L);
        inventoryMap.put(164049L, 219201L);
        inventoryMap.put(19576L, 218761L);
        inventoryMap.put(19578L, 218841L);
        inventoryMap.put(3634L, 219081L);
        inventoryMap.put(58187L, 219001L);
    }

    public static List<AccDoc> convert(JSONArray jsonArray) {
        List<AccDoc> accDocs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Long branchId = null;
            JSONObject accdocJson = jsonArray.getJSONObject(i);
            if (accdocJson.has("statusValue")) {
                if (accdocJson.getString("statusValue").equals("Đã hủy")) continue;
            }
            if (accdocJson.has("branchId")) {
                branchId = accdocJson.getLong("branchId");
                if (!inventoryMap.containsKey(branchId)) continue;
                branchId = inventoryMap.get(branchId);
            }
            AccDoc accDoc = new AccDoc();
            if (accdocJson.has("id")) {
                accDoc.setId(accdocJson.getLong("id"));
            }
            if (accdocJson.has("soldById")) {
                accDoc.setSale_id(String.valueOf(accdocJson.getLong("soldById")));
            }
            if (accdocJson.has("code")) {
                accDoc.setDocNo(accdocJson.getString("code"));
            }
            if (accdocJson.has("customerId")) {
                accDoc.setKiot_Id(String.valueOf(accdocJson.getLong("customerId")));
            }
            if (accdocJson.has("customerCode")) {
                accDoc.setCustomerCode(accdocJson.getString("customerCode"));
            }
            if (accdocJson.has("discount")) {
                accDoc.setDiscountRate(accdocJson.getLong("discount"));
            }
            if (accdocJson.has("discount")) {
                accDoc.setDiscountRate(accdocJson.getLong("discount"));
            }

            if (accdocJson.has("total")) {
                accDoc.setTotal(accdocJson.getLong("total") + accDoc.getDiscountRate());
                accDoc.setPayment(accdocJson.getLong("total"));
            }
            if (accdocJson.has("description")) {
                accDoc.setDescription(accdocJson.getString("description"));
            }
            if (accdocJson.has("invoiceDetails")) {
                accDoc.setAccDocSales(convertSub(accdocJson.getJSONArray("invoiceDetails"), accDoc.getId(), branchId));
            }

            accDocs.add(accDoc);
        }
        return accDocs;
    }

    private static List<AccDocSale> convertSub(JSONArray jsonArray, Long id, Long brandId) {
        List<AccDocSale> accDocSales = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject accdocJson = jsonArray.getJSONObject(i);
            AccDocSale accDocSale = new AccDocSale();
            if (accdocJson.has("productId")) {
                accDocSale.setKiot_Id(String.valueOf(accdocJson.getLong("productId")));
            }
            if (accdocJson.has("quantity")) {
                accDocSale.setQuantity(accdocJson.getLong("quantity"));
            }
            if (accdocJson.has("price")) {
                accDocSale.setPrice(accdocJson.getLong("price"));
            }


            if (accdocJson.has("subTotal")) {
                accDocSale.setTotal(accdocJson.getLong("subTotal"));
            }
            if (accdocJson.has("discount")) {
                accDocSale.setDiscount(accdocJson.getLong("discount") * accDocSale.getQuantity());
            }
            if (accdocJson.has("note")) {
                accDocSale.setDescription(accdocJson.getString("note"));
            }
            accDocSale.setBillId(id);
            accDocSale.setInventoryId(brandId);
            accDocSales.add(accDocSale);
        }
        return accDocSales;
    }
}