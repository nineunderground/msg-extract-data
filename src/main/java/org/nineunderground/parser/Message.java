/*******************************************************************************
 * Copyright (C) 2018 inaki
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
/*
 * msgparser - http://auxilii.com/msgparser
 * Copyright (C) 2007  Roman Kurmanowytsch
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package org.nineunderground.parser;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hmef.CompressedRTF;
import org.nineunderground.parser.attachment.Attachment;
import org.nineunderground.parser.attachment.FileAttachment;
import org.nineunderground.parser.attachment.MsgAttachment;
import org.nineunderground.parser.rtf.RTF2HTMLConverter;
import org.nineunderground.parser.rtf.SimpleRTF2HTMLConverter;

/**
 * The Class Message.
 *
 * @author inaki
 *
 */
public class Message {
	protected static final Logger logger = Logger.getLogger(Message.class.getName());

	/**
	 * Parses the message date from the mail headers.
	 *
	 * @param headers
	 *            The headers in a single String object
	 * @return The Date object or null, if no valid Date: has been found
	 */
	public static Date getDateFromHeaders(String headers) {
		if (headers == null)
			return null;
		final String[] headerLines = headers.split("\n");
		for (final String headerLine : headerLines) {
			if (headerLine.toLowerCase().startsWith("date:")) {
				final String dateValue = headerLine.substring("Date:".length()).trim();
				final SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

				// There may be multiple Date: headers. Let's take the first one
				// that can be parsed.

				try {
					final Date date = formatter.parse(dateValue);

					if (date != null)
						return date;
				} catch (final Exception e) {
					logger.log(Level.FINEST, "Could not parse date " + dateValue, e);
				}
			}
		}
		return null;
	}
	/**
	 * Parses the sender's email address from the mail headers.
	 *
	 * @param The
	 *            headers in a single String object
	 * @return The sender's email or null if nothing was found.
	 */
	protected static String getFromEmailFromHeaders(String headers) {
		String fromEmail = null;
		if (headers != null) {
			final String[] headerLines = headers.split("\n");
			for (final String headerLine : headerLines) {
				if (headerLine.toUpperCase().startsWith("FROM: ")) {
					final String[] tokens = headerLine.split(" ");
					for (final String t : tokens) {
						if (t.contains("@")) {
							fromEmail = t;
							fromEmail = fromEmail.replaceAll("[<>]", "");
							fromEmail = fromEmail.trim();
							break;
						}
					}
				}
				if (fromEmail != null) {
					break;
				}
			}
		}

		return fromEmail;
	}
	/**
	 * @param date
	 *            The date string to be converted (e.g.: 'Mon Jul 23 15:43:12
	 *            CEST 2012')
	 * @return A {@link Date} object representing the given date string.
	 */
	protected static Date parseDateString(String date) {
		// in order to parse the date we try using the US locale before we
		// fall back to the default locale.
		final List<SimpleDateFormat> sdfList = new ArrayList<>(2);
		sdfList.add(new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US));
		sdfList.add(new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy"));

		Date d = null;
		for (final SimpleDateFormat sdf : sdfList) {
			try {
				d = sdf.parse(date);
				if (d != null) {
					break;
				}
			} catch (final ParseException e) {
				logger.log(Level.FINEST, "Unexpected date format for date " + date);
			}
		}
		return d;
	}
	/**
	 * The message class as defined in the .msg file.
	 */
	protected String messageClass = "IPM.Note";
	/**
	 * The message Id.
	 */
	protected String messageId = null;
	/**
	 * The address part of From: mail address.
	 */
	protected String fromEmail = null;
	/**
	 * The name part of the From: mail address
	 */
	protected String fromName = null;
	/**
	 * The address part of To: mail address.
	 */
	protected String toEmail = null;
	/**
	 * The name part of the To: mail address
	 */
	protected String toName = null;
	/**
	 * The mail's subject.
	 */
	protected String subject = null;
	/**
	 * The normalized body text.
	 */
	protected String bodyText = null;

	/**
	 * The displayed To: field
	 */
	protected String displayTo = null;

	/**
	 * The displayed Cc: field
	 */
	protected String displayCc = null;

	/**
	 * The displayed Bcc: field
	 */
	protected String displayBcc = null;
	/**
	 * The body in RTF format (if available)
	 */
	protected String bodyRTF = null;

