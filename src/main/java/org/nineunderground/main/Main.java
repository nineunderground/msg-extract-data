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
package org.nineunderground.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.nineunderground.parser.Message;
import org.nineunderground.parser.MsgParser;
import org.nineunderground.parser.attachment.Attachment;
import org.nineunderground.parser.attachment.FileAttachment;

/**
 * The Class Main.
 *
 * @author inaki rodriguez
 */
public class Main {

	private static Logger LOGGER = null;

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
		LOGGER = Logger.getLogger(Main.class.getName());
	}

	/**
	 * Generate files.
	 *
	 * @param attachments
	 *            the attachments
	 * @throws IOException
	 */
	private static void generateFiles(List<Attachment> attachments) throws IOException {
		for (final Attachment att : attachments) {
			if (att instanceof FileAttachment) {
				final FileAttachment fileAtt = (FileAttachment) att;
				final FileOutputStream fos = new FileOutputStream(fileAtt.getFilename());
				fos.write(fileAtt.getData());
				fos.close();
				LOGGER.info(fileAtt.getMimeTag() + " File extracted from msg -> " + fileAtt.getFilename());
			}
		}
	}

	/**
	 * Checks if is valid file.
	 *
	 * @param filePath
	 *            the file path
	 * @return true, if is valid file
	 */
	private static boolean isValidFile(String filePath) {
		final File testFile = new File(filePath);
		if (testFile.exists() && testFile.isFile() && testFile.getName().endsWith(".msg"))
			return true;
		return false;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		LOGGER.setLevel(Level.INFO);
		if (args == null || args.length != 1) {
			LOGGER.info("Please run with a valid file as an argument");
			LOGGER.info("Syntax:");
			LOGGER.info("java -jar build/libs/msgToOsx-1.0.jar");
			LOGGER.info("");
			return;
		}
		if (isValidFile(args[0])) {
			final List<String> output = parseMsgFile(args[0]);
			for (final String line : output) {
				LOGGER.info(line);
			}
		}

	}

	/**
	 * Parses the msg file.
	 */
	public static List<String> parseMsgFile(String fileName) {
		final List<String> output = new ArrayList<>();
		try {
			output.add("STARTS");
			final MsgParser msgp = new MsgParser();
			final Handler[] handlers = LOGGER.getLogger("").getHandlers();
			for (final Handler handler : handlers) {
				handler.setLevel(Level.INFO);
			}
			final File testFile = new File(fileName);
			final Message msg = msgp.parseMsg(testFile);
			output.add("---------------------------------------");
			output.add("From: " + msg.getFromName());
			output.add("To: " + msg.getToName());
			output.add("Subject: " + msg.getSubject());
			output.add("Attachments: " + msg.getAttachments().size());
			generateFiles(msg.getAttachments());
			output.add("Body: \n" + msg.getBodyText());
			output.add("ENDS");
		} catch (final Exception e) {
			output.add("ERROR " + e.getMessage());
		}
		return output;
	}
}
