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
// ============================================================================
// Braintribe IT Technologies GmbH
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2012
// www.braintribe.com
// ============================================================================
// last changed date: $LastChangedDate: $
// last changed by: $LastChangedBy: $
// ============================================================================
package org.nineunderground.parser;

/**
 * The Class MessageProperty.
 *
 * @author inaki
 */
public class MessageProperty {

	private final String clazz;
	private final Object data;
	private final int size;

	public MessageProperty(String clazz, Object data, int size) {
		super();
		this.clazz = clazz;
		this.data = data;
		this.size = size;
	}

	/**
	 * A 4 digit code representing the property type.
	 *
	 * @return A string representation of the property type.
	 */
	public String getClazz() {
		return clazz;
	}

	/**
	 * An object holding the property data.
	 *
	 * @return The property data.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * The size of the data.
	 *
	 * @return The number of bytes of the data object.
	 */
	public int getSize() {
		return size;
	}

}