	/**
	 * The body in HTML format (if available)
	 */
	protected String bodyHTML = null;

	/**
	 * The body in HTML format (converted from RTF)
	 */
	protected String convertedBodyHTML = null;

	/**
	 * Email headers (if available)
	 */
	protected String headers = null;

	/**
	 * Email Date
	 */
	protected Date date = null;
	/**
	 * Client Submit Time
	 */
	protected Date clientSubmitTime = null;
	protected Date creationDate = null;
	protected Date lastModificationDate = null;
	/**
	 * A list of all attachments (both {@link FileAttachment} and
	 * {@link MsgAttachment}).
	 */
	protected List<Attachment> attachments = new ArrayList<>();

	/**
	 * Contains all properties that are not covered by the special properties.
	 */
	protected Map<Integer, Object> properties = new TreeMap<>();

	/**
	 * A list containing all recipients for this message (which can be set in
	 * the 'to:', 'cc:' and 'bcc:' field, respectively).
	 */
	protected List<RecipientEntry> recipients = new ArrayList<>();

	protected RTF2HTMLConverter rtf2htmlConverter;

	public Message() {
		this.rtf2htmlConverter = new SimpleRTF2HTMLConverter();
	}

	public Message(RTF2HTMLConverter rtf2htmlConverter) {
		if (rtf2htmlConverter != null) {
			this.rtf2htmlConverter = rtf2htmlConverter;
		} else {
			this.rtf2htmlConverter = new SimpleRTF2HTMLConverter();
		}
	}

	public void addAttachment(Attachment attachment) {
		this.attachments.add(attachment);
	}

	public void addRecipient(RecipientEntry recipient) {
		this.recipients.add(recipient);
		if (toEmail == null) {
			setToEmail(recipient.getToEmail());
		}
		if (toName == null) {
			setToName(recipient.getToName());
		}
	}

	/**
	 * Checks if the correct recipient's addresses are set.
	 */
	protected void checkToRecipient() {
		final RecipientEntry toRecipient = getToRecipient();
		if (toRecipient != null) {
			setToEmail(toRecipient.getToEmail(), true);
			setToName(toRecipient.getToName());
			recipients.remove(toRecipient);
			recipients.add(0, toRecipient);
		}
	}

	/**
	 * Converts a given integer to hex notation without leading '0x'.
	 *
	 * @param propCode
	 *            The value to be formatted.
	 * @return A hex formatted number.
	 */
	public String convertToHex(Integer propCode) {
		return String.format("%04x", propCode);
	}

	protected String convertValueToString(Object value) {
		if (value == null)
			return null;
		if (value instanceof String)
			return (String) value;
		else if (value instanceof byte[]) {
			try {
				return new String((byte[]) value, "CP1252");
			} catch (final UnsupportedEncodingException e) {
				logger.log(Level.FINE, "Unsupported encoding!", e);
				return null;
			}
		} else {
			logger.log(Level.FINE, "Unexpected body class: " + value.getClass().getName());
			return value.toString();
		}
	}

	/**
	 * Convenience method for creating an email address expression (including
	 * the name, the address, or both).
	 *
	 * @param mail
	 *            The mail address.
	 * @param name
	 *            The name part of the address.
	 * @return A combination of the name and address.
	 */
	public String createMailString(String mail, String name) {
		if (mail == null && name == null)
			return null;
		if (name == null)
			return mail;
		if (mail == null)
			return name;
		if (mail.equalsIgnoreCase(name))
			return mail;
		return "\"" + name + "\" <" + mail + ">";
	}

	/**
	 * Decompresses compressed RTF data.
	 *
	 * @param value
	 *            Data to be decompressed.
	 * @return A byte array representing the decompressed data.
	 */
	protected byte[] decompressRtfBytes(byte[] value) {
		byte[] decompressed = null;
		if (value != null) {
			try {
				final CompressedRTF crtf = new CompressedRTF();
				decompressed = crtf.decompress(new ByteArrayInputStream(value));
			} catch (final Exception e) {
				logger.log(Level.FINEST, "Could not decompress RTF data", e);
			}
		}
		return decompressed;
	}

	/**
	 * @return the attachments
	 */
	public List<Attachment> getAttachments() {
		return attachments;
	}

