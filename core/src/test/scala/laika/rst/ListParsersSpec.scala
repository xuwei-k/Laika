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

package laika.rst

import laika.ast._
import laika.ast.helper.ModelBuilder
import laika.parse.Parser
import laika.parse.helper.{DefaultParserHelpers, ParseResultHelpers}
import laika.rst.ast._
import laika.rst.ext.Directives.DirectivePart
import laika.rst.ext.{ExtensionProvider, RootParserProvider}
import laika.rst.ext.TextRoles.RoleDirectivePart
import org.scalatest.{FlatSpec, Matchers}
     
class ListParsersSpec extends FlatSpec 
                        with Matchers 
                        with ParseResultHelpers
                        with DefaultParserHelpers[RootElement] 
                        with ModelBuilder {

  import EnumType._

  val rootParser = RootParserProvider.forBundle(ExtensionProvider.forExtensions())
  val defaultParser: Parser[RootElement] = rootParser.rootElement

  
  def blockDirective (name: String): Option[DirectivePart[Block]] = None
  def spanDirective (name: String): Option[DirectivePart[Span]] = None
  def textRole (name: String): Option[RoleDirectivePart[String => Span]] = None
  
  
  def fp (content: String) = ForcedParagraph(List(Text(content)))
  
  
  def fl (fields: Field*) = FieldList(fields.toList)
  
  def field (name: String, blocks: Block*) = Field(List(Text(name)), blocks.toList)
  
  
  def oli (name: String, value: Block*) = OptionListItem(List(ProgramOption(name, None)), value.toList)

  def oli (name: String, value: String) = OptionListItem(List(ProgramOption(name, None)), List(p(value)))

  def oli (name: String, argDelim: String, arg: String, value: String) = 
    OptionListItem(List(ProgramOption(name, Some(OptionArgument(arg,argDelim)))), List(p(value)))
  
  def optL (items: OptionListItem*) = OptionList(items.toList)
  
  
  
  "The bullet list parser" should "parse items that are not separated by blank lines" in {
    val input = """* aaa
      |* bbb
      |* ccc""".stripMargin
    Parsing (input) should produce (root( bulletList() + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items that are separated by blank lines" in {
    val input = """* aaa
      |
      |* bbb
      |
      |* ccc""".stripMargin
    Parsing (input) should produce (root( bulletList() + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items starting with a '+' the same way as those starting with a '*'" in {
    val input = """+ aaa
      |+ bbb
      |+ ccc""".stripMargin
    Parsing (input) should produce (root( bulletList("+") + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items starting with a '-' the same way as those starting with a '*'" in {
    val input = """- aaa
      |- bbb
      |- ccc""".stripMargin
    Parsing (input) should produce (root( bulletList("-") + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items containing multiple paragraphs in a single item" in {
    val input = """* aaa
      |   
      |  bbb
      |  bbb
      |
      |* ccc
      |
      |* ddd""".stripMargin
    Parsing (input) should produce (root( bulletList() + (p("aaa"), p("bbb\nbbb")) + fp("ccc") + fp("ddd")))
  }
  
  it should "parse nested items indented by spaces" in {
    val input = """* aaa
                  |
                  |  * bbb
                  |
                  |    * ccc""".stripMargin
    val list3 = bulletList() + "ccc"
    val list2 = bulletList() + (SpanSequence("bbb"), list3)
    val list1 = bulletList() + (SpanSequence("aaa"), list2)
    Parsing (input) should produce (root(list1))
  }
  
  it should "ignore items when the second line is not indented" in {
    val input = """* aaa
      |bbb""".stripMargin
    Parsing (input) should produce (root(p("* aaa\nbbb")))
  }
  
  it should "parse a literal block after the first line of a list item" in {
    val input = """* aaa::
      |   
      |   bbb
      |   bbb
      |
      |* ccc
      |
      |* ddd""".stripMargin
    Parsing (input) should produce (root( bulletList() + (p("aaa:"), LiteralBlock("bbb\nbbb")) + fp("ccc") + fp("ddd")))
  }
  
  
  "The enumerated list parser" should "parse items with arabic enumeration style" in {
    val input = """1. aaa
      |2. bbb
      |3. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(Arabic, "", ".")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items with lowercase alphabetic enumeration style" in {
    val input = """a. aaa
      |b. bbb
      |c. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(LowerAlpha, "", ".")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items with uppercase alphabetic enumeration style" in {
    val input = """A. aaa
      |B. bbb
      |C. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(UpperAlpha, "", ".")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items with lowercase Roman enumeration style" in {
    val input = """i. aaa
      |ii. bbb
      |iii. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(LowerRoman, "", ".")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items with uppercase Roman enumeration style" in {
    val input = """I. aaa
      |II. bbb
      |III. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(UpperRoman, "", ".")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "keep the right start value for arabic enumeration style" in {
    val input = """4. aaa
      |5. bbb""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(Arabic, "", "."), 4) + "aaa" + "bbb"))
  }
  
  it should "keep the right start value for lowercase alphabetic enumeration style" in {
    val input = """d. aaa
      |e. bbb""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(LowerAlpha, "", "."), 4) + "aaa" + "bbb"))
  }
  
  it should "keep the right start value for uppercase alphabetic enumeration style" in {
    val input = """D. aaa
      |E. bbb""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(UpperAlpha, "", "."), 4) + "aaa" + "bbb"))
  }
  
  it should "keep the right start value for lowercase Roman enumeration style" in {
    val input = """iv. aaa
      |v. bbb""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(LowerRoman, "", "."), 4) + "aaa" + "bbb"))
  }
  
  it should "keep the right start value for uppercase Roman enumeration style" in {
    val input = """IV. aaa
      |V. bbb""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(UpperRoman, "", "."), 4) + "aaa" + "bbb"))
  }
  
  it should "not try to parse a Roman Numeral in a normal paragraph (issue #19)" in {
    val input = "imp"
    Parsing (input) should produce (root(p("imp")))
  }
  
  it should "parse items suffixed by right-parenthesis" in {
    val input = """1) aaa
      |2) bbb
      |3) ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(Arabic, "", ")")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items surrounded by parenthesis" in {
    val input = """(1) aaa
      |(2) bbb
      |(3) ccc""".stripMargin
    Parsing (input) should produce(root(enumList(EnumFormat(Arabic, "(", ")")) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items that are separated by blank lines" in {
    val input = """1. aaa
      |
      |2. bbb
      |
      |3. ccc""".stripMargin
    Parsing (input) should produce (root(enumList(EnumFormat(Arabic)) + "aaa" + "bbb" + "ccc"))
  }
  
  it should "parse items containing multiple paragraphs in a single item" in {
    val input = """1. aaa
      |   
      |   bbb
      |   bbb
      |
      |2. ccc
      |
      |3. ddd""".stripMargin
    Parsing (input) should produce (root( enumList() + (p("aaa"), p("bbb\nbbb")) + fp("ccc") + fp("ddd")))
  }
  
  it should "parse nested items indented by spaces" in {
    val input = """1. aaa
                  |
                  |   1. bbb
                  |
                  |      1. ccc""".stripMargin
    val list3 = enumList() + "ccc"
    val list2 = enumList() + (SpanSequence("bbb"), list3)
    val list1 = enumList() + (SpanSequence("aaa"), list2)
    Parsing (input) should produce (root(list1))
  }
  
  it should "parse items with different enumeration patterns into separate lists" in {
    val input = """1. aaa
      |
      |2. bbb
      |
      |1) ccc
      |
      |2) ddd""".stripMargin
    val f = EnumFormat(Arabic,"",")")
    Parsing (input) should produce (root(enumList() + "aaa" + "bbb", enumList(f) + "ccc" + "ddd"))
  }
  
  
  
  "The definition list parser" should "parse items that are not separated by blank lines" in {
    val input = """term 1
      | aaa
      |term 2
      | bbb""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb"))))
  }
  
  it should "parse items that are separated by blank lines" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb"))))
  }
  
  it should "parse a term with a classifier" in {
    val input = """term 1
      | aaa
      |
      |term 2 : classifier
      | bbb""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + (List(Text("term 2 "), Classifier(List(Text("classifier")))), p("bbb"))))
  }
  
  it should "parse items containing multiple paragraphs in a single item" in {
    val input = """term 1
      |  aaa
      |  aaa
      |
      |  bbb
      |
      |term 2
      |  ccc""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa\naaa"), p("bbb")) + ("term 2", p("ccc"))))
  }
  
  it should "parse items containing multiple paragraphs with different identation in a single item" in {
    val input = """term 1
      |   aaa
      |   aaa
      |
      |  bbb
      |
      |term 2
      |  ccc""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", QuotedBlock("aaa\naaa"), p("bbb")) + ("term 2", p("ccc"))))
  }
  
  it should "support inline markup in the term" in {
    val input = """term *em*
      | aaa
      |
      |term 2
      | bbb""".stripMargin
    Parsing (input) should produce (root( defList + (List(Text("term "), Emphasized("em")), p("aaa")) + ("term 2", p("bbb"))))
  }
  
  it should "ignore subsequent tables" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb
      |
      |=== ===
      | a   b 
      |=== ===""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb")),
      Table(strrow("a","b"))))
  }
  
  it should "ignore subsequent directives" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb
      |
      |.. foo::
      | :name: value""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb")),
      InvalidElement("unknown directive: foo", ".. foo:: \n:name: value").asBlock))
  }
  
  it should "ignore subsequent bullet lists" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb
      |
      |* list
      |  list""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb")), 
        bulletList() + (p("list\nlist"))))
  }
  
  it should "ignore subsequent enum lists" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb
      |
      |1. list
      |   list""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb")), 
        enumList(EnumFormat(Arabic)) + (p("list\nlist"))))
  }
  
  it should "ignore subsequent headers with overline" in {
    val input = """term 1
      | aaa
      |
      |term 2
      | bbb
      |
      |########
      | Header
      |########""".stripMargin
    Parsing (input) should produce (root( defList + ("term 1", p("aaa")) + ("term 2", p("bbb")), 
        DecoratedHeader(OverlineAndUnderline('#'), List(Text("Header")), Id("header"))))
  }
  
  
  
  "The field list parser" should "parse a list with all bodies on the same line as the name" in {
    val input = """:name1: value1
      |:name2: value2
      |:name3: value3""".stripMargin
    Parsing (input) should produce (root( fl( field("name1", p("value1")), field("name2", p("value2")), field("name3", p("value3")))))
  }
  
  it should "parse a list with bodies spanning multiple lines" in {
    val input = """:name1: line1a
      |  line1b
      |:name2: line2a
      |  line2b""".stripMargin
    Parsing (input) should produce (root( fl( field("name1", p("line1a\nline1b")), field("name2", p("line2a\nline2b")))))
  }
  
  it should "parse a list with bodies spanning multiple blocks" in {
    val input = """:name1: line1a
      |  line1b
      |
      |  line1c
      |  line1d
      |:name2: line2a
      |  line2b""".stripMargin
    Parsing (input) should produce (root( fl( field("name1", p("line1a\nline1b"), p("line1c\nline1d")), field("name2", p("line2a\nline2b")))))
  }
  
  
  "The option list parser" should "parse a list with short posix options" in {
    val input = """-a  Option1
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", "Option1"), oli("-b", "Option2"))))
  }
  
  it should "parse a list with long posix options" in {
    val input = """--aaaa  Option1
      |--bbbb  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("--aaaa", "Option1"), oli("--bbbb", "Option2"))))
  }
  
  it should "parse a list with short GNU-style options" in {
    val input = """+a  Option1
      |+b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("+a", "Option1"), oli("+b", "Option2"))))
  }
  
  it should "parse a list with short DOS-style options" in {
    val input = """/a  Option1
      |/b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("/a", "Option1"), oli("/b", "Option2"))))
  }
  
  it should "parse an option argument separated by a space" in {
    val input = """-a FILE  Option1
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", " ", "FILE", "Option1"), oli("-b", "Option2"))))
  }
  
  it should "parse an option argument separated by '='" in {
    val input = """-a=FILE  Option1
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", "=", "FILE", "Option1"), oli("-b", "Option2"))))
  }

  it should "parse an option argument enclosed in angle brackets" in {
    val input = """-a <in=out>  Option1
                  |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", " ", "<in=out>", "Option1"), oli("-b", "Option2"))))
  }
  
  it should "parse a description starting on the next line" in {
    val input = """-a
      |    Option1
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", "Option1"), oli("-b", "Option2"))))
  }
  
  it should "parse a block of options with blank lines between them" in {
    val input = """-a  Option1
      |
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", "Option1"), oli("-b", "Option2"))))
  }
  
  it should "parse a description containing multiple paragraphs" in {
    val input = """-a  Line1
      |                Line2
      |
      |                Line3
      |
      |-b  Option2""".stripMargin
    Parsing (input) should produce (root( optL( oli("-a", p("Line1\nLine2"), p("Line3")), oli("-b", "Option2"))))
  }

  it should "parse an option separated by more than 2 spaces" in {
    val input = """-a   Option""".stripMargin
    Parsing (input) should produce (root( optL(
      oli("-a", "Option")
    )))
  }



  "The line block parser" should "parse a block with out continuation or indentation" in {
    val input = """|| Line1
      || Line2
      || Line3""".stripMargin
    Parsing (input) should produce (root( lb( Line("Line1"), Line("Line2"), Line("Line3"))))
  }
  
  it should "parse a block with a continuation line" in {
    val input = """|| Line1
      |  Line2
      || Line3
      || Line4""".stripMargin
    Parsing (input) should produce (root( lb( Line("Line1\nLine2"), Line("Line3"), Line("Line4"))))
  }
  
  it should "parse a nested structure (pointing right)" in {
    val input = """|| Line1
      ||   Line2
      ||     Line3
      ||   Line4
      || Line5""".stripMargin
    Parsing (input) should produce (root( lb( Line("Line1"), lb(Line("Line2"), lb(Line("Line3")), Line("Line4")), Line("Line5"))))
  }
  
  it should "parse a nested structure (pointing left)" in {
    val input = """||     Line1
      ||   Line2
      || Line3
      ||   Line4
      ||     Line5""".stripMargin
    Parsing (input) should produce (root( lb( lb( lb(Line("Line1")), Line("Line2")), Line("Line3"), lb(Line("Line4"), lb(Line("Line5"))))))
  }
  
  
}
