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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.nineunderground.parser.attachment.Attachment;
import org.nineunderground.parser.attachment.FileAttachment;
import org.nineunderground.parser.attachment.MsgAttachment;
import org.nineunderground.parser.rtf.RTF2HTMLConverter;
import org.nineunderground.parser.rtf.SimpleRTF2HTMLConverter;

/**
 * The Class MsgParser.
 *
 * @author inaki
 */
public class MsgParser {

	/** The Constant logger. */
	protected static final Logger logger = Logger.getLogger(MsgParser.class.getName());
	protected static final String propsKey = "__properties_version1.0";
	protected static final String propertyStreamPrefix = "__substg1.0_";
	protected RTF2HTMLConverter rtf2htmlConverter = new SimpleRTF2HTMLConverter();

	/**
	 * Empty constructor.
	 */
	public MsgParser() {
	}

	/**
	 * Analyzes the {@link DocumentEntry} and returns a {@link FieldInformation}
	 * object containing the class (the field name, so to say) and type of the
	 * entry.
	 *
	 * @param de
	 *            The {@link DocumentEntry} that should be examined.
	 * @return A {@link FieldInformation} object containing class and type of
	 *         the document entry or, if the entry is not an interesting field,
	 *         an empty {@link FieldInformation} object containing
	 *         {@link FieldInformation#UNKNOWN} class and type.
	 */
	protected FieldInformation analyzeDocumentEntry(DocumentEntry de) {
		final String name = de.getName();
		// we are only interested in document entries
		// with names starting with __substg1.
		logger.finest("Document entry: " + name);
		if (name.startsWith(propertyStreamPrefix)) {
			String clazz = FieldInformation.UNKNOWN;
			String type = FieldInformation.UNKNOWN;
			int mapiType = FieldInformation.UNKNOWN_MAPITYPE;
			try {
				final String val = name.substring(propertyStreamPrefix.length()).toLowerCase();
				// the first 4 digits of the remainder
				// defines the field class (or field name)
				// and the last 4 digits indicate the
				// data type.
				clazz = val.substring(0, 4);
				type = val.substring(4);
				logger.finest("  Found document entry: class=" + clazz + ", type=" + type);
				mapiType = Integer.parseInt(type, 16);
			} catch (final RuntimeException re) {
				logger.log(Level.FINE, "Could not parse directory entry " + name, re);
				return new FieldInformation();
			}
			return new FieldInformation(clazz, mapiType);
		} else {
			logger.finest("Ignoring entry with name " + name);
		}
		// we are not interested in the field
		// and return an empty FieldInformation object
		return new FieldInformation();
	}

	private String bytesToHex(byte[] bytes) throws IOException {
		final StringBuilder byteStr = new StringBuilder();
		for (final byte b : bytes) {
			byteStr.append(String.format("%02X", b & 0xff));
		}
		return byteStr.toString();
	}

	/**
	 * Parses a directory document entry which can either be a simple entry or a
	 * stream that has to be split up into multiple document entries again. The
	 * parsed information is put into the {@link Message} object.
	 *
	 * @param de
	 *            The current node in the .msg file.
	 * @param msg
	 *            The resulting {@link Message} object.
	 * @throws IOException
	 *             Thrown if the .msg file could not be parsed.
	 */
	protected void checkDirectoryDocumentEntry(DocumentEntry de, Message msg) throws IOException {
		if (de.getName().startsWith(propsKey)) {
			// TODO: parse properties stream
			final List<DocumentEntry> deList = getDocumentEntriesFromPropertiesStream(de);
			for (final DocumentEntry deFromProps : deList) {
				final MessageProperty msgProp = getMessagePropertyFromDocumentEntry(deFromProps);
				msg.setProperty(msgProp);
			}
		} else {
			final MessageProperty msgProp = getMessagePropertyFromDocumentEntry(de);
			msg.setProperty(msgProp);
		}
	}

