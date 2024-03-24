/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

package org.sitenetsoft.sunseterp.applications.marketing.vcard;

import ezvcard.Ezvcard;
import ezvcard.io.text.VCardReader;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.*;
import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtilProperties;
import org.sitenetsoft.sunseterp.applications.party.party.PartyHelper;
import org.sitenetsoft.sunseterp.applications.party.party.PartyWorker;
import org.sitenetsoft.sunseterp.framework.service.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class VCard {
    private static final String MODULE = VCard.class.getName();
    private static final String RES_ERROR = "MarketingUiLabels";

    /**
     * import a vcard from byteBuffer. the reader use is ez-vcard, see official site https://github.com/mangstadt/ez-vcard/
     * @param dctx
     * @param context
     * @return
     * @throws IOException
     */
    public static Map<String, Object> importVCard(DispatchContext dctx, Map<String, ? extends Object> context) throws IOException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        ByteBuffer byteBuffer = (ByteBuffer) context.get("infile");
        byte[] inputByteArray = byteBuffer.array();
        InputStream in = new ByteArrayInputStream(inputByteArray);
        Map<String, Object> serviceCtx = new HashMap<>();
        boolean isGroup = false;
        List<Map<String, String>> partiesCreated = new ArrayList<>();
        List<Map<String, String>> partiesExist = new ArrayList<>();

        try (VCardReader vCardReader = new VCardReader(in)) {
            ezvcard.VCard vcard = null;
            while ((vcard = vCardReader.readNext()) != null) {

                //Todo create a generic service to resolve duplicate party
                FormattedName formattedName = vcard.getFormattedName();
                if (formattedName != null) {
                    String refCardId = formattedName.getValue();
                    GenericValue partyIdentification = EntityQuery.use(delegator).from("PartyIdentification").where("partyIdentificationTypeId",
                            "VCARD_FN_ORIGIN", "idValue", refCardId).queryFirst();
                    if (partyIdentification != null) {
                        partiesExist.add(UtilMisc.toMap("partyId", (String) partyIdentification.get("partyId")));
                        continue;
                    }
                    //TODO manage update
                }
                //check if it's already load
                isGroup = false;
                if (vcard.getKind() != null) isGroup = vcard.getKind().isGroup();

                StructuredName structuredName = vcard.getStructuredName();
                if (UtilValidate.isEmpty(structuredName)) continue;
                if (!isGroup) {
                    serviceCtx.put("firstName", structuredName.getGiven());
                    serviceCtx.put("lastName", structuredName.getFamily());
                }

                // Resolve all postal Address
                for (Address address : vcard.getAddresses()) {
                    boolean workAddress = false;
                    for (AddressType addressType : address.getTypes()) {
                        if (AddressType.PREF.equals(addressType) || AddressType.WORK.equals(addressType)) {
                            workAddress = true;
                            break;
                        }
                    }
                    if (!workAddress) continue;

                    serviceCtx.put("address1", address.getStreetAddressFull());
                    serviceCtx.put("city", address.getLocality());
                    serviceCtx.put("postalCode", address.getPostalCode());
                    GenericValue countryGeo = EntityQuery.use(delegator).from("Geo")
                            .where(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, "COUNTRY"),
                                    EntityCondition.makeCondition("geoName", EntityOperator.LIKE, address.getCountry()))
                            .cache().queryFirst();
                    if (countryGeo != null) {
                        serviceCtx.put("countryGeoId", countryGeo.get("geoId"));
                    }
                    GenericValue stateGeo = EntityQuery.use(delegator).from("Geo")
                            .where(EntityCondition.makeCondition("geoTypeId", EntityOperator.EQUALS, "STATE"),
                                    EntityCondition.makeCondition("geoName", EntityOperator.LIKE, address.getRegion()))
                            .cache().queryFirst();
                    if (stateGeo != null) {
                        serviceCtx.put("stateProvinceGeoId", stateGeo.get("geoId"));
                    }
                }

                int nbEmailAddr = (vcard.getEmails() != null) ? vcard.getEmails().size() : 0;
                for (Email email : vcard.getEmails()) {
                    if (nbEmailAddr > 1) {
                        boolean workEmail = false;
                        for (EmailType emailType : email.getTypes()) {
                            if (EmailType.PREF.equals(emailType) || EmailType.WORK.equals(emailType)) {
                                workEmail = true;
                                break;
                            }
                        }
                        if (!workEmail) continue;
                    }
                    String emailAddr = email.getValue();
                    if (UtilValidate.isEmail(emailAddr)) {
                        serviceCtx.put("emailAddress", emailAddr);
                    } else {
                        //TODO change uncorrect labellisation
                        String emailFormatErrMsg = UtilProperties.getMessage(RES_ERROR, "SfaImportVCardEmailFormatError", locale);
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "MarketingEmailFormatError", UtilMisc.toMap("firstName",
                                structuredName.getGiven(), "lastName", structuredName.getFamily(), "emailFOrmatErrMsg", emailFormatErrMsg), locale));
                    }
                }

                int nbPhone = (vcard.getTelephoneNumbers() != null) ? vcard.getTelephoneNumbers().size() : 0;
                for (Telephone phone : vcard.getTelephoneNumbers()) {
                    if (nbPhone > 1) {
                        boolean workPhone = false;
                        for (TelephoneType phoneType : phone.getTypes()) {
                            if (TelephoneType.PREF.equals(phoneType) || TelephoneType.WORK.equals(phoneType)) {
                                workPhone = true;
                                break;
                            }
                        }
                        if (!workPhone) continue;
                    }
                    String phoneAddr = phone.getText();
                    boolean internationalPhone = phoneAddr.startsWith("+") || phoneAddr.startsWith("00");
                    phoneAddr = StringUtil.removeNonNumeric(phoneAddr);
                    int indexLocal = 0;
                    if (internationalPhone) {
                        indexLocal = 4;
                        if (!phoneAddr.startsWith("00")) {
                            phoneAddr = phoneAddr.concat("00");
                        }
                        serviceCtx.put("areaCode", phoneAddr.substring(0, indexLocal));
                    }
                    serviceCtx.put("contactNumber", phoneAddr.substring(indexLocal));
                }

                /* TODO improve this part to manage party organization */

                GenericValue userLogin = (GenericValue) context.get("userLogin");
                serviceCtx.put("userLogin", userLogin);
                String serviceName = (String) context.get("serviceName");
                Map<String, Object> serviceContext = UtilGenerics.cast(context.get("serviceContext"));
                if (UtilValidate.isNotEmpty(serviceContext)) {
                    for (Map.Entry<String, Object> entry : serviceContext.entrySet()) {
                        serviceCtx.put(entry.getKey(), entry.getValue());
                    }
                }
                Map<String, Object> resp = dispatcher.runSync(serviceName, serviceCtx);
                if (ServiceUtil.isError(resp)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
                }
                partiesCreated.add(UtilMisc.toMap("partyId", (String) resp.get("partyId")));

                if (formattedName != null) {
                    //store the origin creation
                    Map<String, Object> createPartyIdentificationMap = dctx.makeValidContext("createPartyIdentification",
                            ModelService.IN_PARAM, context);
                    createPartyIdentificationMap.put("partyId", resp.get("partyId"));
                    createPartyIdentificationMap.put("partyIdentificationTypeId", "VCARD_FN_ORIGIN");
                    createPartyIdentificationMap.put("idValue", formattedName.getValue());
                    resp = dispatcher.runSync("createPartyIdentification", createPartyIdentificationMap);
                    if (ServiceUtil.isError(resp)) {
                        return ServiceUtil.returnError(ServiceUtil.getErrorMessage(resp));
                    }
                }
            }
        } catch (IOException | GenericEntityException | GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "SfaImportVCardError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        result.put("partiesCreated", partiesCreated);
        result.put("partiesExist", partiesExist);
        return result;
    }

    public static Map<String, Object> exportVCard(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String partyId = (String) context.get("partyId");
        Locale locale = (Locale) context.get("locale");
        File file = null;
        try {
            ezvcard.VCard vcard = new ezvcard.VCard();
            StructuredName structuredName = new StructuredName();
            GenericValue person = EntityQuery.use(delegator).from("Person").where("partyId", partyId).queryOne();
            if (person != null) {
                if (UtilValidate.isNotEmpty(person.getString("firstName"))) {
                    structuredName.setGiven(person.getString("firstName"));
                }
                if (UtilValidate.isNotEmpty(person.getString("lastName"))) {
                    structuredName.setFamily(person.getString("lastName"));
                }
                vcard.setStructuredName(structuredName);
            }
            String fullName = PartyHelper.getPartyName(delegator, partyId, false);
            vcard.setFormattedName(fullName);

            GenericValue postalAddress = PartyWorker.findPartyLatestPostalAddress(partyId, delegator);
            if (postalAddress != null) {
                Address address = new Address();
                address.setStreetAddress(postalAddress.getString("address1"));
                address.setLocality(postalAddress.getString("city"));
                address.setPostalCode(postalAddress.getString("postalCode"));
                GenericValue state = postalAddress.getRelatedOne("StateProvinceGeo", false);
                if (state != null) {
                    address.setRegion(state.getString("geoName"));
                }
                GenericValue countryGeo = postalAddress.getRelatedOne("CountryGeo", false);
                if (countryGeo != null) {
                    String country = postalAddress.getRelatedOne("CountryGeo", false).getString("geoName");
                    address.setCountry(country);
                    address.getTypes().add(AddressType.WORK);
                    //TODO : this can be better set by checking contactMechPurposeTypeId
                }
                vcard.addAddress(address);
            }

            GenericValue telecomNumber = PartyWorker.findPartyLatestTelecomNumber(partyId, delegator);
            if (telecomNumber != null) {
                Telephone tel = new Telephone(telecomNumber.getString("areaCode") + telecomNumber.getString("contactNumber"));
                tel.getTypes().add(TelephoneType.WORK);
                vcard.addTelephoneNumber(tel);
                //TODO : this can be better set by checking contactMechPurposeTypeId
            }

            GenericValue emailAddress = PartyWorker.findPartyLatestContactMech(partyId, "EMAIL_ADDRESS", delegator);
            if (emailAddress != null && UtilValidate.isNotEmpty(emailAddress.getString("infoString"))) {
                vcard.addEmail(new Email(emailAddress.getString("infoString")));
            }

            //TODO : convert to directdownload of a vcf file
            String saveToDirectory = EntityUtilProperties.getPropertyValue("sfa", "save.outgoing.directory", "", delegator);
            if (UtilValidate.isEmpty(saveToDirectory)) {
                saveToDirectory = System.getProperty("ofbiz.home");
            }
            String saveToFilename = fullName + ".vcf";
            file = FileUtil.getFile(saveToDirectory + "/" + saveToFilename);
            Ezvcard.write(vcard).go(file);
        } catch (FileNotFoundException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "SfaExportVCardErrorOpeningFile", UtilMisc.toMap("errorString", file.getAbsolutePath()), locale));
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "SfaExportVCardErrorWritingFile", UtilMisc.toMap("errorString", file.getAbsolutePath()), locale));
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR,
                    "SfaExportVCardError", UtilMisc.toMap("errorString", e.getMessage()), locale));
        }
        return ServiceUtil.returnSuccess();
    }
}
