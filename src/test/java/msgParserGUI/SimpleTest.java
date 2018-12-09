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
/**
 *
 */
package msgParserGUI;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.nineunderground.main.Main;

/**
 * @author inaki
 *
 */
public class SimpleTest {

	@Test
	public void test() {

		try {
			final List<String> output = Main.parseMsgFile("src/test/resources/test_file.msg");
			final StringBuilder sb = new StringBuilder();
			for (final String line : output) {
				sb.append(line);
			}
			final String expectedOutput = new String(
					Files.readAllBytes(Paths.get("src/test/resources/expected-output.txt")));
			assertTrue("Output expected... ", expectedOutput.equals(sb.toString()));
		} catch (final IOException e) {
			fail("Error reading file " + e.getMessage());
		}

	}

}