	/**
	 * Recursively parses the complete .msg file with the help of the POI
	 * library. The parsed information is put into the {@link Message} object.
	 *
	 * @param dir
	 *            The current node in the .msg file.
	 * @param msg
	 *            The resulting {@link Message} object.
	 * @throws IOException
	 *             Thrown if the .msg file could not be parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file contains unknown data.
	 */
	protected void checkDirectoryEntry(DirectoryEntry dir, Message msg)
			throws IOException, UnsupportedOperationException {

		// we iterate through all entries in the current directory
		for (final Iterator<?> iter = dir.getEntries(); iter.hasNext();) {
			final Entry entry = (Entry) iter.next();

			// check whether the entry is either a directory entry
			// or a document entry

			if (entry.isDirectoryEntry()) {
				final DirectoryEntry de = (DirectoryEntry) entry;
				// attachments have a special name and
				// have to be handled separately at this point
				if (de.getName().startsWith("__attach_version1.0")) {
					this.parseAttachment(de, msg);
				} else if (de.getName().startsWith("__recip_version1.0")) {
					// a recipient entry has been found (which is also a
					// directory entry itself)
					this.checkRecipientDirectoryEntry(de, msg);
				} else {
					// a directory entry has been found. this
					// node will be recursively checked
					this.checkDirectoryEntry(de, msg);
				}
			} else if (entry.isDocumentEntry()) {
				// a document entry contains information about
				// the mail (e.g, from, to, subject, ...)
				final DocumentEntry de = (DocumentEntry) entry;
				checkDirectoryDocumentEntry(de, msg);
			} else {
				// any other type is not supported
			}
		}
	}

	/**
	 * Parses a recipient directory entry which holds informations about one of
	 * possibly multiple recipients. The parsed information is put into the
	 * {@link Message} object.
	 *
	 * @param dir
	 *            The current node in the .msg file.
	 * @param msg
	 *            The resulting {@link Message} object.
	 * @throws IOException
	 *             Thrown if the .msg file could not be parsed.
	 */
	protected void checkRecipientDirectoryEntry(DirectoryEntry dir, Message msg) throws IOException {

		final RecipientEntry recipient = new RecipientEntry();

		// we iterate through all entries in the current directory
		for (final Iterator<?> iter = dir.getEntries(); iter.hasNext();) {
			final Entry entry = (Entry) iter.next();

			// check whether the entry is either a directory entry
			// or a document entry, while we are just interested in document
			// entries on this level
			if (entry.isDirectoryEntry()) {
				// not expected within a recipient entry
			} else if (entry.isDocumentEntry()) {
				// a document entry contains information about
				// the mail (e.g, from, to, subject, ...)
				final DocumentEntry de = (DocumentEntry) entry;
				checkRecipientDocumentEntry(de, recipient);
			} else {
				// any other type is not supported
			}
		}

		// after all properties are set -> add recipient to msg object
		msg.addRecipient(recipient);
	}

	/**
	 * Parses a recipient document entry which can either be a simple entry or a
	 * stream that has to be split up into multiple document entries again. The
	 * parsed information is put into the {@link RecipientEntry} object.
	 *
	 * @param de
	 *            The current node in the .msg file.
	 * @param recipient
	 *            The resulting {@link RecipientEntry} object.
	 * @throws IOException
	 *             Thrown if the .msg file could not be parsed.
	 */
	protected void checkRecipientDocumentEntry(DocumentEntry de, RecipientEntry recipient) throws IOException {
		if (de.getName().startsWith(propsKey)) {
			// TODO: parse properties stream
			final List<DocumentEntry> deList = getDocumentEntriesFromPropertiesStream(de);
			for (final DocumentEntry deFromProps : deList) {
				final MessageProperty msgProp = getMessagePropertyFromDocumentEntry(deFromProps);
				recipient.setProperty(msgProp);
			}
		} else {
			final MessageProperty msgProp = getMessagePropertyFromDocumentEntry(de);
			recipient.setProperty(msgProp);
		}
	}

