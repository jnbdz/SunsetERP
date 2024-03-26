<#--
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
-->
<#assign searchOptionsHistoryList = Static["org.sitenetsoft.sunseterp.applications.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)>
<#assign currentCatalogId = Static["org.sitenetsoft.sunseterp.applications.product.catalog.CatalogWorker"].getCurrentCatalogId(request)>
<h1>${uiLabelMap.ProductAdvancedSearchInCategory}</h1>
<br />
<form class="basic-form" name="advtokeywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="VIEW_SIZE" value="10" />
  <input type="hidden" name="SEARCH_CATALOG_ID" value="${currentCatalogId}" />
  <table border="0" wdith="100%">
    <#if searchCategory?has_content>
        <tr>
          <td class="label">
            <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId!}" />
            <label>${uiLabelMap.ProductCategory}:</label>
          </td>
          <td>
              <b>"${(searchCategory.description)!}"</b>${uiLabelMap.ProductIncludeSubCategories}
              <label>${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="Y" checked="checked" /></label>
              <label>${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_CATEGORIES" value="N" /></label>
          </td>
        </tr>
    </#if>
    <tr>
      <td class="label">
        <label>${uiLabelMap.ProductKeywords}:</label>
      </td>
      <td>
          <input type="text" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING!}" />&nbsp;
          <label>${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if "OR" == searchOperator>checked="checked"</#if> /></label>
          <label>${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if "AND" == searchOperator>checked="checked"</#if> /></label>
      </td>
    </tr>
    <#list productFeatureTypeIdsOrdered as productFeatureTypeId>
      <#assign findPftMap = Static["org.sitenetsoft.sunseterp.framework.base.util.UtilMisc"].toMap("productFeatureTypeId", productFeatureTypeId)>
      <#assign productFeatureType = delegator.findOne("ProductFeatureType", findPftMap, true)>
      <#assign productFeatures = productFeaturesByTypeMap[productFeatureTypeId]>
      <tr>
        <td class="label">
          <label>${(productFeatureType.get("description",locale))!}:</label>
        </td>
        <td>
            <select name="pft_${productFeatureTypeId}">
              <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
              <#list productFeatures as productFeature>
              <option value="${productFeature.productFeatureId}">${productFeature.get("description",locale)?default(productFeature.productFeatureId)}</option>
              </#list>
            </select>
        </td>
      </tr>
    </#list>
    <tr>
      <td class="label">
        <label>${uiLabelMap.ProductSupplier}:</label>
      </td>
      <td>
          <select name="SEARCH_SUPPLIER_ID">
            <option value="">- ${uiLabelMap.CommonSelectAny} -</option>
            <#list supplerPartyRoleAndPartyDetails as supplerPartyRoleAndPartyDetail>
              <option value="${supplerPartyRoleAndPartyDetail.partyId}"<#if (sessionAttributes.orderPartyId?? & sessionAttributes.orderPartyId = supplerPartyRoleAndPartyDetail.partyId)> selected="selected"</#if>>${supplerPartyRoleAndPartyDetail.groupName!} ${supplerPartyRoleAndPartyDetail.firstName!} ${supplerPartyRoleAndPartyDetail.lastName!} [${supplerPartyRoleAndPartyDetail.partyId}]</option>
            </#list>
          </select>
      </td>
    </tr>
    <tr>
      <td class="label">
        <label>${uiLabelMap.CommonSortedBy}:</label>
      </td>
      <td>
          <select name="sortOrder">
            <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevancy}</option>
            <option value="SortProductField:productName">${uiLabelMap.ProductProductName}</option>
            <option value="SortProductField:internalName">${uiLabelMap.ProductInternalName}</option>
            <option value="SortProductField:totalQuantityOrdered">${uiLabelMap.ProductPopularityByOrders}</option>
            <option value="SortProductField:totalTimesViewed">${uiLabelMap.ProductPopularityByViews}</option>
            <option value="SortProductField:averageCustomerRating">${uiLabelMap.ProductCustomerRating}</option>
            <option value="SortProductPrice:LIST_PRICE">${uiLabelMap.ProductListPrice}</option>
            <option value="SortProductPrice:DEFAULT_PRICE">${uiLabelMap.ProductDefaultPrice}</option>
            <option value="SortProductPrice:AVERAGE_COST">${uiLabelMap.ProductAverageCost}</option>
          </select>
          <label>${uiLabelMap.ProductLowToHigh}<input type="radio" name="sortAscending" value="Y" checked="checked" /></label>
          <label>${uiLabelMap.ProductHighToLow}<input type="radio" name="sortAscending" value="N" /></label>
      </td>
    </tr>
    <#if searchConstraintStrings?has_content>
      <tr>
        <td class="label">
          <label>${uiLabelMap.ProductLastSearch}:</label>
        </td>
        <td>
            <#list searchConstraintStrings as searchConstraintString>
                <div>&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
              <label>${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</label>
              <label>${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked="checked" /></label>
              <label>${uiLabelMap.CommonRefineSearch}<input type="radio" name="clearSearch" value="N" /></label>
        </td>
      </tr>
    </#if>
    <tr>
      <td class="label"></td>
      <td>
        <input type="submit" value="${uiLabelMap.CommonFind}"/>
      </td>
    </tr>
  </table>

  <#if searchOptionsHistoryList?has_content>
    <hr />

    <h2>${uiLabelMap.OrderLastSearches}...</h2>

    <div>
      <a href="<@ofbizUrl>clearSearchOptionsHistoryList</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderClearSearchHistory}</a>
      ${uiLabelMap.OrderClearSearchHistoryNote}
    </div>
    <#list searchOptionsHistoryList as searchOptions>
    <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
        <div>
          <b>${uiLabelMap.CommonSearch} #${searchOptions_index + 1}</b>
          <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&amp;clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSearch}</a>
          <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine}</a>
        </div>
        <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
        <#list constraintStrings as constraintString>
          <div>&nbsp;-&nbsp;${constraintString}</div>
        </#list>
        <#if searchOptions_has_next>
          <hr />
        </#if>
    </#list>
  </#if>
</form>
