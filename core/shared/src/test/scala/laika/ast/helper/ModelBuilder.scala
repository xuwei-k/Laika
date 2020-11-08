/*
 * Copyright 2012-2020 the original author or authors.
 *
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
 */

package laika.ast.helper

import laika.ast._
import laika.config.{Config, ConfigParser}
import laika.parse.{LineSource, SourceCursor, SourceFragment}

trait ModelBuilder { self =>

  def p (spans: Span*): Paragraph = Paragraph(spans.toList)

  def p (text: String): Paragraph = Paragraph(text)

  
  def toSource (label: FootnoteLabel): String = label match {
    case Autonumber => "[#]_"
    case Autosymbol => "[*]_"
    case AutonumberLabel(label) => s"[#$label]_"
    case NumericLabel(label) => s"[$label]_"
  }
  
  def source (fragment: String, root: String): SourceFragment = {
    val offset = root.indexOf(fragment)
    LineSource(fragment, SourceCursor(root).consume(offset))
  }

  def generatedSource (fragment: String): SourceFragment = LineSource(fragment, SourceCursor(fragment))

  
  val disableInternalLinkValidation: Config = 
    ConfigParser.parse("""{ laika.links.excludeFromValidation = ["/"]}""").resolve().toOption.get
}