	/**
	 * Reads the bytes from the DocumentEntry. This is a convenience method that
	 * calls {@see #getBytesFromStream(InputStream)} internally. It ensures that
	 * the opened input stream is closed at the end.
	 *
	 * @param de
	 *            The document entry that should be read.
	 * @return The bytes of the document entry.
	 * @throws IOException
	 *             Thrown if the document entry could not be read.
	 */
	private byte[] getBytesFromDocumentEntry(DocumentEntry de) throws IOException {
		InputStream is = null;
		try {
			is = new DocumentInputStream(de);
			return this.getBytesFromStream(is);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (final Exception e) {
					logger.fine("Could not close input stream for document entry: " + de + ": " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Reads the bytes from the stream to a byte array.
	 *
	 * @param dstream
	 *            The stream to be read from.
	 * @return An array of bytes.
	 * @throws IOException
	 *             If the stream cannot be read properly.
	 */
	private byte[] getBytesFromStream(InputStream dstream) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] buffer = new byte[1024];
		int read = -1;
		while ((read = dstream.read(buffer)) > 0) {
			baos.write(buffer, 0, read);
		}
		final byte[] bytes = baos.toByteArray();
		return bytes;
	}

	/**
	 * Reads the information from the InputStream and creates, based on the
	 * information in the {@link FieldInformation} object, either a String or a
	 * byte[] (e.g., for attachments) Object containing this data.
	 *
	 * @param de
	 *            The Document Entry.
	 * @param info
	 *            The field information that is needed to determine the data
	 *            type of the input stream.
	 * @return The String/byte[] object representing the data.
	 * @throws IOException
	 *             Thrown if the .msg file could not be parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file contains unknown data.
	 */
	protected Object getData(DocumentEntry de, FieldInformation info) throws IOException {
		// if there is no field information available, we simply
		// return null. in that case, we're not interested in the
		// data anyway
		if (info == null)
			return null;

		// if the type is 001e (we know it is lower case
		// because analyzeDocumentEntry stores the type in
		// lower case), we create a String object from the data.
		// the encoding of the binary data is most probably
		// ISO-8859-1 (not pure ASCII).
		final int mapiType = info.getMapiType();

		switch (mapiType) {
			case FieldInformation.UNKNOWN_MAPITYPE :
				// if there is no field information available, we simply return
				// null
				// in that case, we're not interested in the data anyway
				return null;
			case 0x1e :
				// we put the complete data into a byte[] object...
				final byte[] textBytes1e = this.getBytesFromDocumentEntry(de);
				// ...and create a String object from it
				final String text1e = new String(textBytes1e, "ISO-8859-1");
				return text1e;
			case 0x1f :
				// Unicode encoding with lowbyte followed by hibyte
				// Note: this is arcane guesswork, but it works
				final byte[] textBytes1f = this.getBytesFromDocumentEntry(de);
				// now that we have all bytes from the stream,
				// we can now convert the byte array into
				// a character array by switching hi- and lowbytes
				final char[] characters = new char[textBytes1f.length / 2];
				int c = 0;
				for (int i = 0; i < textBytes1f.length - 1; i = i + 2) {
					final int ch = textBytes1f[i + 1];
					final int cl = textBytes1f[i] & 0xff; // Using unsigned
															// value (thanks to
															// Reto Schuettel)
					characters[c++] = (char) ((ch << 8) + cl);
				}
				final String text1f = new String(characters);
				return text1f;
			case 0x102 :
				try {
					// the data is read into a byte[] object
					final byte[] bytes102 = this.getBytesFromDocumentEntry(de);
					// and returned as-is
					return bytes102;
				} catch (final Exception e) {
					logger.fine("Could not get content of byte array of field 0x102: " + e.getMessage());
					// To keep compatible with previous implementations, we
					// return an empty array here
					return new byte[0];
				}
			case 0x40 :
				// The following part has been provided by Morten Sørensen
				// (Thanks!)

				// This parsing has been lifted from the MsgViewer project
				// https://sourceforge.net/projects/msgviewer/

				// the data is read into a byte[] object
				final byte[] bytes = this.getBytesFromDocumentEntry(de);
				// Read the byte array as little endian byteorder
				final ByteBuffer buff = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
				buff.put(bytes);
				// Convert the bytes to a long
				// Nanoseconds since 1601
				Long timeLong = buff.getLong(0);
				// Convert to milliseconds
				timeLong /= 10000L;
				// Move the offset from since 1601 to 1970
				timeLong -= 11644473600000L;
				// Convert to a Date object, and return
				return new Date(timeLong);
			default :
				// this should not happen
				logger.fine("Unknown field type " + mapiType);
				return null;
		}

	}

	/**
	 * Parses a document entry which has been detected to be a stream of
	 * document entries itself. This stream is identified by the key
	 * "__properties_version1.0".
	 *
	 * @param de
	 *            The stream to be parsed.
	 * @return A list of document entries for further processing.
	 * @throws IOException
	 *             Thrown if the properties stream could not be parsed.
	 */
	private List<DocumentEntry> getDocumentEntriesFromPropertiesStream(DocumentEntry de) throws IOException {
		final List<DocumentEntry> result = new ArrayList<>();
		DocumentInputStream dstream = null;
		try {
			dstream = new DocumentInputStream(de);

			final int headerLength = 4;
			final int flagsLength = 4;
			byte[] bytes = new byte[headerLength];
			while (dstream.read(bytes) == headerLength) {
				final StringBuilder header = new StringBuilder();
				for (int i = bytes.length - 1; i >= 0; i--) {
					header.append(bytesToHex(new byte[]{bytes[i]}));
				}

				// header ready for use
				final String type = header.substring(4);
				final String clazz = header.substring(0, 4);

				int typeNumber = -1;
				try {
					typeNumber = Integer.parseInt(type, 16);
				} catch (final NumberFormatException e) {
					logger.log(Level.FINEST, "Unexpected type: " + type);
				}

				if (!clazz.equals("0000")) {
					// reading and ignoring flags
					bytes = new byte[flagsLength];
					dstream.read(bytes);
					// System.out.println("flags: " + bytesToHex(bytes));

					// reading data
					if (typeNumber == 0x48 // CLSID
							|| typeNumber == 0x1e // STRING
							|| typeNumber == 0x1f // UNICODE STRING
							|| typeNumber == 0xd // OBJECT
							|| typeNumber == 0x102) { // BINARY
						// found datatype with variable length, thus the value
						// is stored in a separate string
						// no data available inside the properties stream
						// reading and ignoring size
						bytes = null;
						dstream.read(new byte[4]);
					} else if (typeNumber == 0x3 // INT
							|| typeNumber == 0x4 // FLOAT
							|| typeNumber == 0xa // ERROR
							|| typeNumber == 0xb // BOOLEAN
							|| typeNumber == 0x2) { // SHORT
						// 4 bytes
						bytes = new byte[4];
						dstream.read(bytes);
						dstream.read(bytes); // read and ignore padding
					} else if (typeNumber == 0x5 // DOUBLE
							|| typeNumber == 0x7 // APPTIME
							|| typeNumber == 0x6 // CURRENCY
							|| typeNumber == 0x14 // INT8BYTE
							|| typeNumber == 0x40) { // SYSTIME
						// 8 bytes
						bytes = new byte[8];
						dstream.read(bytes);
					}
					// stream ready for use

					if (bytes != null) {
						// creating new document entry for later processing of
						// all document entries
						final POIFSFileSystem poifs = new POIFSFileSystem();
						result.add(poifs.createDocument(new ByteArrayInputStream(bytes),
								"__substg1.0_" + header.toString()));
					}
				}

				// start over with new byte[] for next header
				bytes = new byte[headerLength];
			}

			return result;

		} finally {
			if (dstream != null) {
				try {
					dstream.close();
				} catch (final Exception e) {
					logger.fine("Could not close input stream of document entry: " + de + ": " + e.getMessage());
				}
			}
		}

	}

	/**
	 * Reads a property from a document entry and puts it's type and data to a
	 * {@link MessageProperty} object.
	 *
	 * @param de
	 *            The {@link DocumentEntry} to be read.
	 * @return An object holding the type and data of the read property.
	 * @throws IOException
	 *             In case the property could not be parsed.
	 */
	private MessageProperty getMessagePropertyFromDocumentEntry(DocumentEntry de) throws IOException {
		// analyze the document entry
		// (i.e., get class and data type)
		final FieldInformation info = this.analyzeDocumentEntry(de);
		// create a Java object from the data provided
		// by the input stream. depending on the field
		// information, either a String or a byte[] will
		// be returned. other datatypes are not yet supported
		final Object data = this.getData(de, info);
		logger.finest("  Document data: " + (data == null ? "null" : data.toString()));
		return new MessageProperty(info.getClazz(), data, de.getSize());
	}

	/**
	 * Creates an {@link Attachment} object based on the given directory entry.
	 * The entry may either point to an attached file or to an attached .msg
	 * file, which will be added as a {@link MsgAttachment} object instead.
	 *
	 * @param dir
	 *            The directory entry containing the attachment document entry
	 *            and some other document entries describing the attachment
	 *            (name, extension, mime type, ...)
	 * @param msg
	 *            The {@link Message} object that this attachment should be
	 *            added to.
	 * @throws IOException
	 *             Thrown if the attachment could not be parsed/read.
	 */
	protected void parseAttachment(DirectoryEntry dir, Message msg) throws IOException {

		final FileAttachment attachment = new FileAttachment();

		// iterate through all document entries
		for (final Iterator<?> iter = dir.getEntries(); iter.hasNext();) {
			final Entry entry = (Entry) iter.next();
			if (entry.isDocumentEntry()) {

				// the document entry may contain information
				// about the attachment
				final DocumentEntry de = (DocumentEntry) entry;
				final MessageProperty msgProp = getMessagePropertyFromDocumentEntry(de);

				// we provide the class and data of the document
				// entry to the attachment. the attachment implementation
				// has to know the semantics of the field names
				attachment.setProperty(msgProp);

			} else {

				// a directory within the attachment directory
				// entry means that a .msg file is attached
				// at this point. we recursively parse
				// this .msg file and add it as a MsgAttachment
				// object to the current Message object.
				final Message attachmentMsg = new Message();
				final MsgAttachment msgAttachment = new MsgAttachment();
				msgAttachment.setMessage(attachmentMsg);
				msg.addAttachment(msgAttachment);
				this.checkDirectoryEntry((DirectoryEntry) entry, attachmentMsg);
			}
		}

		// only if there was really an attachment, we
		// add this object to the Message object
		if (attachment.getSize() > -1) {
			msg.addAttachment(attachment);
		}

	}

	/**
	 * Parses a .msg file provided in the specified file.
	 *
	 * @param msgFile
	 *            The .msg file.
	 * @return A {@link Message} object representing the .msg file.
	 * @throws IOException
	 *             Thrown if the file could not be loaded or parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file cannot be parsed correctly.
	 */
	public Message parseMsg(File msgFile) throws IOException, UnsupportedOperationException {
		return this.parseMsg(new FileInputStream(msgFile), true);
	}

	/**
	 * Parses a .msg file provided by an input stream.
	 *
	 * @param msgFileStream
	 *            The .msg file as a InputStream.
	 * @return A {@link Message} object representing the .msg file.
	 * @throws IOException
	 *             Thrown if the file could not be loaded or parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file cannot be parsed correctly.
	 */
	public Message parseMsg(InputStream msgFileStream) throws IOException, UnsupportedOperationException {
		return this.parseMsg(msgFileStream, true);
	}

	/**
	 * Parses a .msg file provided by an input stream.
	 *
	 * @param msgFileStream
	 *            The .msg file as a InputStream.
	 * @param closeStream
	 *            Indicates whether the provided stream should be closed after
	 *            the message has been read.
	 * @return A {@link Message} object representing the .msg file.
	 * @throws IOException
	 *             Thrown if the file could not be loaded or parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file cannot be parsed correctly.
	 */
	public Message parseMsg(InputStream msgFileStream, boolean closeStream)
			throws IOException, UnsupportedOperationException {
		// the .msg file, like a file system, contains directories
		// and documents within this directories
		// we now gain access to the root node
		// and recursively go through the complete 'filesystem'.
		Message msg = null;
		try {
			final POIFSFileSystem fs = new POIFSFileSystem(msgFileStream);
			final DirectoryEntry dir = fs.getRoot();
			msg = new Message(rtf2htmlConverter);
			this.checkDirectoryEntry(dir, msg);
		} finally {
			if (closeStream) {
				try {
					msgFileStream.close();
				} catch (final Exception e) {
					// ignore
				}
			}
		}
		return msg;
	}

	/**
	 * Parses a .msg file provided in the specified file.
	 *
	 * @param msgFile
	 *            The .msg file as a String path.
	 * @return A {@link Message} object representing the .msg file.
	 * @throws IOException
	 *             Thrown if the file could not be loaded or parsed.
	 * @throws UnsupportedOperationException
	 *             Thrown if the .msg file cannot be parsed correctly.
	 */
	public Message parseMsg(String msgFile) throws IOException, UnsupportedOperationException {
		return this.parseMsg(new FileInputStream(msgFile), true);
	}

	/**
	 * Setter for overriding the default {@link RTF2HTMLConverter}
	 * implementation which is used to get HTML code from an RTF body.
	 *
	 * @param rtf2htmlConverter
	 *            The converter instance to be used.
	 */
	public void setRtf2htmlConverter(RTF2HTMLConverter rtf2htmlConverter) {
		this.rtf2htmlConverter = rtf2htmlConverter;
	}
}
