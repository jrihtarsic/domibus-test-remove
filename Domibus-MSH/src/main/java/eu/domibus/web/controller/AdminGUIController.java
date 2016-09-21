/*
 * Copyright 2015 e-CODEX Project
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 * http://ec.europa.eu/idabc/eupl5
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.domibus.web.controller;

import eu.domibus.common.ErrorCode;
import eu.domibus.common.MSHRole;
import eu.domibus.common.MessageStatus;
import eu.domibus.common.NotificationStatus;
import eu.domibus.common.dao.ErrorLogDao;
import eu.domibus.common.dao.MessageLogDao;
import eu.domibus.common.model.logging.MessageLogEntry;
import eu.domibus.common.util.DomibusPropertiesService;
import eu.domibus.ebms3.common.dao.PModeProvider;
import eu.domibus.ebms3.common.model.MessageType;
import eu.domibus.plugin.NotificationListener;
import eu.domibus.plugin.routing.*;
import eu.domibus.wss4j.common.crypto.TrustStoreService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author Christian Walczac
 */

@Controller
public class AdminGUIController {

    private final static Log LOG = LogFactory.getLog(AdminGUIController.class);

    @Autowired
    private MessageLogDao mld;
    @Autowired
    private ErrorLogDao eld;

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private DomibusPropertiesService domibusPropertiesService;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private List<NotificationListener> notificationListenerServices;

    @Autowired
    private TrustStoreService trustStoreService;

    @Resource(name = "routingCriteriaFactories")
    private List<CriteriaFactory> routingCriteriaFactories;


    @RequestMapping(value = {"/home"}, method = GET)
    public ModelAndView welcomePage() {

        final ModelAndView model = new ModelAndView();
        model.addObject("title", "Domibus - Home");
        model.addObject("displayVersion", domibusPropertiesService.getDisplayVersion());
        model.setViewName("home");
        return model;

    }

