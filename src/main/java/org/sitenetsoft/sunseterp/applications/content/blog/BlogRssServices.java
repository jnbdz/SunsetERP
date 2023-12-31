/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.sitenetsoft.sunseterp.applications.content.blog;

import com.rometools.rome.feed.synd.*;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWorker;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.io.IOException;
import java.util.*;

/**
 * BlogRssServices
 */
public class BlogRssServices {

    private static final String MODULE = BlogRssServices.class.getName();
    private static final String RESOURCE = "ContentUiLabels";
    public static final String MIME_TYPE_ID = "text/html";
    public static final String MAP_KEY = "SUMMARY";

    public static Map<String, Object> generateBlogRssFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("blogContentId");
        String entryLink = (String) context.get("entryLink");
        String feedType = (String) context.get("feedType");
        Locale locale = (Locale) context.get("locale");

        // create the main link
        String mainLink = (String) context.get("mainLink");
        mainLink = mainLink + "?blogContentId=" + contentId;

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        // get the main blog content
        GenericValue content = null;
        try {
            content = EntityQuery.use(delegator).from("Content").where("contentId", contentId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (content == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                    "ContentCannotGenerateBlogRssFeed",
                    UtilMisc.toMap("contentId", contentId), locale));
        }

        // create the feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setLink(mainLink);

        feed.setTitle(content.getString("contentName"));
        feed.setDescription(content.getString("description"));
        feed.setEntries(generateEntryList(dispatcher, delegator, contentId, entryLink, locale, userLogin));

        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put("wireFeed", feed.createWireFeed());
        return resp;
    }

    public static List<SyndEntry> generateEntryList(LocalDispatcher dispatcher, Delegator delegator, String contentId,
                                                    String entryLink, Locale locale, GenericValue userLogin) {
        List<SyndEntry> entries = new LinkedList<>();

        List<GenericValue> contentRecs = null;
        try {
            contentRecs = EntityQuery.use(delegator).from("ContentAssocViewTo")
                    .where("contentIdStart", contentId,
                           "caContentAssocTypeId", "PUBLISH_LINK",
                           "statusId", "CTNT_PUBLISHED")
                    .orderBy("-caFromDate").queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

        if (contentRecs != null) {
            for (GenericValue v : contentRecs) {
                String sub = null;
                try {
                    Map<String, Object> dummy = new HashMap<>();
                    sub = ContentWorker.renderSubContentAsText(dispatcher, v.getString("contentId"), MAP_KEY, dummy, locale, MIME_TYPE_ID, true);
                } catch (GeneralException | IOException e) {
                    Debug.logError(e, MODULE);
                }
                if (sub != null) {
                    String thisLink = entryLink + "?articleContentId=" + v.getString("contentId") + "&blogContentId=" + contentId;
                    SyndContent desc = new SyndContentImpl();
                    desc.setType("text/plain");
                    desc.setValue(sub);

                    SyndEntry entry = new SyndEntryImpl();
                    entry.setTitle(v.getString("contentName"));
                    entry.setPublishedDate(v.getTimestamp("createdDate"));
                    entry.setDescription(desc);
                    entry.setLink(thisLink);
                    entry.setAuthor((v.getString("createdByUserLogin")));
                    entries.add(entry);
                }
            }
        }

        return entries;
    }
}
