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
package org.sitenetsoft.sunseterp.applications.accounting.thirdparty.valuelink;

import org.apache.commons.codec.binary.Base64;
import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;

import javax.crypto.*;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ValueLinkApi - Implementation of ValueLink Encryption and Transport
 */
public class ValueLinkApi {

    private static final String MODULE = ValueLinkApi.class.getName();

    // static object cache
    private static Map<String, Object> objectCache = new HashMap<>();

    // instance variables
    private Delegator delegator = null;
    private Properties props = null;
    private SecretKey kek = null;
    private SecretKey mwk = null;
    private String merchantId = null;
    private String terminalId = null;
    private Long mwkIndex = null;
    private boolean debug = false;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    protected ValueLinkApi() { }
    protected ValueLinkApi(Delegator delegator, Properties props) {
        String mId = (String) props.get("payment.valuelink.merchantId");
        String tId = (String) props.get("payment.valuelink.terminalId");
        this.delegator = delegator;
        this.merchantId = mId;
        this.terminalId = tId;
        this.props = props;
        if ("Y".equalsIgnoreCase((String) props.get("payment.valuelink.debug"))) {
            this.debug = true;
        }

        if (debug) {
            Debug.logInfo("New ValueLinkApi instance created", MODULE);
            Debug.logInfo("Merchant ID : " + merchantId, MODULE);
            Debug.logInfo("Terminal ID : " + terminalId, MODULE);
        }
    }

    /**
     * Obtain an instance of the ValueLinkApi
     * @param delegator Delegator used to query the encryption keys
     * @param props Properties to use for the Api (usually payment.properties)
     * @param reload When true, will replace an existing instance in the cache and reload all properties
     * @return ValueLinkApi reference
     */
    public static ValueLinkApi getInstance(Delegator delegator, Properties props, boolean reload) {
        if (props == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        String merchantId = (String) props.get("payment.valuelink.merchantId");

        ValueLinkApi api = (ValueLinkApi) objectCache.get(merchantId);
        if (api == null) {
            throw new RuntimeException("Runtime problems with ValueLinkApi; unable to create instance");
        }
        if (reload) {
            synchronized (ValueLinkApi.class) {
                api = (ValueLinkApi) objectCache.get(merchantId);
                if (api == null) {
                    api = new ValueLinkApi(delegator, props);
                    objectCache.put(merchantId, api);
                }
            }
        }

        return api;
    }

    /**
     * Obtain an instance of the ValueLinkApi; this method will always return an existing reference if one is available
     * @param delegator Delegator used to query the encryption keys
     * @param props Properties to use for the Api (usually payment.properties)
     * @return Obtain an instance of the ValueLinkApi
     */
    public static ValueLinkApi getInstance(Delegator delegator, Properties props) {
        return getInstance(delegator, props, false);
    }

    /**
     * Encrypt the defined pin using the configured keys
     * @param pin Plain text String of the pin
     * @return Hex String of the encrypted pin (EAN) for transmission to ValueLink
     */
    public String encryptPin(String pin) {
        byte[] rawIv = new byte[8];
        SECURE_RANDOM.nextBytes(rawIv);
        IvParameterSpec iv = new IvParameterSpec(rawIv);
        // get the Cipher
        Cipher mwkCipher = null;
        try {
            mwkCipher = getCipher(this.getMwkKey(), Cipher.ENCRYPT_MODE, iv);
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }

        // pin to bytes
        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);

        // 7 bytes of random data
        byte[] randomBytes = this.getRandomBytes(7);

        // pin checksum
        byte[] checkSum = this.getPinCheckSum(pinBytes);

        // put all together
        byte[] eanBlock = new byte[16];
        int i;
        for (i = 0; i < randomBytes.length; i++) {
            eanBlock[i] = randomBytes[i];
        }
        eanBlock[7] = checkSum[0];
        for (i = 0; i < pinBytes.length; i++) {
            eanBlock[i + 8] = pinBytes[i];
        }

        // encrypy the ean
        String encryptedEanHex = null;
        try {
            byte[] encryptedEan = mwkCipher.doFinal(eanBlock);
            encryptedEanHex = StringUtil.toHexString(encryptedEan);
        } catch (IllegalStateException | BadPaddingException | IllegalBlockSizeException e) {
            Debug.logError(e, MODULE);
        }

        if (debug) {
            Debug.logInfo("encryptPin : " + pin + " / " + encryptedEanHex, MODULE);
        }

        return encryptedEanHex;
    }