	/**
	 * Retrieves a list of {@link RecipientEntry} objects that represent the BCC
	 * recipients of the message.
	 *
	 * @return the BCC recipients of the message.
	 */
	public List<RecipientEntry> getBccRecipients() {
		final List<RecipientEntry> recipients = new ArrayList<>();
		final String recipientKey = getDisplayBcc().trim();
		for (final RecipientEntry entry : recipients) {
			final String name = entry.getToName().trim();
			if (recipientKey.contains(name)) {
				recipients.add(entry);
			}
		}
		return recipients;
	}

	/**
	 * @return the bodyHTML
	 */
	public String getBodyHTML() {
		return bodyHTML;
	}

	/**
	 * @return the bodyRTF
	 */
	public String getBodyRTF() {
		return bodyRTF;
	}

	/**
	 * @return the bodyText
	 */
	public String getBodyText() {
		return bodyText;
	}

	/**
	 * Retrieves a list of {@link RecipientEntry} objects that represent the CC
	 * recipients of the message.
	 *
	 * @return the CC recipients of the message.
	 */
	public List<RecipientEntry> getCcRecipients() {
		final List<RecipientEntry> recipients = new ArrayList<>();
		final String recipientKey = getDisplayCc().trim();
		for (final RecipientEntry entry : recipients) {
			final String name = entry.getToName().trim();
			if (recipientKey.contains(name)) {
				recipients.add(entry);
			}
		}
		return recipients;
	}

	public Date getClientSubmitTime() {
		return clientSubmitTime;
	}

