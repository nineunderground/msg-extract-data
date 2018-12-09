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
package org.nineunderground.parser.attachment;

import org.nineunderground.parser.MessageProperty;

/**
 * The Class FileAttachment.
 *
 * @author inaki
 */
public class FileAttachment implements Attachment {

	/**
	 * The (by Outlook) shortened filename of the attachment.
	 */
	protected String filename = null;
	/**
	 * The full filename of the attachment.
	 */
	protected String longFilename = null;

	/** Mime type of the attachment. */
	protected String mimeTag = null;
	/**
	 * The extension of the attachment (may not be set).
	 */
	protected String extension = null;
	/**
	 * The attachment itself as a byte array.
	 */
	protected byte[] data = null;
	/**
	 * The size of the attachment.
	 */
	protected long size = -1;

	/**
	 * Gets the data.
	 *
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Gets the extension.
	 *
	 * @return the extension
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Gets the filename.
	 *
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * Gets the long filename.
	 *
	 * @return the longFilename
	 */
	public String getLongFilename() {
		return longFilename;
	}

	/**
	 * Gets the mime tag.
	 *
	 * @return the mimeTag
	 */
	public String getMimeTag() {
		return mimeTag;
	}

	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Sets the data.
	 *
	 * @param data
	 *            the data to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * Sets the extension.
	 *
	 * @param extension
	 *            the extension to set
	 */
	public void setExtension(String extension) {
		this.extension = extension;
	}

	/**
	 * Sets the filename.
	 *
	 * @param filename
	 *            the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Sets the long filename.
	 *
	 * @param longFilename
	 *            the longFilename to set
	 */
	public void setLongFilename(String longFilename) {
		this.longFilename = longFilename;
	}

	/**
	 * Sets the mime tag.
	 *
	 * @param mimeTag
	 *            the mimeTag to set
	 */
	public void setMimeTag(String mimeTag) {
		this.mimeTag = mimeTag;
	}

	/**
	 * Sets the property specified by the name parameter. Unknown names are
	 * ignored.
	 *
	 * @param msgProp
	 *            The property to be set.
	 * @throws ClassCastException
	 *             the class cast exception
	 */
	public void setProperty(MessageProperty msgProp) throws ClassCastException {
		String name = msgProp.getClazz();
		final Object value = msgProp.getData();
		final int size = msgProp.getSize();

		if (name == null || value == null)
			return;
		name = name.intern();

		if (name == "3701") {
			this.setSize(size);
			this.setData((byte[]) value);
		} else if (name == "3704") {
			this.setFilename((String) value);
		} else if (name == "3707") {
			this.setLongFilename((String) value);
		} else if (name == "370e") {
			this.setMimeTag((String) value);
		} else if (name == "3703") {
			this.setExtension((String) value);
		}
	}

	/**
	 * Sets the size.
	 *
	 * @param size
	 *            the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Returns either the long filename or the short filename, depending on
	 * which is available.
	 *
	 * @return the string
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (this.longFilename != null)
			return this.longFilename;
		return this.filename;
	}
}