    /**
     * Decrypt an encrypted pin using the configured keys
     * @param pin Hex String of the encrypted pin (EAN)
     * @return Plain text String of the pin @
     */
    public String decryptPin(String pin) {
        // separate prefix with IV from the rest of encrypted data
        byte[] encryptedPayload = Base64.decodeBase64(pin.getBytes());
        byte[] iv = new byte[8];
        byte[] encryptedBytes = new byte[encryptedPayload.length - iv.length];

        // populate iv with bytes:
        System.arraycopy(encryptedPayload, 0, iv, 0, iv.length);

        // populate encryptedBytes with bytes:
        System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length);

        Cipher mwkCipher = null;
        try {
            mwkCipher = getCipher(getMwkKey(), Cipher.ENCRYPT_MODE, new IvParameterSpec(iv));
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }

        // decrypt pin
        String decryptedPinString = null;
        try {
            byte[] decryptedEan = mwkCipher.doFinal(StringUtil.fromHexString(pin));
            byte[] decryptedPin = getByteRange(decryptedEan, 8, 8);
            decryptedPinString = new String(decryptedPin, StandardCharsets.UTF_8);
        } catch (IllegalStateException | BadPaddingException | IllegalBlockSizeException e) {
            Debug.logError(e, MODULE);
        }

        if (debug) {
            Debug.logInfo("decryptPin : " + pin + " / " + decryptedPinString, MODULE);
        }

