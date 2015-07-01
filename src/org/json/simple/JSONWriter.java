/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Elad Tabak
 * @author Maciej Komosinski, minor improvements
 *
 */
package org.json.simple;

import java.io.StringWriter;

/**
 * Sample usage:
 * <pre>
 * Writer writer = new JSONWriter(); // this writer adds indentation
 * jsonobject.writeJSONString(writer);
 * System.out.println(writer.toString());
 * </pre>
 *
 * @author Elad Tabak
 * @author Maciej Komosinski, minor improvements, 2015
 * @since 28-Nov-2011
 * @version 0.2
 */
public class JSONWriter extends StringWriter
{
   final static String indentstring = "  "; //define as you wish
   final static String spaceaftercolon = " "; //use "" if you don't want space after colon

   private int indentlevel = 0;

   @Override
   public void write(int c)
   {
      char ch = (char) c;
      if (ch == '[' || ch == '{')
      {
        super.write(c);
        super.write('\n');
        indentlevel++;
        writeIndentation();
      } else if (ch == ',')
      {
        super.write(c);
        super.write('\n');
        writeIndentation();
      } else if (ch == ']' || ch == '}')
      {
        super.write('\n');
        indentlevel--;
        writeIndentation();
        super.write(c);
      } else if (ch == ':')
      {
        super.write(c);
        super.write(spaceaftercolon);
      } else
      {
        super.write(c);
      }

   }

   private void writeIndentation()
   {
      for (int i = 0; i < indentlevel; i++)
      {
        super.write(indentstring);
      }
   }
}
