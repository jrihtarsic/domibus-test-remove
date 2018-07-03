package eu.domibus.web.rest;

import eu.domibus.core.alerts.AlertRo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AlertResult {

    private int pageSize;
    private int count;
    private List<AlertRo> alertsEntries=new ArrayList<>();

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<AlertRo> getAlertsEntries() {
        return alertsEntries;
    }

    public void addAlertEntry(AlertRo alertRo){
        alertsEntries.add(alertRo);
    }


    public void setAlertsEntries(List<AlertRo> alertsEntries) {
        this.alertsEntries = alertsEntries;
    }
}
