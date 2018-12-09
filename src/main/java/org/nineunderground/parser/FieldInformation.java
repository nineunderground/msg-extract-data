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

import org.apache.poi.poifs.filesystem.DocumentEntry;

/**
 * The Class FieldInformation.
 *
 * @author inaki
 *
 */
public class FieldInformation {

	/**
	 * The default value for both the {@link #clazz} and the {@link #type}
	 * properties.
	 */
	public static final String UNKNOWN = "unknown";

	/**
	 * The default value for the {@link #mapiType}
	 */
	public static final int UNKNOWN_MAPITYPE = -1;

	/**
	 * The class of the {@link DocumentEntry}.
	 */
	protected String clazz = UNKNOWN;
	/**
	 * The type of the {@link DocumentEntry}.
	 */
	protected String type = UNKNOWN;

	/**
	 * The mapi type of the {@link DocumentEntry}.
	 */
	protected int mapiType = UNKNOWN_MAPITYPE;

	/**
	 * Empty constructor that uses the default values.
	 */
	public FieldInformation() {
	}

	/**
	 * Constructor that allows to set the class and type properties.
	 *
	 * @param clazz
	 *            The class of the {@link DocumentEntry}.
	 * @param mapiType
	 *            The mapiType of the {@link DocumentEntry} (see
	 *            {@link MAPIProp}).
	 */
	public FieldInformation(String clazz, int mapiType) {
		this.setClazz(clazz);
		this.setMapiType(mapiType);
	}

	/**
	 * Constructor that allows to set the class and type properties.
	 *
	 * @param clazz
	 *            The class of the {@link DocumentEntry}.
	 * @param type
	 *            The type of the {@link DocumentEntry}.
	 */
	@Deprecated
	public FieldInformation(String clazz, String type) {
		this.setClazz(clazz);
		this.setType(type);
	}

	/**
	 * @return the clazz
	 */
	public String getClazz() {
		return clazz;
	}

	/**
	 * @return the mapiType
	 */
	public int getMapiType() {
		return mapiType;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param clazz
	 *            the clazz to set
	 */
	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	/**
	 * @param mapiType
	 *            the mapiType to set
	 */
	public void setMapiType(int mapiType) {
		this.mapiType = mapiType;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
