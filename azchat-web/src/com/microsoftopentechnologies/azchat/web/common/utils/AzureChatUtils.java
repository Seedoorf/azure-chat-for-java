/*
 Copyright 2015 Microsoft Open Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.microsoftopentechnologies.azchat.web.common.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoftopentechnologies.azchat.web.common.exceptions.AzureChatException;
import com.microsoftopentechnologies.azchat.web.data.beans.BaseBean;
import com.microsoftopentechnologies.azchat.web.data.beans.ErrorBean;
import com.microsoftopentechnologies.azchat.web.data.beans.ErrorListBean;

/**
 * This class contains common utility methods.
 * 
 * @author Dnyaneshwar_Pawar
 *
 */
public class AzureChatUtils {

	private static final Logger LOGGER = LogManager
			.getLogger(AzureChatUtils.class);
	private static final Properties PROPERTIES = new Properties();
	private static Connection connection = null;
	private static final String TOKEN_SEPARATOR = ";";

	/**
	 * Gets XML DOM object from string data
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static Document getDocument(String data) throws Exception {
		LOGGER.info("[AZChatUtils][getDocument] start");
		Document document = null;
		DocumentBuilder documentBuilder;

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setIgnoringElementContentWhitespace(true);
		documentBuilder = documentBuilderFactory.newDocumentBuilder();
		byte[] samlToken = data.getBytes("UTF-8");

		ByteArrayInputStream bis = new ByteArrayInputStream(samlToken);
		document = documentBuilder.parse(bis);
		document.getDocumentElement().normalize();
		LOGGER.info("[AZChatUtils][getDocument] end");
		return document;
	}

	/**
	 * Retrieves nameID assetion from ACS token
	 * @param xPath
	 * @param assertionDoc
	 * @return
	 * @throws Exception
	 */
	public static String getNameIDFromAssertion(XPath xPath,
			Document assertionDoc) throws Exception {
		LOGGER.info("[AZChatUtils][getNameIDFromAssertion] start");
		Node nameIDNode = (Node) xPath.evaluate(
				AzureChatConstants.NAME_ID_NODE, assertionDoc,
				XPathConstants.NODE);
		LOGGER.info("[AZChatUtils][getNameIDFromAssertion] end");
		return nameIDNode.getTextContent();
	}

	/**
	 * Retrieves user attribute details from ACS token.
	 * @param xPath
	 * @param assertionDoc
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getUserAttributeDetails(XPath xPath,
			Document assertionDoc) throws Exception {
		LOGGER.info("[AZChatUtils][getUserAttributeDetails] start");
		NodeList userAttributes = (NodeList) xPath.evaluate(
				AzureChatConstants.ATTRIBUTE_NODE, assertionDoc,
				XPathConstants.NODESET);
		Map<String, String> claimMap = new HashMap<String, String>();

		for (int i = 0; i < userAttributes.getLength(); i++) {
			Node attribute = userAttributes.item(i);
			String attributeName = attribute.getAttributes()
					.getNamedItem(AzureChatConstants.NAME_ATTRIBUTE)
					.getNodeValue();
			String claimName = attributeName.substring(attributeName
					.lastIndexOf(AzureChatConstants.CONSTANT_BACK_SLASH) + 1);

			if (AzureChatConstants.ATTR_IDENTITY_PROVIDER
					.equalsIgnoreCase(claimName)) {
				claimMap.put(AzureChatConstants.ATTR_IDENTITY_PROVIDER,
						attribute.getFirstChild().getTextContent());
			} else if (AzureChatConstants.ATTR_EMAIL_ADDRESS
					.equalsIgnoreCase(claimName)) {
				claimMap.put(AzureChatConstants.ATTR_EMAIL_ADDRESS, attribute
						.getFirstChild().getTextContent());
			} else if (AzureChatConstants.ATTR_NAME.equalsIgnoreCase(claimName)) {
				claimMap.put(AzureChatConstants.ATTR_NAME, attribute
						.getFirstChild().getTextContent());
			}

		}
		LOGGER.info("[AZChatUtils][getUserAttributeDetails] start");
		return claimMap;
	}

	/**
	 * This method loads project properties.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static Properties getProperties() throws AzureChatException {
		InputStream inputStream = AzureChatUtils.class.getClassLoader()
				.getResourceAsStream(AzureChatConstants.RESOURCE_PROP_FILE);
		InputStream inputStream1 = AzureChatUtils.class.getClassLoader()
				.getResourceAsStream(AzureChatConstants.MSG_PROP_FILE);
		
		if (inputStream != null) {
			try {
				PROPERTIES.load(inputStream);
				PROPERTIES.load(inputStream1);
			} catch (IOException e) {
				LOGGER.error("Exception Occurred while reading property file"
						+ AzureChatConstants.RESOURCE_PROP_FILE);
			} finally {
				try {
					inputStream.close();
					inputStream1.close();
				} catch (IOException e) {
					LOGGER.error("IOException Occurred while trying to close input stream after property load."
							+ e.getMessage());
					throw new AzureChatException(
							"IOException Occurred while trying to close input stream after property load."
									+ e.getMessage());
				}

			}
		}
		return PROPERTIES;
	}

	/**
	 * Fetches property value for given property name
	 * 
	 * @param propName
	 * @return
	 * @throws IOException
	 */
	public static String getProperty(String propName) throws AzureChatException {
		return null != getProperties().get(propName) ? (String) getProperties()
				.get(propName) : null;
	}