        return decryptedPinString;
    }

    /**
     * Transmit a request to ValueLink
     * @param request Map of request parameters
     * @return Map of response parameters
     * @throws HttpClientException
     */
    public Map<String, Object> send(Map<String, Object> request) throws HttpClientException {
        return send((String) props.get("payment.valuelink.url"), request);
    }

    /**
     * Transmit a request to ValueLink
     * @param url override URL from what is defined in the properties
     * @param request request Map of request parameters
     * @return Map of response parameters
     * @throws HttpClientException
     */
    public Map<String, Object> send(String url, Map<String, Object> request) throws HttpClientException {
        if (debug) {
            Debug.logInfo("Request : " + url + " / " + request, MODULE);
        }

        // read the timeout value
        String timeoutString = (String) props.get("payment.valuelink.timeout");
        int timeout = 34;
        try {
            timeout = Integer.parseInt(timeoutString);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to set timeout to " + timeoutString + " using default " + timeout);
        }

        // create the HTTP client
        HttpClient client = new HttpClient(url, request);
        client.setTimeout(timeout * 1000);
        client.setDebug(debug);

        client.setClientCertificateAlias((String) props.get("payment.valuelink.certificateAlias"));
        String response = client.post();

        // parse the response and return a map
        return this.parseResponse(response);
    }

    /**
     * Output the creation of public/private keys + KEK to the console for manual database update @
     */
    public StringBuffer outputKeyCreation(boolean kekOnly, String kekTest) {
        return this.outputKeyCreation(0, kekOnly, kekTest);
    }

    private StringBuffer outputKeyCreation(int loop, boolean kekOnly, String kekTest) {
        StringBuffer buf = new StringBuffer();
        loop++;

        if (loop > 100) {
            // only loop 100 times; then throw an exception
            throw new IllegalStateException("Unable to create 128 byte keys in 100 tries");
        }

        // place holder for the keys
        DHPrivateKey privateKey = null;
        DHPublicKey publicKey = null;

        if (!kekOnly) {
            KeyPair keyPair = null;
            try {
                keyPair = this.createKeys();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException e) {
                Debug.logError(e, MODULE);
            }

            if (keyPair != null) {
                publicKey = (DHPublicKey) keyPair.getPublic();
                privateKey = (DHPrivateKey) keyPair.getPrivate();

                if (publicKey == null || publicKey.getY().toByteArray().length != 128) {
                    // run again until we get a 128 byte public key for VL
                    return this.outputKeyCreation(loop, kekOnly, kekTest);
                }
            } else {
                Debug.logInfo("Returned a null KeyPair", MODULE);
                return this.outputKeyCreation(loop, kekOnly, kekTest);
            }
        } else {
            // use our existing private key to generate a KEK
            try {
                privateKey = (DHPrivateKey) this.getPrivateKey();
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }

        // the KEK
        byte[] kekBytes = null;
        try {
            kekBytes = this.generateKek(privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
            Debug.logError(e, MODULE);
        }

        // the 3DES KEK value
        SecretKey loadedKek = this.getDesEdeKey(kekBytes);
        byte[] loadKekBytes = loadedKek.getEncoded();

        // test the KEK
        byte[] rawIv = new byte[8];
        SECURE_RANDOM.nextBytes(rawIv);
        IvParameterSpec iv = new IvParameterSpec(rawIv);

        Cipher cipher = null;
        try {
            cipher = getCipher(this.getKekKey(), Cipher.ENCRYPT_MODE, iv);
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }
        byte[] kekTestB = {0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] kekTestC = new byte[0];
        if (kekTest != null) {
            kekTestB = StringUtil.fromHexString(kekTest);
        }

        // encrypt the test bytes
        try {
            kekTestC = cipher.doFinal(kekTestB);
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }

        if (!kekOnly) {
            // public key (just Y)
            BigInteger y = publicKey.getY();
            byte[] yBytes = y.toByteArray();
            String yHex = StringUtil.toHexString(yBytes);
            buf.append("======== Begin Public Key (Y @ ").append(yBytes.length).append(" / ").append(yHex.length()).append(") ========\n");
            buf.append(yHex).append("\n");
            buf.append("======== End Public Key ========\n\n");

            // private key (just X)
            BigInteger x = privateKey.getX();
            byte[] xBytes = x.toByteArray();
            String xHex = StringUtil.toHexString(xBytes);
            buf.append("======== Begin Private Key (X @ ").append(xBytes.length).append(" / ").append(xHex.length()).append(") ========\n");
            buf.append(xHex).append("\n");
            buf.append("======== End Private Key ========\n\n");

            // private key (full)
            byte[] privateBytes = privateKey.getEncoded();
            String privateHex = StringUtil.toHexString(privateBytes);
            buf.append("======== Begin Private Key (Full @ ").append(privateBytes.length).append(" / ").append(privateHex.length())
                    .append(") ========\n");
            buf.append(privateHex).append("\n");
            buf.append("======== End Private Key ========\n\n");
        }

        if (kekBytes != null) {
            buf.append("======== Begin KEK (").append(kekBytes.length).append(") ========\n");
            buf.append(StringUtil.toHexString(kekBytes)).append("\n");
            buf.append("======== End KEK ========\n\n");

            buf.append("======== Begin KEK (DES) (").append(loadKekBytes.length).append(") ========\n");
            buf.append(StringUtil.toHexString(loadKekBytes)).append("\n");
            buf.append("======== End KEK (DES) ========\n\n");

            buf.append("======== Begin KEK Test (").append(kekTestC.length).append(") ========\n");
            buf.append(StringUtil.toHexString(kekTestC)).append("\n");
            buf.append("======== End KEK Test ========\n\n");
        } else {
            Debug.logError("KEK came back empty", MODULE);
        }

        return buf;
    }

    /**
     * Create a set of public/private keys using ValueLinks defined parameters
     * @return KeyPair object containing both public and private keys
     * @throws NoSuchAlgorithmException
     * @throws InvalidAlgorithmParameterException
     */
    public KeyPair createKeys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException {
        // initialize the parameter spec
        DHPublicKey publicKey = (DHPublicKey) this.getValueLinkPublicKey();
        DHParameterSpec dhParamSpec = publicKey.getParams();
        // create the public/private key pair using parameters defined by valuelink
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhParamSpec);
        KeyPair keyPair = keyGen.generateKeyPair();

        return keyPair;
    }

    /**
     * Generate a key exchange key for use in encrypting the mwk
     * @param privateKey The private key for the merchant
     * @return byte array containing the kek
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
    public byte[] generateKek(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        // get the ValueLink public key
        PublicKey vlPublic = this.getValueLinkPublicKey();

        // generate shared secret key
        KeyAgreement ka = KeyAgreement.getInstance("DH");
        ka.init(privateKey);
        ka.doPhase(vlPublic, true);
        byte[] secretKey = ka.generateSecret();

        if (debug) {
            Debug.logInfo("Secret Key : " + StringUtil.toHexString(secretKey) + " / " + secretKey.length, MODULE);
        }

        // generate 3DES from secret key using VL algorithm (KEK)
        MessageDigest md = MessageDigest.getInstance("SHA1");
        byte[] digest = md.digest(secretKey);
        byte[] des2 = getByteRange(digest, 0, 16);
        byte[] first8 = getByteRange(des2, 0, 8);
        byte[] kek = copyBytes(des2, first8, 0);

        if (debug) {
            Debug.logInfo("Generated KEK : " + StringUtil.toHexString(kek) + " / " + kek.length, MODULE);
        }

        return kek;
    }

    /**
     * Get a public key object for the ValueLink supplied public key
     * @return PublicKey object of ValueLinks's public key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public PublicKey getValueLinkPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // read the valuelink public key
        String publicValue = (String) props.get("payment.valuelink.publicValue");
        byte[] publicKeyBytes = StringUtil.fromHexString(publicValue);

        // initialize the parameter spec
        DHParameterSpec dhParamSpec = this.getDHParameterSpec();

        // load the valuelink public key
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        BigInteger publicKeyInt = new BigInteger(publicKeyBytes);
        DHPublicKeySpec dhPublicSpec = new DHPublicKeySpec(publicKeyInt, dhParamSpec.getP(), dhParamSpec.getG());
        PublicKey vlPublic = keyFactory.generatePublic(dhPublicSpec);

        return vlPublic;
    }

    /**
     * Get merchant Private Key
     * @return PrivateKey object for the merchant
     */
    public PrivateKey getPrivateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] privateKeyBytes = this.getPrivateKeyBytes();

        // initialize the parameter spec
        DHParameterSpec dhParamSpec = this.getDHParameterSpec();

        // load the private key
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        BigInteger privateKeyInt = new BigInteger(privateKeyBytes);
        DHPrivateKeySpec dhPrivateSpec = new DHPrivateKeySpec(privateKeyInt, dhParamSpec.getP(), dhParamSpec.getG());
        PrivateKey privateKey = keyFactory.generatePrivate(dhPrivateSpec);

        return privateKey;
    }

    /**
     * Generate a new MWK
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink @
     */
    public byte[] generateMwk() {
        KeyGenerator keyGen = null;
        try {
            keyGen = KeyGenerator.getInstance("DES");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, MODULE);
        }

        // generate the DES key 1
        SecretKey des1 = keyGen.generateKey();
        SecretKey des2 = keyGen.generateKey();

        if (des1 != null && des2 != null) {
            byte[] desByte1 = des1.getEncoded();
            byte[] desByte2 = des2.getEncoded();
            byte[] desByte3 = des1.getEncoded();

            // check for weak keys
            try {
                if (DESKeySpec.isWeak(des1.getEncoded(), 0) || DESKeySpec.isWeak(des2.getEncoded(), 0)) {
                    return generateMwk();
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }

            byte[] des3 = copyBytes(desByte1, copyBytes(desByte2, desByte3, 0), 0);
            return generateMwk(des3);
        }

        Debug.logInfo("Null DES keys returned", MODULE);
        return null;
    }

    /**
     * Generate a new MWK
     * @param desBytes byte array of the DES key (24 bytes)
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink @
     */
    public byte[] generateMwk(byte[] desBytes) {
        if (debug) {
            Debug.logInfo("DES Key : " + StringUtil.toHexString(desBytes) + " / " + desBytes.length, MODULE);
        }
        SecretKeyFactory skf1 = null;
        SecretKey mwk = null;
        try {
            skf1 = SecretKeyFactory.getInstance("DESede");
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, MODULE);
        }
        DESedeKeySpec desedeSpec2 = null;
        try {
            desedeSpec2 = new DESedeKeySpec(desBytes);
        } catch (InvalidKeyException e) {
            Debug.logError(e, MODULE);
        }
        if (skf1 != null && desedeSpec2 != null) {
            try {
                mwk = skf1.generateSecret(desedeSpec2);
            } catch (InvalidKeySpecException e) {
                Debug.logError(e, MODULE);
            }
        }
        if (mwk != null) {
            return generateMwk(mwk);
        }
        return null;
    }

    /**
     * Generate a new MWK
     * @param mwkdes3 pre-generated DES3 SecretKey
     * @return Hex String of the new encrypted MWK ready for transmission to ValueLink @
     */
    public byte[] generateMwk(SecretKey mwkdes3) {
        // zeros for checksum
        byte[] zeros = {0, 0, 0, 0, 0, 0, 0, 0 };

        // 8 bytes random data
        byte[] random = new byte[8];

        byte[] rawIv = new byte[8];
        SECURE_RANDOM.nextBytes(rawIv);
        IvParameterSpec iv = new IvParameterSpec(rawIv);

        // open a cipher using the new mwk
        Cipher cipher = null;
        try {
            cipher = getCipher(mwkdes3, Cipher.ENCRYPT_MODE, iv);
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }

        // make the checksum - encrypted 8 bytes of 0's
        byte[] encryptedZeros = new byte[0];
        try {
            encryptedZeros = cipher.doFinal(zeros);
        } catch (IllegalStateException | BadPaddingException | IllegalBlockSizeException e) {
            Debug.logError(e, MODULE);
        }

        // make the 40 byte MWK - random 8 bytes + key + checksum
        byte[] newMwk = copyBytes(mwkdes3.getEncoded(), encryptedZeros, 0);
        newMwk = copyBytes(random, newMwk, 0);

        if (debug) {
            Debug.logInfo("Random 8 byte : " + StringUtil.toHexString(random), MODULE);
            Debug.logInfo("Encrypted 0's : " + StringUtil.toHexString(encryptedZeros), MODULE);
            Debug.logInfo("Decrypted MWK : " + StringUtil.toHexString(mwkdes3.getEncoded()) + " / " + mwkdes3.getEncoded().length, MODULE);
            Debug.logInfo("Encrypted MWK : " + StringUtil.toHexString(newMwk) + " / " + newMwk.length, MODULE);
        }

        return newMwk;
    }

    /**
     * Use the KEK to encrypt a value usually the MWK
     * @param content byte array to encrypt
     * @return encrypted byte array
     */
    public byte[] encryptViaKek(byte[] content) {
        try {
            return cryptoViaKek(content, Cipher.ENCRYPT_MODE);
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }
        return null;
    }

    /**
     * Ue the KEK to decrypt a value
     * @param content byte array to decrypt
     * @return decrypted byte array
     */
    public byte[] decryptViaKek(byte[] content) {
        try {
            return cryptoViaKek(content, Cipher.DECRYPT_MODE);
        } catch (GeneralException e1) {
            Debug.logError(e1, MODULE);
        }
        return null;
    }

    /**
     * Returns a date string formatted as directed by ValueLink
     * @return ValueLink formatted date String
     */
    public String getDateString() {
        String format = (String) props.get("payment.valuelink.timestamp");
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date());
    }

    /**
     * Returns the current working key index
     * @return Long number of the current working key index
     */
    public Long getWorkingKeyIndex() {
        if (this.mwkIndex == null) {
            synchronized (this) {
                if (this.mwkIndex == null) {
                    this.mwkIndex = this.getGenericValue().getLong("workingKeyIndex");
                }
            }
        }

        if (debug) {
            Debug.logInfo("Current Working Key Index : " + this.mwkIndex, MODULE);
        }

        return this.mwkIndex;
    }

    /**
     * Returns a ValueLink formatted amount String
     * @param amount BigDecimal value to format
     * @return Formatted String
     */
    public String getAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return Integer.toString(amount.movePointRight(2).intValue());
    }

    /**
     * Returns a BigDecimal from a ValueLink formatted amount String
     * @param amount The ValueLink formatted amount String
     * @return BigDecimal object
     */
    public BigDecimal getAmount(String amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amountBd = new BigDecimal(amount);
        return amountBd.movePointLeft(2);
    }

    /**
     * Gets currency.
     * @param currency the currency
     * @return the currency
     */
    public String getCurrency(String currency) {
        return "840"; // todo make this multi-currency
    }

    /**
     * Creates a Map of initial request values (MerchID, AltMerchNo, Modes, MerchTime, TermTxnNo, EncryptID)
     * Note: For 2010 (assign working key) transaction, the EncryptID will need to be adjusted
     * @return Map containing the inital request values
     */
    public Map<String, Object> getInitialRequestMap(Map<String, Object> context) {
        Map<String, Object> request = new HashMap<>();

        // merchant information
        request.put("MerchID", merchantId + terminalId);
        request.put("AltMerchNo", props.get("payment.valuelink.altMerchantId"));

        // mode settings
        String modes = (String) props.get("payment.valuelink.modes");
        if (UtilValidate.isNotEmpty(modes)) {
            request.put("Modes", modes);
        }

        // merchant timestamp
        String merchTime = (String) context.get("MerchTime");
        if (merchTime == null) {
            merchTime = this.getDateString();
        }
        request.put("MerchTime", merchTime);

        // transaction number
        String termTxNo = (String) context.get("TermTxnNo");
        if (termTxNo == null) {
            termTxNo = delegator.getNextSeqId("ValueLinkKey");
        }
        request.put("TermTxnNo", termTxNo);

        // current working key index
        request.put("EncryptID", this.getWorkingKeyIndex());

        if (debug) {
            Debug.logInfo("Created Initial Request Map : " + request, MODULE);
        }

        return request;
    }

    /**
     * Gets the cached value object for this merchant's keys
     * @return Cached GenericValue object
     */
    public GenericValue getGenericValue() {
        GenericValue value = null;
        try {
            value = EntityQuery.use(delegator).from("ValueLinkKey").where("merchantId", merchantId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        if (value == null) {
            throw new RuntimeException("No ValueLinkKey record found for Merchant ID : " + merchantId);
        }
        return value;
    }

    /**
     * Reloads the keys in the object cache; use this when re-creating keys
     */
    public void reload() {
        this.kek = null;
        this.mwk = null;
        this.mwkIndex = null;
    }

    /** using the prime and generator provided by valuelink; create a parameter object */
    protected DHParameterSpec getDHParameterSpec() {
        String primeHex = (String) props.get("payment.valuelink.prime");
        String genString = (String) props.get("payment.valuelink.generator");

        // convert the p/g hex values
        byte[] primeByte = StringUtil.fromHexString(primeHex);
        BigInteger prime = new BigInteger(1, primeByte); // force positive (unsigned)
        BigInteger generator = new BigInteger(genString);

        // initialize the parameter spec
        DHParameterSpec dhParamSpec = new DHParameterSpec(prime, generator, 1024);

        return dhParamSpec;
    }

    /**
     * actual kek encryption/decryption code
     * @throws GeneralException
     */
    protected byte[] cryptoViaKek(byte[] content, int mode) throws GeneralException {
        // open a cipher using the kek for transport
        byte[] rawIv = new byte[8];
        SECURE_RANDOM.nextBytes(rawIv);
        IvParameterSpec iv = new IvParameterSpec(rawIv);

        // Create the Cipher - DESede/CBC/PKCS5Padding
        byte[] encBytes = null;
        Cipher cipher = getCipher(getKekKey(), mode, iv);
        try {
            encBytes = cipher.doFinal(content);
        } catch (IllegalStateException | BadPaddingException | IllegalBlockSizeException e) {
            Debug.logError(e, MODULE);
        }
        // Prepend iv as a prefix to use it during decryption
        byte[] combinedPayload = new byte[rawIv.length + encBytes.length];

        // populate payload with prefix iv and encrypted data
        System.arraycopy(iv, 0, combinedPayload, 0, rawIv.length);
        System.arraycopy(cipher, 0, combinedPayload, 8, encBytes.length);

        return encBytes;
    }

    // return a cipher for a key - DESede/CBC/NoPadding with random IV
    protected static Cipher getCipher(Key key, int mode, IvParameterSpec iv) throws GeneralException {
        // create the Cipher - DESede/CBC/NoPadding
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new GeneralException(e);
        }
        try {
            cipher.init(mode, key, iv);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new GeneralException(e);
        }
        return cipher;
    }

    /**
     * Get pin check sum byte [ ].
     * @param pinBytes the pin bytes
     * @return the byte [ ]
     */
    protected byte[] getPinCheckSum(byte[] pinBytes) {
        byte[] checkSum = new byte[1];
        checkSum[0] = 0;
        for (byte pinByte : pinBytes) {
            checkSum[0] += pinByte;
        }
        return checkSum;
    }

    /**
     * Get random bytes byte [ ].
     * @param length the length
     * @return the byte [ ]
     */
    protected byte[] getRandomBytes(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Gets mwk key.
     * @return the mwk key
     */
    protected SecretKey getMwkKey() {
        if (mwk == null) {
            mwk = this.getDesEdeKey(getByteRange(getMwk(), 8, 24));
        }

        if (debug) {
            Debug.logInfo("Raw MWK : " + StringUtil.toHexString(getMwk()), MODULE);
            Debug.logInfo("MWK : " + StringUtil.toHexString(mwk.getEncoded()), MODULE);
        }

        return mwk;
    }

    /**
     * Gets kek key.
     * @return the kek key
     */
    protected SecretKey getKekKey() {
        if (kek == null) {
            kek = this.getDesEdeKey(getKek());
        }

        if (debug) {
            Debug.logInfo("Raw KEK : " + StringUtil.toHexString(getKek()), MODULE);
            Debug.logInfo("KEK : " + StringUtil.toHexString(kek.getEncoded()), MODULE);
        }

        return kek;
    }

    /**
     * Gets des ede key.
     * @param rawKey the raw key
     * @return the des ede key
     */
    protected SecretKey getDesEdeKey(byte[] rawKey) {
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("DESede");
        } catch (NoSuchAlgorithmException e) {
            // should never happen since DESede is a standard algorithm
            Debug.logError(e, MODULE);
            return null;
        }

        // load the raw key
        if (rawKey.length > 0) {
            DESedeKeySpec desedeSpec1 = null;
            try {
                desedeSpec1 = new DESedeKeySpec(rawKey);
            } catch (InvalidKeyException e) {
                Debug.logError(e, "Not a valid DESede key", MODULE);
                return null;
            }

            // create the SecretKey Object
            SecretKey key = null;
            try {
                key = skf.generateSecret(desedeSpec1);
            } catch (InvalidKeySpecException e) {
                Debug.logError(e, MODULE);
            }
            return key;
        } else {
            throw new RuntimeException("No valid DESede key available");
        }
    }

    /**
     * Get mwk byte [ ].
     * @return the byte [ ]
     */
    protected byte[] getMwk() {
        return StringUtil.fromHexString(this.getGenericValue().getString("workingKey"));
    }

    /**
     * Get kek byte [ ].
     * @return the byte [ ]
     */
    protected byte[] getKek() {
        return StringUtil.fromHexString(this.getGenericValue().getString("exchangeKey"));
    }

    /**
     * Get private key bytes byte [ ].
     * @return the byte [ ]
     */
    protected byte[] getPrivateKeyBytes() {
        return StringUtil.fromHexString(this.getGenericValue().getString("privateKey"));
    }

    /**
     * Parse response map.
     * @param response the response
     * @return the map
     */
    protected Map<String, Object> parseResponse(String response) {
        if (debug) {
            Debug.logInfo("Raw Response : " + response, MODULE);
        }

        // covert to all lowercase and trim off the html header
        String subResponse = response.toLowerCase(Locale.getDefault());
        int firstIndex = subResponse.indexOf("<tr>");
        int lastIndex = subResponse.lastIndexOf("</tr>");
        subResponse = subResponse.substring(firstIndex, lastIndex);

        // check for a history table
        String history = null;
        List<Map<String, String>> historyMapList = null;
        if (subResponse.indexOf("<table") > -1) {
            int startHistory = subResponse.indexOf("<table");
            int endHistory = subResponse.indexOf("</table>") + 8;
            history = subResponse.substring(startHistory, endHistory);

            // replace the subResponse string so it doesn't conflict
            subResponse = StringUtil.replaceString(subResponse, history, "[_HISTORY_]");

            // parse the history into a list of maps
            historyMapList = this.parseHistoryResponse(history);
        }

        // replace all end rows with | this is the name delimiter
        subResponse = StringUtil.replaceString(subResponse, "</tr>", "|");

        // replace all </TD><TD> with = this is the value delimiter
        subResponse = StringUtil.replaceString(subResponse, "</td><td>", "=");

        // clean off a bunch of other useless stuff
        subResponse = StringUtil.replaceString(subResponse, "<tr>", "");
        subResponse = StringUtil.replaceString(subResponse, "<td>", "");
        subResponse = StringUtil.replaceString(subResponse, "</td>", "");

        // make the map
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.putAll(StringUtil.strToMap(subResponse, true));

        // add the raw html back in just in case we need it later
        responseMap.put("_rawHtmlResponse", response);

        // if we have a history add it back in
        if (history != null) {
            responseMap.put("_rawHistoryHtml", history);
            responseMap.put("history", historyMapList);
        }

        if (debug) {
            Debug.logInfo("Response Map : " + responseMap, MODULE);
        }

        return responseMap;
    }

    private List<Map<String, String>> parseHistoryResponse(String response) {
        if (debug) {
            Debug.logInfo("Raw History : " + response, MODULE);
        }

        // covert to all lowercase and trim off the html header
        String subResponse = response.toLowerCase(Locale.getDefault());
        int firstIndex = subResponse.indexOf("<tr>");
        int lastIndex = subResponse.lastIndexOf("</tr>");
        subResponse = subResponse.substring(firstIndex, lastIndex);

        // clean up the html and replace the delimiters with '|'
        subResponse = StringUtil.replaceString(subResponse, "<td>", "");
        subResponse = StringUtil.replaceString(subResponse, "</td>", "|");

        // test the string to make sure we have fields to parse
        String testResponse = StringUtil.replaceString(subResponse, "<tr>", "");
        testResponse = StringUtil.replaceString(testResponse, "</tr>", "");
        testResponse = StringUtil.replaceString(testResponse, "|", "");
        testResponse = testResponse.trim();
        if (testResponse.isEmpty()) {
            if (debug) {
                Debug.logInfo("History did not contain any fields, returning null", MODULE);
            }
            return null;
        }

        // break up the keys from the values
        int valueStart = subResponse.indexOf("</tr>");
        String keys = subResponse.substring(4, valueStart - 1);
        String values = subResponse.substring(valueStart + 9, subResponse.length() - 6);

        // split sets of values up
        values = StringUtil.replaceString(values, "|</tr><tr>", "&");
        List<String> valueList = StringUtil.split(values, "&");

        // create a List of Maps for each set of values
        List<Map<String, String>> valueMap = new LinkedList<>();
        for (String s : valueList) {
            valueMap.add(StringUtil.createMap(StringUtil.split(keys, "|"), StringUtil.split(s, "|")));
        }

        if (debug) {
            Debug.logInfo("History Map : " + valueMap, MODULE);
        }

        return valueMap;
    }

    /**
     * Returns a new byte[] from the offset of the defined byte[] with a specific number of bytes
     * @param bytes The byte[] to extract from
     * @param offset The starting postition
     * @param length The number of bytes to copy
     * @return a new byte[]
     */
    public static byte[] getByteRange(byte[] bytes, int offset, int length) {
        byte[] newBytes = new byte[length];
        for (int i = 0; i < length; i++) {
            newBytes[i] = bytes[offset + i];
        }
        return newBytes;
    }

    /**
     * Copies a byte[] into another byte[] starting at a specific position
     * @param source byte[] to copy from
     * @param target byte[] coping into
     * @param position the position on target where source will be copied to
     * @return a new byte[]
     */
    public static byte[] copyBytes(byte[] source, byte[] target, int position) {
        byte[] newBytes = new byte[target.length + source.length];
        for (int i = 0, n = 0, x = 0; i < newBytes.length; i++) {
            if (i < position || i > (position + source.length - 2)) {
                newBytes[i] = target[n];
                n++;
            } else {
                for (; x < source.length; x++) {
                    newBytes[i] = source[x];
                    if (source.length - 1 > x) {
                        i++;
                    }
                }
            }
        }
        return newBytes;
    }
}