    @RequestMapping(value = {"/home/messagelog"}, method = GET)
    public ModelAndView messageLogPage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "orderby", required = false) String column,
            @RequestParam(value = "asc", defaultValue = "true") boolean asc,
            @RequestParam(value = "messageId", required = false) String messageId,
            @RequestParam(value = "messageStatus", required = false) MessageStatus messageStatus,
            @RequestParam(value = "notificationStatus", required = false) NotificationStatus notificationStatus,
            @RequestParam(value = "mshRole", required = false) MSHRole mshRole,
            @RequestParam(value = "messageType", required = false) MessageType messageType,
            @RequestParam(value = "receivedFrom", required = false) String receivedFrom,
            @RequestParam(value = "receivedTo", required = false) String receivedTo
    ) {


        HashMap<String, Object> filters = new HashMap<>();
        filters.put("messageId", messageId);
        filters.put("messageStatus", messageStatus);
        filters.put("notificationStatus", notificationStatus);
        filters.put("mshRole", mshRole);
        filters.put("messageType", messageType);
        filters.put("receivedFrom", receivedFrom);
        filters.put("receivedTo", receivedTo);

        List<MessageLogEntry> messageLogEntries = mld.findPaged(size * (page - 1), size, column, asc, filters);
//        long entries = mld.countEntries();
        long entries = mld.countMessages(filters);
        long pages = entries / size;
        if (entries % size != 0) {
            pages++;
        }
        int begin = Math.max(1, page - 5);
        long end = Math.min(begin + 10, pages);

        ModelAndView model = new ModelAndView();
        model.addObject("messageId", messageId);
        model.addObject("messageStatus", messageStatus);
        model.addObject("notificationStatus", notificationStatus);
        model.addObject("mshRole", mshRole);
        model.addObject("messageType", messageType);
        model.addObject("receivedFrom", receivedFrom);
        model.addObject("receivedTo", receivedTo);

        model.addObject("page", page);
        model.addObject("size", size);
        model.addObject("pages", pages);
        model.addObject("column", column);
        model.addObject("asc", asc);
        model.addObject("beginIndex", begin);
        model.addObject("endIndex", end);
        if (page <= pages) {
            model.addObject("table", messageLogEntries);
        }
        model.addObject("title", "Domibus - Messages Log: ");
        model.addObject("messagestatusvalues", MessageStatus.values());
        model.addObject("notificationstatusvalues", NotificationStatus.values());
        model.addObject("mshrolevalues", MSHRole.values());
        model.addObject("messagetypevalues", MessageType.values());
        model.setViewName("messagelog");
        return model;

    }

    @RequestMapping(value = {"/home/errorlog**"}, method = GET)
    public ModelAndView errorLogPage(
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "size", defaultValue = "10") final int size,
            @RequestParam(value = "orderby", required = false) final String column,
            @RequestParam(value = "asc", defaultValue = "true") final boolean asc,
            @RequestParam(value = "errorSignalMessageId", required = false) final String errorSignalMessageId,
            @RequestParam(value = "mshRole", required = false) final MSHRole mshRole,
            @RequestParam(value = "messageInErrorId", required = false) final String messageInErrorId,
            @RequestParam(value = "errorCode", required = false) final ErrorCode errorCode,
            @RequestParam(value = "errorDetail", required = false) final String errorDetail,
            @RequestParam(value = "timestampFrom", required = false) final String timestampFrom,
            @RequestParam(value = "timestampTo", required = false) final String timestampTo,
            @RequestParam(value = "notifiedFrom", required = false) final String notifiedFrom,
            @RequestParam(value = "notifiedTo", required = false) final String notifiedTo) {

        final HashMap<String, Object> filters = new HashMap<String, Object>();
        filters.put("errorSignalMessageId", errorSignalMessageId);
        filters.put("mshRole", mshRole);
        filters.put("messageInErrorId", messageInErrorId);
        filters.put("errorCode", errorCode);
        filters.put("errorDetail", errorDetail);
        filters.put("timestampFrom", timestampFrom);
        filters.put("timestampTo", timestampTo);
        filters.put("notifiedFrom", notifiedFrom);
        filters.put("notifiedTo", notifiedTo);

        final long entries = eld.countEntries();
        long pages = entries / size;
        if (entries % size != 0) {
            pages++;
        }
        final int begin = Math.max(1, page - 5);
        final long end = Math.min(begin + 10, pages);

        final ModelAndView model = new ModelAndView();
        model.addObject("page", page);
        model.addObject("size", size);
        model.addObject("pages", pages);
        model.addObject("column", column);
        model.addObject("asc", asc);
        model.addObject("beginIndex", begin);
        model.addObject("endIndex", end);
        if (page <= pages) {
            model.addObject("table", eld.findPaged(size * (page - 1), size, column, asc, filters));
        }
        model.addObject("title", "Domibus - Error Log:");
        model.setViewName("errorlog");
        model.addObject("mshrolevalues", MSHRole.values());
        model.addObject("errorcodevalues", ErrorCode.values());
        return model;
    }

    @RequestMapping(value = {"/home/updatepmode**"}, method = GET)
    public ModelAndView updatePModePage() {

        final ModelAndView model = new ModelAndView();
        model.addObject("title", "Domibus - Update PMode");
        model.setViewName("updatepmode");
        return model;
    }

    @RequestMapping(value = {"/home/messagefilter"}, method = GET)
    public ModelAndView messageFilterPage() {

        final ModelAndView model = new ModelAndView();
        model.addObject("title", "Domibus - Message Filter: Routing Criteria");
        final List<String> routingCriteriaNames = new ArrayList<>();
        model.addObject("routingcriterias", routingCriteriaFactories);
        model.addObject("backendConnectors", routingService.getBackendFilters());
        model.setViewName("messagefilter");
        return model;

    }


    /**
     * Update filters
     */
    @RequestMapping(value = "/home/messagefilter", method = RequestMethod.POST)
    public
    @ResponseBody
    String updateFilters(@RequestParam final MultiValueMap<String, String> map) {
        final List<String> mappedBackends = map.get("backends");
        final List<BackendFilter> backendFilters = routingService.getBackendFilters();
        for (int j = 0; j < mappedBackends.size(); j++) {
            final String backendName = mappedBackends.get(j);

            for (final BackendFilter backendFilter : backendFilters) {
                if (backendFilter.getBackendName().equals(backendName)) {
                    backendFilter.setIndex(j);

                    final List<String> mappedRoutingCrierias = map.get(backendName.replaceAll(" ", "") + "filter");
                    final List<String> mappedExpression = map.get(backendName.replaceAll(" ", "") + "selection");

                    backendFilter.getRoutingCriterias().clear();
                    if (mappedRoutingCrierias != null) {
                        for (int i = 0; i < mappedRoutingCrierias.size(); i++) {
                            for (final CriteriaFactory criteriaFactory : routingCriteriaFactories) {
                                if (mappedRoutingCrierias.get(i).equals(criteriaFactory.getName())) {
                                    final IRoutingCriteria criteria = criteriaFactory.getInstance();
                                    criteria.setExpression(mappedExpression.get(i));
                                    backendFilter.getRoutingCriterias().add((RoutingCriteria) criteria);
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(backendFilters);
        routingService.updateBackendFilters(backendFilters);
        return "Filters updated.";
    }

    /**
     * Upload single file using Spring Controller
     */
    @RequestMapping(value = "/home/uploadPmodeFile", method = RequestMethod.POST)
    public
    @ResponseBody
    String uploadPmodeFile(@RequestParam("pmode") final MultipartFile pmode) {

        if (!pmode.isEmpty()) {
            try {
                final byte[] bytes = pmode.getBytes();
                pModeProvider.updatePModes(bytes);
                return "PMode file has been successfully uploaded.";
            } catch (final Exception e) {
                return "Failed to upload the PMode file due to => " + e.getMessage();
            }
        } else {
            return "Failed to upload the PMode file since it was empty.";
        }
    }

    @RequestMapping(value = "/home/uploadTruststoreFile", method = RequestMethod.POST)
    public
    @ResponseBody
    String uploadTruststoreFile(@RequestParam("truststore") final MultipartFile truststore, @RequestParam("password") final String password) {

        if (!truststore.isEmpty()) {
            try {
                final byte[] bytes = truststore.getBytes();
                trustStoreService.replaceTruststore(bytes, password);
                return "Truststore file has been successfully replaced.";
            } catch (final Exception e) {
                LOG.error("Failed to upload the truststore file", e);
                return "Failed to upload the truststore file due to => " + e.getMessage();
            }
        } else {
            return "Failed to upload the truststore file since it was empty.";
        }
    }
}
