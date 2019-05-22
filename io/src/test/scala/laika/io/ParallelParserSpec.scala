/*
 * Copyright 2013-2016 the original author or authors.
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

package laika.io

import laika.api.{MarkupParser, Parse}
import laika.ast.DocumentType._
import laika.ast.Path.Root
import laika.ast._
import laika.ast.helper.DocumentViewBuilder._
import laika.ast.helper.ModelBuilder
import laika.bundle.{BundleProvider, ExtensionBundle}
import laika.format.{Markdown, ReStructuredText}
import laika.io.helper.InputBuilder
import laika.parse.Parser
import laika.parse.text.TextParsers
import laika.rewrite.TemplateRewriter
import org.scalatest.{FlatSpec, Matchers}


class ParallelParserSpec extends FlatSpec 
                   with Matchers
                   with ModelBuilder {

  
  it should "allow parsing Markdown from a file" in {
//    val input = """aaa
//      |bbb
//      |ccc""".stripMargin
//    val filename = getClass.getResource("/testInput.md").getFile
//    (Parse as Markdown fromFile filename).content should be (root(p(input))) 
  }
  
  it should "allow parsing Markdown from a java.io.Reader instance" ignore {
//    val input = """aaa
//      |bbb
//      |ccc""".stripMargin
//    val reader = new StringReader(input)
//    (Parse as Markdown fromReader reader).execute.content should be (root(p(input)))
  }
  
  it should "allow parsing Markdown from a java.io.InputStream instance" ignore {
//    val input = """aaa
//      |bbb
//      |ccc""".stripMargin
//    val stream = new ByteArrayInputStream(input.getBytes())
//    (Parse as Markdown fromStream stream).execute.content should be (root(p(input)))
  }
  
  it should "allow parsing Markdown from a java.io.InputStream instance, specifying the encoding explicitly" ignore {
//    val input = """äää
//      |ööö
//      |üüü""".stripMargin
//    val stream = new ByteArrayInputStream(input.getBytes("ISO-8859-1"))
//    (Parse as Markdown).fromStream(stream)(Codec.ISO8859).execute.content should be (root(p(input)))
  }
  
  it should "allow parsing Markdown from a java.io.InputStream instance, specifying the encoding implicitly" ignore {
//    val input = """äää
//      |ööö
//      |üüü""".stripMargin
//    val stream = new ByteArrayInputStream(input.getBytes("ISO-8859-1"))
//    implicit val codec:Codec = Codec.ISO8859
//    (Parse as Markdown fromStream stream).execute.content should be (root(p(input)))
  }
  
  trait TreeParser extends InputBuilder {
    
    def dirs: String 
    
    def contents = Map(
      "link" -> "[link](foo)",
      "name" -> "foo",
      "name2" -> "bar",
      "multiline" -> """aaa
        |
        |bbb""".stripMargin,
      "directive" -> "aa @:foo bar. bb",
      "template" -> """<div>
        |  {{document.content}}
        |</div>""".stripMargin,
      "template2" -> """<div>
        |xx{{document.content}}
        |</div>""".stripMargin,
      "dynDoc" -> "{{config.value}}",
      "conf" -> "value: abc",
      "order" -> """navigationOrder: [
        |  lemon.md
        |  shapes
        |  cherry.md
        |  colors
        |  apple.md
        |  orange.md
        |]""".stripMargin
    )
    
    val docTypeMatcher = MarkupParser.of(Markdown).or(ReStructuredText).config.docTypeMatcher
    
    def builder (source: String): TreeInput = parseTreeStructure(source, docTypeMatcher)
    
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p("foo"))) :: Nil)
    
    def customDocView (name: String, content: Seq[Block], path: Path = Root) = DocumentView(path / name, Content(content) :: Nil)
  
    def withTemplatesApplied (tree: DocumentTree): DocumentTree = TemplateRewriter.applyTemplates(tree, "html")
    
    def parsedTree = viewOf(withTemplatesApplied(MarkupParser.of(Markdown).fromTreeInput(builder(dirs)).execute.tree))
    
    def rawParsedTree = viewOf(MarkupParser.of(Markdown).withoutRewrite.fromTreeInput(builder(dirs)).execute)

    def rawMixedParsedTree = viewOf(MarkupParser.of(Markdown).or(ReStructuredText).withoutRewrite.fromTreeInput(builder(dirs)).execute)
    
    def parsedInParallel = viewOf(withTemplatesApplied(MarkupParser.of(Markdown).inParallel.fromTreeInput(builder(dirs)).execute.tree))

    def parsedWith (bundle: ExtensionBundle) =
      viewOf(withTemplatesApplied(MarkupParser.of(Markdown).using(bundle).fromTreeInput(builder(dirs)).execute.tree))
      
    def parsedRawWith (bundle: ExtensionBundle = ExtensionBundle.Empty, customMatcher: PartialFunction[Path, DocumentType] = PartialFunction.empty) =
      viewOf(MarkupParser.of(Markdown).withoutRewrite.using(bundle).fromTreeInput(parseTreeStructure(dirs, customMatcher.orElse({case path => docTypeMatcher(path)}))).execute)
  }
  

  
  it should "allow parsing an empty tree" in {
    new TreeParser {
      val dirs = ""
      val treeResult = TreeView(Root, Nil)
      parsedTree should be (treeResult)
    }
  }
  
  it should "allow parsing a tree with a single document" in {
    new TreeParser {
      val dirs = """- name.md:name"""
      val docResult = DocumentView(Root / "name.md", Content(List(p("foo"))) :: Nil)
      val treeResult = TreeView(Root, List(Documents(Markup, List(docResult))))
      parsedTree should be (treeResult)
    }
  }
  
  it should "allow parsing a tree with multiple subtrees" in {
    new TreeParser {
      val dirs = """- doc1.md:name
        |- doc2.md:name
        |+ dir1
        |  - doc3.md:name
        |  - doc4.md:name
        |+ dir2
        |  - doc5.md:name
        |  - doc6.md:name""".stripMargin
      val subtree1 = TreeView(Root / "dir1", List(Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1")))))
      val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2")))))
      val treeResult = TreeView(Root, List(
        Documents(Markup, List(docView(1),docView(2))),
        Subtrees(List(subtree1,subtree2))
      ))
      parsedTree should be (treeResult)
    }
  }
  
  it should "allow parsing a tree with a single template" in {
    new TreeParser {
      val dirs = """- main.template.html:name"""
      val template = TemplateView(Root / "main.template.html", TemplateRoot(List(TemplateString("foo"))))
      val treeResult = TreeView(Root, List(TemplateDocuments(List(template))))
      rawParsedTree should be (treeResult)
    }
  }
  
  it should "allow parsing a tree with a static document" ignore {
    new TreeParser {
      val dirs = """- omg.js:name"""
      val input = InputView("omg.js")
      val treeResult = TreeView(Root, List(Inputs(Static, List(input))))
      parsedTree should be (treeResult)
    }
  }
  
  it should "allow parsing a tree with all available file types" ignore {
    new TreeParser {
      val dirs = """- doc1.md:link
        |- doc2.rst:link
        |- mainA.template.html:name
        |+ dir1
        |  - mainB.template.html:name
        |  - doc3.md:name
        |  - doc4.md:name
        |+ dir2
        |  - main.dynamic.html:name
        |  - omg.js:name
        |  - doc5.md:name
        |  - doc6.md:name""".stripMargin
      def template (char: Char, path: Path) = TemplateView(path / s"main$char.template.html", TemplateRoot(List(TemplateString("foo"))))
      val dyn = TemplateView(Root / "dir2" / "main.dynamic.html", TemplateRoot(List(TemplateString("foo"))))
      val subtree1 = TreeView(Root / "dir1", List(
        Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1"))),
        TemplateDocuments(List(template('B', Root / "dir1")))
      ))
      val subtree2 = TreeView(Root / "dir2", List(
        Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2"))),
        Inputs(Static, List(InputView("omg.js")))
      ))
      val treeResult = TreeView(Root, List(
        Documents(Markup, List(customDocView("doc1.md", Seq(p(ExternalLink(Seq(txt("link")), "foo")))),customDocView("doc2.rst", Seq(p("[link](foo)"))))),
        TemplateDocuments(List(template('A', Root))),
        Subtrees(List(subtree1,subtree2))
      ))
      rawMixedParsedTree should be (treeResult)
    }
  }
  
  it should "allow to specify a custom document type matcher" ignore {
    // TODO - 0.12 - might need to become a file-system based test, as in-memory input do no longer use/need a docTypeMatcher
    new TreeParser {
      val dirs = """- name.md:name
        |- main.dynamic.html:name""".stripMargin
      val treeResult = TreeView(Root, List(Inputs(Static, List(InputView("name.md"), InputView("main.dynamic.html")))))
      parsedWith(BundleProvider.forDocTypeMatcher{ case _ => Static }) should be (treeResult)
    }
  }
  
  it should "allow to specify a custom template engine" in {
    new TreeParser {
      val parser: Parser[TemplateRoot] = TextParsers.any ^^ { str => TemplateRoot(List(TemplateString("$$" + str))) }
      val dirs = """- main1.template.html:name
        |- main2.template.html:name""".stripMargin
      def template (num: Int) = TemplateView(Root / (s"main$num.template.html"), TemplateRoot(List(TemplateString("$$foo"))))
      val treeResult = TreeView(Root, List(TemplateDocuments(List(template(1),template(2)))))
      parsedRawWith(BundleProvider.forTemplateParser(parser)) should be (treeResult)
    }
  }
  
  it should "allow to specify a custom style sheet engine" in {
    new TreeParser {
      override val docTypeMatcher: PartialFunction[Path, DocumentType] = { case path =>
        val Stylesheet = """.+\.([a,b]+).css$""".r
        path.name match {
          case Stylesheet(kind) => StyleSheet(kind)
        }
      }
      def styleDecl(styleName: String, order: Int = 0) =
        StyleDeclaration(StylePredicate.ElementType("Type"), styleName -> "foo").increaseOrderBy(order)
      val parser: Parser[Set[StyleDeclaration]] = TextParsers.any ^^ {n => Set(styleDecl(n))}
      val dirs = """- main1.aaa.css:name
        |- main2.bbb.css:name2
        |- main3.aaa.css:name""".stripMargin
      val treeResult = TreeView(Root, List(StyleSheets(Map(
          "aaa" -> StyleDeclarationSet(Set(Path("/main1.aaa.css"), Path("/main3.aaa.css")), Set(styleDecl("foo"), styleDecl("foo", 1))),
          "bbb" -> StyleDeclarationSet(Set(Path("/main2.bbb.css")), Set(styleDecl("bar")))
      ))))
      parsedRawWith(BundleProvider.forDocTypeMatcher(docTypeMatcher)
        .withBase(BundleProvider.forStyleSheetParser(parser))) should be (treeResult)
    }
  }
  
  it should "allow to specify a template directive" in {
    new TreeParser {
      import laika.directive.Templates
      import Templates.dsl._

      val directive = Templates.create("foo") {
        attribute(Default) map { TemplateString(_) }
      }
      val dirs = """- main1.template.html:directive
        |- main2.template.html:directive""".stripMargin
      def template (num: Int) = TemplateView(Root / (s"main$num.template.html"), tRoot(tt("aa "),tt("bar"),tt(" bb")))
      val treeResult = TreeView(Root, List(TemplateDocuments(List(template(1),template(2)))))
      parsedRawWith(BundleProvider.forTemplateDirective(directive)) should be (treeResult)
    }
  }
  
  it should "add indentation information if an embedded root is preceded by whitespace characters" in {
    new TreeParser {
      import laika.ast.EmbeddedRoot
      val dirs = """- default.template.html:template
        |- doc.md:multiline""".stripMargin
      val docResult = DocumentView(Root / "doc.md", Content(List(tRoot(
          tt("<div>\n  "),
          EmbeddedRoot(List(p("aaa"),p("bbb")), 2),
          tt("\n</div>")
      ))) :: Nil)
      val treeResult = TreeView(Root, List(Documents(Markup, List(docResult))))
      parsedTree should be (treeResult)
    }
  }
  
  it should "not add indentation information if an embedded root is preceded by non-whitespace characters" in {
    new TreeParser {
      import laika.ast.EmbeddedRoot
      val dirs = """- default.template.html:template2
        |- doc.md:multiline""".stripMargin
      val docResult = DocumentView(Root / "doc.md", Content(List(tRoot(
        tt("<div>\nxx"),
        EmbeddedRoot(List(p("aaa"),p("bbb")), 0),
        tt("\n</div>")
      ))) :: Nil)
      val treeResult = TreeView(Root, List(Documents(Markup, List(docResult))))
      parsedTree should be (treeResult)
    }
  }
  
  it should "allow to specify a custom navigation order" in {
    new TreeParser {
      val dirs = """- apple.md:name
        |- orange.md:name
        |+ colors
        |  - green.md:name
        |- lemon.md:name
        |+ shapes
        |  - rectangle.md:name
        |- cherry.md:name
        |- directory.conf:order""".stripMargin
      val tree = MarkupParser.of(Markdown) fromTreeInput builder(dirs)
      tree.execute.tree.content map (_.path.name) should be (List("lemon.md","shapes","cherry.md","colors","apple.md","orange.md"))
    }
  }

  it should "always move title documents to the front, even with a custom navigation order" in {
    new TreeParser {
      val dirs = """- apple.md:name
                   |- orange.md:name
                   |+ colors
                   |  - green.md:name
                   |- lemon.md:name
                   |- title.md:name
                   |+ shapes
                   |  - rectangle.md:name
                   |- cherry.md:name
                   |- directory.conf:order""".stripMargin
      val tree = MarkupParser.of(Markdown).fromTreeInput(builder(dirs)).execute.tree
      
      tree.titleDocument.map(_.path.basename) shouldBe Some("title")
      
      tree.content map (_.path.name) should be (List("lemon.md","shapes","cherry.md","colors","apple.md","orange.md"))
      tree.content map (_.position) should be (List(
        TreePosition(Seq(1)),
        TreePosition(Seq(2)),
        TreePosition(Seq(3)),
        TreePosition(Seq(4)),
        TreePosition(Seq(5)),
        TreePosition(Seq(6)),
      ))
    }
  }
  
  it should "allow parallel parser execution" in {
    new TreeParser {
      val dirs = """- doc1.md:name
        |- doc2.md:name
        |+ dir1
        |  - doc3.md:name
        |  - doc4.md:name
        |  - doc5.md:name
        |+ dir2
        |  - doc6.md:name
        |  - doc7.md:name
        |  - doc8.md:name""".stripMargin
      val subtree1 = TreeView(Root / "dir1", List(Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1"),docView(5, Root / "dir1")))))
      val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(6, Root / "dir2"),docView(7, Root / "dir2"),docView(8, Root / "dir2")))))
      val treeResult = TreeView(Root, List(
        Documents(Markup, List(docView(1),docView(2))),
        Subtrees(List(subtree1,subtree2))
      ))
      parsedInParallel should be (treeResult)
    }
  }
  
  it should "read a directory from the file system using the fromDirectory method" in {
    val dirname = getClass.getResource("/trees/a/").getFile
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p("Doc"+num))) :: Nil)
    val subtree1 = TreeView(Root / "dir1", List(Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1")))))
    val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2")))))
    val treeResult = TreeView(Root, List(
      Documents(Markup, List(docView(1),docView(2))),
      Subtrees(List(subtree1,subtree2))
    ))
    viewOf(MarkupParser.of(Markdown).fromDirectory(dirname).execute) should be (treeResult)
  }
  
  it should "read a directory from the file system using the fromDirectories method" in {
    val dir1 = new java.io.File(getClass.getResource("/trees/a/").getFile)
    val dir2 = new java.io.File(getClass.getResource("/trees/b/").getFile)
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p("Doc"+num))) :: Nil)
    val subtree1 = TreeView(Root / "dir1", List(Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1"),docView(7, Root / "dir1")))))
    val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2")))))
    val subtree3 = TreeView(Root / "dir3", List(Documents(Markup, List(docView(8, Root / "dir3")))))
    val treeResult = TreeView(Root, List(
      Documents(Markup, List(docView(1),docView(2),docView(9))),
      Subtrees(List(subtree1,subtree2,subtree3))
    ))
    viewOf(MarkupParser.of(Markdown).fromDirectories(Seq(dir1,dir2)).execute) should be (treeResult)
  }

  it should "read a directory from the file system containing a file with non-ASCII characters" in {
    val dirname = getClass.getResource("/trees/c/").getFile
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p(s"Doc$num äöü"))) :: Nil)
    val treeResult = TreeView(Root, List(
      Documents(Markup, List(docView(1)))
    ))
    viewOf(MarkupParser.of(Markdown).fromDirectory(dirname).execute) should be (treeResult)
  }
  
  it should "allow to specify a custom exclude filter" in {
    val dirname = getClass.getResource("/trees/a/").getFile
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p("Doc"+num))) :: Nil)
    val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2")))))
    val treeResult = TreeView(Root, List(
      Documents(Markup, List(docView(2))),
      Subtrees(List(subtree2))
    ))
    viewOf(MarkupParser.of(Markdown).fromDirectory(dirname, {f:java.io.File => f.getName == "doc1.md" || f.getName == "dir1"}).execute) should be (treeResult)
  }
  
  it should "read a directory from the file system using the Directory object" in {
    val dirname = getClass.getResource("/trees/a/").getFile
    def docView (num: Int, path: Path = Root) = DocumentView(path / (s"doc$num.md"), Content(List(p("Doc"+num))) :: Nil)
    val subtree1 = TreeView(Root / "dir1", List(Documents(Markup, List(docView(3, Root / "dir1"),docView(4, Root / "dir1")))))
    val subtree2 = TreeView(Root / "dir2", List(Documents(Markup, List(docView(5, Root / "dir2"),docView(6, Root / "dir2")))))
    val treeResult = TreeView(Root, List(
      Documents(Markup, List(docView(1),docView(2))),
      Subtrees(List(subtree1,subtree2))
    ))
    viewOf(MarkupParser.of(Markdown).fromDirectory(dirname).execute) should be (treeResult)
  }

}
