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
package org.sitenetsoft.sunseterp.framework.common.image;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilXml;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



/**
 * ImageTransform Class
 * <p>
 * Services to apply transformation to images
 */
public class ImageTransform {

    private static final String MODULE = ImageTransform.class.getName();
    private static final String RESOURCE = "CommonErrorUiLabels";

    public ImageTransform() {
    }

    /**
     * getBufferedImage
     * <p>
     * Set a buffered image
     * @param   fileLocation    Full file Path or URL
     * @return  URL images for all different size types
     * @throws  IOException Error prevents the document from being fully parsed
     * @throws  IllegalArgumentException Errors occur in parsing
     */
    public static Map<String, Object> getBufferedImage(String fileLocation, Locale locale)
        throws IllegalArgumentException, IOException {

        /* VARIABLES */
        BufferedImage bufImg;
        Map<String, Object> result = new LinkedHashMap<>();

        /* BUFFERED IMAGE */
        try {
            bufImg = ImageIO.read(new File(fileLocation));
        } catch (IllegalArgumentException e) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.input_is_null", locale) + " : " + fileLocation + "; " + e.toString();
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        } catch (IOException e) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.error_occurs_during_reading", locale) + " : "
                    + fileLocation + "; " + e.toString();
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        result.put("responseMessage", "success");
        result.put("bufferedImage", bufImg);
        return result;

    }

    /**
     * scaleImage
     * <p>
     * scale original image related to the ImageProperties.xml dimensions
     * @param   bufImg          Buffered image to scale
     * @param   imgHeight       Original image height
     * @param   imgWidth        Original image width
     * @param   dimensionMap    Image dimensions by size type
     * @param   sizeType        Size type to scale
     * @return                  New scaled buffered image
     */
    public static Map<String, Object> scaleImage(BufferedImage bufImg, double imgHeight, double imgWidth, Map<String, Map<String, String>>
            dimensionMap, String sizeType, Locale locale) {

        /* VARIABLES */
        BufferedImage bufNewImg;
        double defaultHeight;
        double defaultWidth;
        double scaleFactor;
        Map<String, Object> result = new LinkedHashMap<>();

        /* DIMENSIONS from ImageProperties */
        // A missed dimension is authorized
        if (dimensionMap.get(sizeType).containsKey("height")) {
            defaultHeight = Double.parseDouble(dimensionMap.get(sizeType).get("height"));
        } else {
            defaultHeight = -1;
        }
        if (dimensionMap.get(sizeType).containsKey("width")) {
            defaultWidth = Double.parseDouble(dimensionMap.get(sizeType).get("width"));
        } else {
            defaultWidth = -1;
        }
        if (defaultHeight == 0.0 || defaultWidth == 0.0) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.one_default_dimension_is_null", locale)
                    + " : defaultHeight = " + defaultHeight + "; defaultWidth = " + defaultWidth;
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }

        /* SCALE FACTOR */
        // find the right Scale Factor related to the Image Dimensions
        if (defaultHeight == -1) {
            scaleFactor = defaultWidth / imgWidth;
            if (scaleFactor == 0.0) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.width_scale_factor_is_null", locale)
                        + "  (defaultWidth = " + defaultWidth + "; imgWidth = " + imgWidth;
                Debug.logError(errMsg, MODULE);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
        } else if (defaultWidth == -1) {
            scaleFactor = defaultHeight / imgHeight;
            if (scaleFactor == 0.0) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.height_scale_factor_is_null", locale)
                        + "  (defaultHeight = " + defaultHeight + "; imgHeight = " + imgHeight;
                Debug.logError(errMsg, MODULE);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
        } else if (imgHeight > imgWidth) {
            scaleFactor = defaultHeight / imgHeight;
            if (scaleFactor == 0.0) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.height_scale_factor_is_null", locale)
                        + "  (defaultHeight = " + defaultHeight + "; imgHeight = " + imgHeight;
                Debug.logError(errMsg, MODULE);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
            // get scaleFactor from the smallest width
            if (defaultWidth < (imgWidth * scaleFactor)) {
                scaleFactor = defaultWidth / imgWidth;
            }
        } else {
            scaleFactor = defaultWidth / imgWidth;
            if (scaleFactor == 0.0) {
                String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.width_scale_factor_is_null", locale)
                        + "  (defaultWidth = " + defaultWidth + "; imgWidth = " + imgWidth;
                Debug.logError(errMsg, MODULE);
                result.put(ModelService.ERROR_MESSAGE, errMsg);
                return result;
            }
            // get scaleFactor from the smallest height
            if (defaultHeight < (imgHeight * scaleFactor)) {
                scaleFactor = defaultHeight / imgHeight;
            }
        }

        if (scaleFactor == 0.0) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.final_scale_factor_is_null", locale) + " = " + scaleFactor;
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, errMsg);
            return result;
        }
        int bufImgType;
        if (BufferedImage.TYPE_CUSTOM == bufImg.getType()) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.unknown_buffered_image_type", locale);
            Debug.logWarning(errMsg, MODULE);
            // apply a type for image majority
            bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
        } else {
            bufImgType = bufImg.getType();
        }

        // scale original image with new size
        Image newImg = bufImg.getScaledInstance((int) (imgWidth * scaleFactor), (int) (imgHeight * scaleFactor), Image.SCALE_SMOOTH);

        bufNewImg = ImageTransform.toBufferedImage(newImg, bufImgType);

        result.put("responseMessage", "success");
        result.put("bufferedImage", bufNewImg);
        result.put("scaleFactor", scaleFactor);
        return result;

    }

    /**
     * getXMLValue
     * <p>
     * From a XML element, get a values map
     * @param fileFullPath      File path to parse
     * @return Map contains asked attribute values by attribute name
     */
    public static Map<String, Object> getXMLValue(String fileFullPath, Locale locale)
        throws IllegalStateException, IOException {

        /* VARIABLES */
        Document document;
        Element rootElt;
        Map<String, Map<String, String>> valueMap = new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();

        /* PARSING */
        try {
            document = UtilXml.readXmlDocument(new FileInputStream(fileFullPath), fileFullPath);
        } catch (ParserConfigurationException | SAXException e) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.errors_occurred_during_parsing", locale)
                    + " ImageProperties.xml " + e.toString();
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, "error");
            return result;
        } catch (IOException e) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.error_prevents_the document_from_being_fully_parsed",
                    locale) + e.toString();
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, "error");
            return result;
        }
        // set Root Element
        try {
            rootElt = document.getDocumentElement();
        } catch (IllegalStateException e) {
            String errMsg = UtilProperties.getMessage(RESOURCE, "ImageTransform.root_element_has_not_been_set", locale) + e.toString();
            Debug.logError(errMsg, MODULE);
            result.put(ModelService.ERROR_MESSAGE, "error");
            return result;
        }

        /* get NAME and VALUE */
        List<? extends Element> children = UtilXml.childElementList(rootElt);
        // FIXME : despite upgrading to jdom 1.1, it seems that getChildren is pre 1.5 java code (ie getChildren does not retun List<Element>
        //  but only List)
        for (Element currentElt : children) {
            Map<String, String> eltMap = new LinkedHashMap<>();
            List<? extends Element> children2 = UtilXml.childElementList(currentElt);
            if (!children2.isEmpty()) {
                Map<String, String> childMap = new LinkedHashMap<>();
                // loop over Children 1st level
                for (Element currentChild : children2) {
                    childMap.put(currentChild.getAttribute("name"), currentChild.getAttribute("value"));
                }
                valueMap.put(currentElt.getAttribute("name"), childMap);
            } else {
                eltMap.put(currentElt.getAttribute("name"), currentElt.getAttribute("value"));
                valueMap.put(currentElt.getNodeName(), eltMap);
            }
        }

        result.put("responseMessage", "success");
        result.put("xml", valueMap);
        return result;

    }

    /**
     * toBufferedImage
     * <p>
     * Transform from an Image instance to a BufferedImage instance
     * @param image             Source image
     * @return BufferedImage
     */
    public static BufferedImage toBufferedImage(Image image) {
        return ImageTransform.toBufferedImage(image, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    public static BufferedImage toBufferedImage(Image image, int bufImgType) {
        /** Check if the image isn't already a BufferedImage instance */
        if (image instanceof BufferedImage) {
            return ((BufferedImage) image);
        }
        /** Full image loading */
        image = new ImageIcon(image).getImage();

        /** new BufferedImage creation */
        BufferedImage bufferedImage = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    bufImgType);

        Graphics2D g = bufferedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return (bufferedImage);
    }
}