	/**
	 * This method checks for empty or null collections.
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(Collection<?> value) {
		return value == null || value.isEmpty();
	}

	/**
	 * This method checks for if map is empty.
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(Map<?, ?> value) {
		return value == null || value.isEmpty();
	}

	/**
	 * This method checks for empty or null string.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmptyOrNull(String str) {
		if (str == null || str.isEmpty()) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns Azure SQL database connection object.
	 * 
	 * @param connectionString
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws AzureChatException
	 */
	public static Connection getConnection(String connectionString)
			throws ClassNotFoundException, SQLException, AzureChatException {
		Class.forName(AzureChatUtils.getProperty(AzureChatConstants.DRIVER));
		connection = DriverManager.getConnection(connectionString);
		return connection;
	}

	/**
	 * This method prepare connection string for the Azure SQL Database.
	 * 
	 * @return
	 * @throws AzureChatException
	 */
	public static String buildConnectionString() throws AzureChatException {
		String timeout = AzureChatUtils
				.getProperty(AzureChatConstants.DB_PROP_KEY_LOG_IN_TIMEOUT);

		if (AzureChatUtils.isEmptyOrNull(timeout)) {
			timeout = AzureChatConstants.DB_PROP_TIMEOUT_VAL;
		}

		StringBuilder connectionString = new StringBuilder(
				AzureChatUtils.getProperty(AzureChatConstants.DB_PROP_KEY_URL)
						+ TOKEN_SEPARATOR
						+ "database="
						+ AzureChatUtils
								.getProperty(AzureChatConstants.DB_PROP_KEY_DATABASE)
						+ TOKEN_SEPARATOR
						+ "user="
						+ AzureChatUtils
								.getProperty(AzureChatConstants.DB_PROP_KEY_USER)
						+ TOKEN_SEPARATOR
						+ "password="
						+ AzureChatUtils
								.getProperty(AzureChatConstants.DB_PROP_KEY_PASSWORD)
						+ TOKEN_SEPARATOR
						+ "encrypt="
						+ AzureChatUtils
								.getProperty(AzureChatConstants.DB_PROP_KEY_ENCRYPT)
						+ TOKEN_SEPARATOR
						+ "hostNameInCertificate="
						+ AzureChatUtils
								.getProperty(AzureChatConstants.DB_PROP_KEY_HOST_NAME_IN_CERT)
						+ TOKEN_SEPARATOR + "loginTimeout=" + timeout
						+ TOKEN_SEPARATOR);
		return connectionString.toString();
	}

	/**
	 * This method converts the bytes value into the megabytes value.
	 * 
	 * @param bytes
	 * @return
	 */
	public static Long getMegaBytes(Long bytes) {
		if (null != bytes) {
			return bytes / 1000000;
		}
		return null;
	}

	/**
	 * This method extracts the digits from the provided string.
	 * 
	 * @param str
	 * @return
	 */
	public static Long getNumbers(String str) {
		Long num = null;
		if (null != str) {
			str = str.replaceAll("[^0-9]", "");
			num = Long.parseLong(str);
		}
		return num;
	}
	
	/**
	 * This method populates the error in input bean.
	 * @param baseBean
	 * @param excpCode
	 * @param excpMsg
	 */
	public static void populateErrors(BaseBean baseBean,String excpCode,String excpMsg){
		ErrorListBean errorListBean=new ErrorListBean();
		List<ErrorBean> errorBeanList=new ArrayList<ErrorBean>();
		ErrorBean errorBean=new ErrorBean();
		errorBean.setExcpMsg(excpMsg);
		errorListBean.setExcpCode(excpCode);
		errorBeanList.add(errorBean);
		errorListBean.setErrorList(errorBeanList);
		baseBean.setErrorList(errorListBean);
	}
}