	/**
	 * @return the convertedBodyHTML which is basically the result of an
	 *         RTF-HTML conversion
	 */
	public String getConvertedBodyHTML() {
		return convertedBodyHTML;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	public String getDisplayBcc() {
		return displayBcc;
	}

	public String getDisplayCc() {
		return displayCc;
	}

	public String getDisplayTo() {
		return displayTo;
	}

	/**
	 * @return the fromEmail
	 */
	public String getFromEmail() {
		return fromEmail;
	}

	/**
	 * @return the fromName
	 */
	public String getFromName() {
		return fromName;
	}

	/**
	 * @return the headers
	 */
	public String getHeaders() {
		return headers;
	}

	public Date getLastModificationDate() {
		return lastModificationDate;
	}

	/**
	 * @return the messageClass
	 */
	public String getMessageClass() {
		return messageClass;
	}

	/**
	 * @return the messageId
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * This method should no longer be used due to the fact that message
	 * properties are now stored with their keys being represented as integers.
	 *
	 * @return All available keys properties have been found for.
	 */
	@Deprecated
	public Set<String> getProperties() {
		return getPropertiesAsHex();
	}

	/**
	 * This method provides a convenient way of retrieving property keys for all
	 * guys that like to stick to hex values. <br>
	 * Note that this method includes parsing of string values to integers which
	 * will be less efficient than using {@link #getPropertyCodes()}.
	 *
	 * @return All available keys properties have been found for.
	 */
	public Set<String> getPropertiesAsHex() {
		final Set<Integer> keySet = this.properties.keySet();
		final Set<String> result = new HashSet<>();
		for (final Integer k : keySet) {
			final String s = convertToHex(k);
			result.add(s);
		}

		return result;
	}

	/**
	 * This method should no longer be used due to the fact that message
	 * properties are now stored with their keys being represented as integers.
	 * <br>
	 * <br>
	 * Please refer to {@link #getPropertyCodes()} for dealing with integer
	 * based keys.
	 *
	 * @return The value for the requested property.
	 */
	@Deprecated
	public Object getProperty(String name) {
		return getPropertyFromHex(name);
	}

	/**
	 * This method returns a list of all available properties.
	 *
	 * @return All available keys properties have been found for.
	 */
	public Set<Integer> getPropertyCodes() {
		return this.properties.keySet();
	}

	/**
	 * This method provides a convenient way of retrieving properties for all
	 * guys that like to stick to hex values. <br>
	 * Note that this method includes parsing of string values to integers which
	 * will be less efficient than using {@link #getPropertyValue(Integer)}.
	 *
	 * @param name
	 *            The hex notation of the property to be retrieved.
	 * @return The value for the requested property.
	 */
	public Object getPropertyFromHex(String name) {
		Integer i = -1;
		try {
			i = Integer.parseInt(name, 16);
		} catch (final NumberFormatException e) {
			logger.log(Level.FINEST, "Could not parse integer: " + name);
		}
		return getPropertyValue(i);
	}

	/**
	 * Generates a string that can be used to debug the properties of the msg.
	 *
	 * @return A property listing holding hexadecimal, decimal and string
	 *         representations of properties and their values.
	 */
	public String getPropertyListing() {
		final StringBuffer sb = new StringBuffer();
		for (final Integer propCode : getPropertyCodes()) {
			final Object value = getPropertyValue(propCode);
			final String hexCode = "0x" + convertToHex(propCode);
			sb.append(hexCode + " / " + propCode);
			sb.append(": " + value.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * This method retrieves the value for a specific property. <br>
	 * Please refer to {@link #getPropertyValue(Integer)} for dealing with
	 * integer based keys. <br>
	 * <b>NOTE:</b> You can also use fields defined within {@link MAPIProp} to
	 * easily read certain properties.
	 *
	 * @param code
	 *            The key for the property to be retrieved.
	 * @return The value of the specified property.
	 */
	public Object getPropertyValue(Integer code) {
		return this.properties.get(code);
	}

	/**
	 * @return the recipients
	 */
	public List<RecipientEntry> getRecipients() {
		return recipients;
	}
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * @return the toEmail
	 */
	public String getToEmail() {
		return toEmail;
	}
	/**
	 * @return the toName
	 */
	public String getToName() {
		return toName;
	}

	/**
	 * Retrieves the {@link RecipientEntry} object that represents the TO
	 * recipient of the message.
	 *
	 * @return the TO recipient of the message or null in case no
	 *         {@link RecipientEntry} was found.
	 */
	public RecipientEntry getToRecipient() {
		if (getDisplayTo() != null) {
			final String recipientKey = getDisplayTo().trim();
			for (final RecipientEntry entry : recipients) {
				final String name = entry.getToName().trim();
				if (recipientKey.contains(name))
					return entry;
			}
		}
		return null;
	}

	/**
	 * @param attachments
	 *            the attachments to set
	 */
	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	/**
	 * @param bodyHTML
	 *            the bodyHTML to set
	 */
	public void setBodyHTML(String bodyHTML) {
		setBodyHTML(bodyHTML, false);
	}
	/**
	 * @param bodyHTML
	 *            the bodyHTML to set
	 * @param force
	 *            forces overwriting of the field if already set
	 */
	protected void setBodyHTML(String bodyToSet, boolean force) {
		if ((force || this.bodyHTML == null) && bodyToSet != null) {
			if (!(this.bodyHTML != null && this.bodyHTML.length() > bodyToSet.length())) {
				// only if the new body to be set is bigger than the current one
				// thus the short one is most probably wrong
				this.bodyHTML = bodyToSet;
			}
		}
	}

	/**
	 * @param bodyRTF
	 *            the bodyRTF to set
	 */
	public void setBodyRTF(Object bodyRTF) {
		// we simply try to decompress the RTF data
		// if it's not compressed, the utils class
		// is able to detect this anyway
		if (this.bodyRTF == null && bodyRTF != null) {
			if (bodyRTF instanceof byte[]) {
				final byte[] decompressedBytes = decompressRtfBytes((byte[]) bodyRTF);
				if (decompressedBytes != null) {
					this.bodyRTF = new String(decompressedBytes);
					try {
						setConvertedBodyHTML(rtf2htmlConverter.rtf2html(this.bodyRTF));
					} catch (final Exception e) {
						logger.log(Level.WARNING, "Could not convert RTF body to HTML.", e);
					}
				}
			} else {
				logger.log(Level.FINEST, "Unexpected data type " + bodyRTF.getClass());
			}
		}
	}

	/**
	 * @param bodyText
	 *            the bodyText to set
	 */
	public void setBodyText(String bodyText) {
		if (this.bodyText == null && bodyText != null) {
			this.bodyText = bodyText;
		}
	}

	public void setClientSubmitTime(String value) {
		if (value != null) {
			final Date d = Message.parseDateString(value);
			if (d != null) {
				this.clientSubmitTime = d;
			}
		}
	}

	/**
	 * @param convertedBodyHTML
	 *            the bodyHTML to set
	 */
	public void setConvertedBodyHTML(String convertedBodyHTML) {
		this.convertedBodyHTML = convertedBodyHTML;
	}
	public void setCreationDate(String value) {
		if (value != null) {
			final Date d = Message.parseDateString(value);
			if (d != null) {
				this.creationDate = d;
				setDate(d);
			}
		}
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	public void setDisplayBcc(String displayBcc) {
		if (displayBcc != null) {
			this.displayBcc = displayBcc;
		}
	}
	public void setDisplayCc(String displayCc) {
		if (displayCc != null) {
			this.displayCc = displayCc;
		}
	}

	public void setDisplayTo(String displayTo) {
		if (displayTo != null) {
			this.displayTo = displayTo;
		}
	}

	/**
	 * @param fromEmail
	 *            the fromEmail to set
	 */
	public void setFromEmail(String fromEmail) {
		if (fromEmail != null && fromEmail.contains("@")) {
			setFromEmail(fromEmail, true);
		} else {
			setFromEmail(fromEmail, false);
		}
	}

	/**
	 * @param fromEmail
	 *            the fromEmail to set
	 * @param force
	 *            forces overwriting of the field if already set
	 */
	public void setFromEmail(String fromEmail, boolean force) {
		if ((force || this.fromEmail == null) && fromEmail != null && fromEmail.contains("@")) {
			this.fromEmail = fromEmail;
		}
	}

	/**
	 * @param fromName
	 *            the fromName to set
	 */
	public void setFromName(String fromName) {
		if (fromName != null) {
			this.fromName = fromName;
		}
	}

	/**
	 * @param headers
	 *            the headers to set
	 */
	public void setHeaders(String headers) {
		if (headers != null) {
			this.headers = headers;
			// try to parse the date from the headers
			final Date d = Message.getDateFromHeaders(headers);
			if (d != null) {
				this.setDate(d);
			}
			final String s = Message.getFromEmailFromHeaders(headers);
			if (s != null) {
				this.setFromEmail(s);
			}
		}
	}

	public void setLastModificationDate(String value) {
		if (value != null) {
			final Date d = Message.parseDateString(value);
			if (d != null) {
				this.lastModificationDate = d;
			}
		}
	}

	/**
	 * @param messageClass
	 *            the messageClass to set
	 */
	public void setMessageClass(String messageClass) {
		if (messageClass != null) {
			this.messageClass = messageClass;
		}
	}

	/**
	 * @param messageId
	 *            the messageId to set
	 */
	public void setMessageId(String messageId) {
		if (messageId != null) {
			this.messageId = messageId;
		}
	}
	/**
	 * Sets the name/value pair in the {@link #properties} map. Some properties
	 * are put into special attributes (e.g., {@link #toEmail} when the property
	 * name is '0076').
	 *
	 * @param name
	 *            The property name (i.e., the class of the document entry).
	 * @param value
	 *            The value of the field.
	 * @throws ClassCastException
	 *             Thrown if the detected data type does not match the expected
	 *             data type.
	 */
	public void setProperty(MessageProperty msgProp) throws ClassCastException {
		final String name = msgProp.getClazz();
		final Object value = msgProp.getData();

		if (name == null || value == null)
			return;

		// Most fields expect a String representation of the value
		final String stringValue = this.convertValueToString(value);

		int mapiClass = -1;
		try {
			mapiClass = Integer.parseInt(name, 16);
		} catch (final NumberFormatException e) {
			logger.log(Level.FINEST, "Unexpected type: " + name);
		}

		switch (mapiClass) {
			case 0x1a : // MESSAGE CLASS
				this.setMessageClass(stringValue);
				break;
			case 0x1035 :
				this.setMessageId(stringValue);
				break;
			case 0x37 : // SUBJECT
			case 0xe1d : // NORMALIZED SUBJECT
				this.setSubject(stringValue);
				break;
			case 0xc1f : // SENDER EMAIL ADDRESS
			case 0x65 : // SENT REPRESENTING EMAIL ADDRESS
			case 0x3ffa : // LAST MODIFIER NAME
			case 0x800d :
			case 0x8008 :
				this.setFromEmail(stringValue);
				break;
			case 0x42 : // SENT REPRESENTING NAME
				this.setFromName(stringValue);
				break;
			case 0x76 : // RECEIVED BY EMAIL ADDRESS
				this.setToEmail(stringValue, true);
				break;
			case 0x8000 :
				this.setToEmail(stringValue);
				break;
			case 0x3001 : // DISPLAY NAME
				this.setToName(stringValue);
				break;
			case 0xe04 : // DISPLAY TO
				this.setDisplayTo(stringValue);
				break;
			case 0xe03 : // DISPLAY CC
				this.setDisplayCc(stringValue);
				break;
			case 0xe02 : // DISPLAY BCC
				this.setDisplayBcc(stringValue);
				break;
			case 0x1013 : // HTML
				this.setBodyHTML(stringValue, true);
				break;
			case 0x1000 : // BODY
				this.setBodyText(stringValue);
				break;
			case 0x1009 : // RTF COMPRESSED
				this.setBodyRTF(value);
				break;
			case 0x7d : // TRANSPORT MESSAGE HEADERS
				this.setHeaders(stringValue);
				break;
			case 0x3007 : // CREATION TIME
				this.setCreationDate(stringValue);
				break;
			case 0x3008 : // LAST MODIFICATION TIME
				this.setLastModificationDate(stringValue);
				break;
			case 0x39 : // CLIENT SUBMIT TIME
				this.setClientSubmitTime(stringValue);
				break;
		}

		// save all properties (incl. those identified above)
		this.properties.put(mapiClass, value);

		checkToRecipient();

		// other possible values (some are duplicates)
		// 0044: recv name
		// 004d: author
		// 0050: reply
		// 005a: sender
		// 0065: sent email
		// 0076: received email
		// 0078: repr. email
		// 0c1a: sender name
		// 0e04: to
		// 0e1d: subject normalized
		// 1046: sender email
		// 3003: email address
		// 1008 rtf sync
	}

	/**
	 * @param recipients
	 *            the recipients to set
	 */
	public void setRecipients(List<RecipientEntry> recipients) {
		this.recipients = recipients;
	}

	/**
	 * @param subject
	 *            the subject to set
	 */
	public void setSubject(String subject) {
		if (subject != null) {
			this.subject = subject;
		}
	}

	/**
	 * @param toEmail
	 *            the toEmail to set
	 */
	public void setToEmail(String toEmail) {
		setToEmail(toEmail, false);
	}

	/**
	 * @param toEmail
	 *            the toEmail to set
	 * @param force
	 *            forces overwriting of the field if already set
	 */
	public void setToEmail(String toEmail, boolean force) {
		if ((force || this.toEmail == null) && toEmail != null && toEmail.contains("@")) {
			this.toEmail = toEmail;
		}
	}

	/**
	 * @param toName
	 *            the toName to set
	 */
	public void setToName(String toName) {
		if (toName != null) {
			toName = toName.trim();
			this.toName = toName;
		}
	}

	/**
	 * Provides all information of this message object.
	 *
	 * @return The full message information.
	 */
	public String toLongString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("From: " + this.createMailString(this.fromEmail, this.fromName) + "\n");
		sb.append("To: " + this.createMailString(this.toEmail, this.toName) + "\n");
		if (this.date != null) {
			final SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
			sb.append("Date: " + formatter.format(this.date) + "\n");
		}
		if (this.subject != null) {
			sb.append("Subject: " + this.subject + "\n");
		}
		sb.append("\n");
		if (this.bodyText != null) {
			sb.append(this.bodyText);
		}
		if (this.attachments.size() > 0) {
			sb.append("\n");
			sb.append("" + this.attachments.size() + " attachments.\n");
			for (final Attachment att : this.attachments) {
				sb.append(att.toString() + "\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Provides a short representation of this .msg object.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("From: " + this.createMailString(this.fromEmail, this.fromName) + "\n");
		sb.append("To: " + this.createMailString(this.toEmail, this.toName) + "\n");
		if (this.date != null) {
			final SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);
			sb.append("Date: " + formatter.format(this.date) + "\n");
		}
		if (this.subject != null) {
			sb.append("Subject: " + this.subject + "\n");
		}
		sb.append("" + this.attachments.size() + " attachments.");
		return sb.toString();
	}
}
