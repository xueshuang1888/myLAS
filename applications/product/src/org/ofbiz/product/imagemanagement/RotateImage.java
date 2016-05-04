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
package org.ofbiz.product.imagemanagement;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jdom.JDOMException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;


public class RotateImage {

    public static final String module = RotateImage.class.getName();
    public static final String resource = "ProductErrorUiLabels";

    public static Map<String, Object> imageRotate(DispatchContext dctx, Map<String, ? extends Object> context)
    throws IOException, JDOMException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String nameOfThumb = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.nameofthumbnail", delegator), context);
        
        String productId = (String) context.get("productId");
        String imageName = (String) context.get("imageName");
        String angle = (String) context.get("angle");
        
        if (UtilValidate.isNotEmpty(imageName)) {
            Map<String, Object> contentCtx = new HashMap<String, Object>();
            contentCtx.put("contentTypeId", "DOCUMENT");
            contentCtx.put("userLogin", userLogin);
            Map<String, Object> contentResult = new HashMap<String, Object>();
            try {
                contentResult = dispatcher.runSync("createContent", contentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            
            Map<String, Object> contentThumb = new HashMap<String, Object>();
            contentThumb.put("contentTypeId", "DOCUMENT");
            contentThumb.put("userLogin", userLogin);
            Map<String, Object> contentThumbResult = new HashMap<String, Object>();
            try {
                contentThumbResult = dispatcher.runSync("createContent", contentThumb);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            
            String contentIdThumb = (String) contentThumbResult.get("contentId");
            String contentId = (String) contentResult.get("contentId");
            String filenameToUse = (String) contentResult.get("contentId") + ".jpg";
            String filenameTouseThumb = (String) contentResult.get("contentId") + nameOfThumb + ".jpg";
            
            String imageServerPath = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.path", delegator), context);
            String imageServerUrl = FlexibleStringExpander.expandString(EntityUtilProperties.getPropertyValue("catalog", "image.management.url", delegator), context);
            BufferedImage bufImg = ImageIO.read(new File(imageServerPath + "/" + productId + "/" + imageName));
            
            int bufImgType;
            if (BufferedImage.TYPE_CUSTOM == bufImg.getType()) {
                bufImgType = BufferedImage.TYPE_INT_ARGB_PRE;
            } else {
                bufImgType = bufImg.getType();
            }
            
            int w = bufImg.getWidth(null);
            int h = bufImg.getHeight(null);
            BufferedImage bufNewImg = new BufferedImage(w, h, bufImgType);
            Graphics2D g = bufNewImg.createGraphics();
            g.rotate(Math.toRadians(Double.parseDouble(angle)), w/2, h/2);
            g.drawImage(bufImg,0,0,null);
            g.dispose();
            
            String mimeType = imageName.substring(imageName.lastIndexOf(".") + 1);
            ImageIO.write(bufNewImg, mimeType, new File(imageServerPath + "/" + productId + "/" + filenameToUse));
            
            double imgHeight = bufNewImg.getHeight();
            double imgWidth = bufNewImg.getWidth();
            
            Map<String, Object> resultResize = ImageManagementServices.resizeImageThumbnail(bufNewImg, imgHeight, imgWidth);
            ImageIO.write((RenderedImage) resultResize.get("bufferedImage"), mimeType, new File(imageServerPath + "/" + productId + "/" + filenameTouseThumb));
            
            String imageUrlResource = imageServerUrl + "/" + productId + "/" + filenameToUse;
            String imageUrlThumb = imageServerUrl + "/" + productId + "/" + filenameTouseThumb;
            
            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameToUse, imageUrlResource, contentId, "image/jpeg");
            ImageManagementServices.createContentAndDataResource(dctx, userLogin, filenameTouseThumb, imageUrlThumb, contentIdThumb, "image/jpeg");
            
            Map<String, Object> createContentAssocMap = new HashMap<String, Object>();
            createContentAssocMap.put("contentAssocTypeId", "IMAGE_THUMBNAIL");
            createContentAssocMap.put("contentId", contentId);
            createContentAssocMap.put("contentIdTo", contentIdThumb);
            createContentAssocMap.put("userLogin", userLogin);
            createContentAssocMap.put("mapKey", "100");
            try {
                dispatcher.runSync("createContentAssoc", createContentAssocMap);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            
            Map<String, Object> productContentCtx = new HashMap<String, Object>();
            productContentCtx.put("productId", productId);
            productContentCtx.put("productContentTypeId", "IMAGE");
            productContentCtx.put("fromDate", UtilDateTime.nowTimestamp());
            productContentCtx.put("userLogin", userLogin);
            productContentCtx.put("contentId", contentId);
            productContentCtx.put("statusId", "IM_PENDING");
            try {
                dispatcher.runSync("createProductContent", productContentCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
            
            Map<String, Object> contentApprovalCtx = new HashMap<String, Object>();
            contentApprovalCtx.put("contentId", contentId);
            contentApprovalCtx.put("userLogin", userLogin);
            try {
                dispatcher.runSync("createImageContentApproval", contentApprovalCtx);
            } catch (GenericServiceException e) {
                Debug.logError(e, module);
                return ServiceUtil.returnError(e.getMessage());
            }
        } else {
            String errMsg = "Please select Image.";
            Debug.logFatal(errMsg, module);
            return ServiceUtil.returnError(errMsg);
        }
        String successMsg = "Rotated image successfully.";
        Map<String, Object> result = ServiceUtil.returnSuccess(successMsg);
        return result;
    }
}
