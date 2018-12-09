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
package org.nineunderground.parser.rtf;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class SimpleRTF2HTMLConverter.
 *
 * @author inaki
 *
 */
public class SimpleRTF2HTMLConverter implements RTF2HTMLConverter {

	protected static final Logger logger = Logger.getLogger(SimpleRTF2HTMLConverter.class.getName());

	/**
	 * Converts a hexadecimal representation of a special character to string.
	 *
	 * @param hex
	 *            The hex value to be converted.
	 * @return The string representing the hex value.
	 */
	private static String hexToString(String hex, String charset) {
		int i = 0;
		try {
			i = Integer.parseInt(hex, 16);
		} catch (final NumberFormatException nfe) {
			logger.warning("Could not interpret " + hex + " as a number.");
			return null;
		}
		final byte[] b = new byte[]{(byte) i};
		try {
			return new String(b, charset);
		} catch (final UnsupportedEncodingException e) {
			logger.log(Level.FINEST, "Unsupported encoding: " + charset);
		}
		return null;
	}

	/**
	 * Fetches the actual HTML block that should be cleaned afterwards.
	 *
	 * @param text
	 *            The text to be searched for an HTML section.
	 * @return The HTML section only but still with RTF code inside.
	 */
	private String fetchHtmlSection(String text) {
		String html = null;
		int htmlStart = -1;
		int htmlEnd = -1;

		// determine html tags
		final String[] htmlStartTags = new String[]{"<html ", "<Html ", "<HTML "};
		final String[] htmlEndTags = new String[]{"</html>", "</Html>", "</HTML>"};
		for (int i = 0; i < htmlStartTags.length && htmlStart < 0; i++) {
			htmlStart = text.indexOf(htmlStartTags[i]);
		}
		for (int i = 0; i < htmlEndTags.length && htmlEnd < 0; i++) {
			htmlEnd = text.indexOf(htmlEndTags[i]);
			if (htmlEnd > 0) {
				htmlEnd = htmlEnd + htmlEndTags[i].length();
			}
		}

		if (htmlStart > -1 && htmlEnd > -1) {
			// trim rtf code
			html = text.substring(htmlStart, htmlEnd + 1);
		} else {
			// embed code within html tags
			html = "<html><body style=\"font-family:'Courier',monospace;font-size:10pt;\">" + text + "</body></html>";
			// replace linebreaks with html breaks
			html = html.replaceAll("[\\n\\r]+", " <br/> ");
			// create hyperlinks
			html = html.replaceAll("(http://\\S+)", "<a href=\"$1\">$1</a>");
			html = html.replaceAll("mailto:(\\S+@\\S+)", "<a href=\"mailto:$1\">$1</a>");
		}

		return html;
	}

	/**
	 * Replaces sequences that denote hex codes for strings using Windows CP1252
	 * encoding.
	 *
	 * @param text
	 *            The text to be searched.
	 * @return The text with replaced special characters.
	 */
	private String replaceHexSequences(String text) {
		final Pattern p = Pattern.compile("\\\\'(..)");
		final Matcher m = p.matcher(text);

		while (m.find()) {
			for (int g = 1; g <= m.groupCount(); g++) {
				final String hex = m.group(g);
				final String hexToString = hexToString(hex, "CP1252");
				if (hexToString != null) {
					text = text.replaceAll("\\\\'" + hex, hexToString);
				}
			}
		}

		return text;
	}

	/**
	 * Replaces/filters newlines as they are only part of the RTF document and
	 * should not be inside the HTML.
	 *
	 * @param text
	 *            The text to be processed.
	 * @return The text with removed newlines.
	 */
	private String replaceLineBreaks(String text) {
		text = text.replaceAll("( <br/> ( <br/> )+)", " <br/> ");
		text = text.replaceAll("[\\n\\r]+", "");
		return text;
	}

	/**
	 * Replaces control sequences such as line breaks with plain text breaks or
	 * equivalent representations.
	 *
	 * @param text
	 *            The text to be processed.
	 * @return The text with all control sequences replaced.
	 */
	private String replaceRemainingControlSequences(String text) {
		// filtering \par sequences
		text = text.replaceAll("\\\\pard*", "\n");
		// filtering \tab sequences
		text = text.replaceAll("\\\\tab", "\t");
		// filtering \*\<rtfsequence> like e.g.: \*\fldinst
		text = text.replaceAll("\\\\\\*\\\\\\S+", "");
		// filtering \<rtfsequence> like e.g.: \htmlrtf
		text = text.replaceAll("\\\\\\S+", "");
		return text;
	}

	/**
	 * Replaces special sequences with equivalent representations.
	 *
	 * @param text
	 *            The text to be processed.
	 * @return The text with all control sequences replaced.
	 */
	private String replaceSpecialSequences(String text) {

		// filtering whatever color control sequence, e.g. {\sp{\sn
		// fillColor}{\sv 14935011}}{\sp{\sn fFilled}{\sv 1}}
		text = text.replaceAll("\\{\\\\S+ [^\\s\\\\}]*\\}", "");
		// filtering hyperlink sequences like {HYPERLINK
		// "http://xyz.com/print.jpg"}
		text = text.replaceAll("\\{HYPERLINK[^\\}]*\\}", "");
		// filtering plain text sequences like {\pntext *\tab}
		text = text.replaceAll("\\{\\\\pntext[^\\}]*\\}", "");
		// filtering rtf style headers like {\f0\fswiss\fcharset0 Arial;}
		text = text.replaceAll("\\{\\\\f\\d+[^\\}]*\\}", "");
		// filtering embedded tags like {\*\htmltag64 <tr>} }
		text = text.replaceAll("\\{\\\\\\*\\\\htmltag\\d+[^\\}<]+(<.+>)\\}", "$1");
		// filtering embedded tags like {\*\htmltag84 &#43;}
		text = text.replaceAll("\\{\\\\\\*\\\\htmltag\\d+[^\\}<]+\\}", "");
		// filtering curly braces that are NOT escaped with backslash },
		// thus marking the end of an RTF sequence
		text = text.replaceAll("([^\\\\])" + "\\}+", "$1");
		text = text.replaceAll("([^\\\\])" + "\\{+", "$1");
		// filtering curly braces that are escaped with backslash \},
		// thus representing an actual brace
		text = text.replaceAll("\\\\\\}", "}");
		text = text.replaceAll("\\\\\\{", "{");

		return text;
	}

	@Override
	public String rtf2html(String rtf) throws Exception {

		String plain = null;

		if (rtf != null) {
			plain = rtf;
			plain = this.fetchHtmlSection(plain);
			plain = this.replaceHexSequences(plain);
			plain = this.replaceSpecialSequences(plain);
			plain = this.replaceRemainingControlSequences(plain);
			plain = this.replaceLineBreaks(plain);
		}

		return plain;
	}

}
