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

package laika.render.epub

import laika.ast.Path.Root
import laika.ast._
import laika.io.model.{RenderedDocument, RenderedTree}

/** Represents a recursive book navigation structure.
  */
trait NavigationItem extends Block with ElementContainer[NavigationItem] with RewritableContainer {
  type Self <: NavigationItem
  def title: SpanSequence
}

/** Represents a book navigation entry that only serves as a section header without linking to content.
  */
case class NavigationHeader (title: SpanSequence, content: Seq[NavigationItem], options: Options = NoOpt) extends NavigationItem {
  type Self = NavigationHeader
  def rewriteChildren (rules: RewriteRules): NavigationHeader = copy(
    title = title.rewriteChildren(rules), 
    content = content.map(_.rewriteChildren(rules))
  )
  def withOptions (options: Options): NavigationHeader = copy(options = options)
}

/** Represents a book navigation entry that links to content in the document tree.
  */
case class NavigationLink (title: SpanSequence, target: Target, content: Seq[NavigationItem], options: Options = NoOpt) extends NavigationItem {
  type Self = NavigationLink
  def rewriteChildren (rules: RewriteRules): NavigationLink = copy(
    title = title.rewriteChildren(rules),
    content = content.map(_.rewriteChildren(rules))
  )
  def withOptions (options: Options): NavigationLink = copy(options = options)
}

object NavigationItem {

  /** Provides the full path to the document relative to the EPUB container root
    * from the specified virtual path of the Laika document tree.
    */
  def fullPath (path: Path, forceXhtml: Boolean = false): String = {
    val finalPath = if (forceXhtml || path.suffix.contains("html")) path.withSuffix("epub.xhtml") else path
    val parent = finalPath.parent match {
      case Root => ""
      case _ => finalPath.parent.toString
    }
    s"content$parent/${finalPath.name}"
  }

  def forTree (tree: RenderedTree, depth: Int): Seq[NavigationItem] = {

    def hasContent (level: Int)(nav: Navigatable): Boolean = nav match {
      case _: RenderedDocument => true
      case tree: RenderedTree => if (level > 0) (tree.titleDocument.toSeq ++ tree.content).exists(hasContent(level - 1)) else false
    }

    def forSections (path: Path, sections: Seq[SectionInfo], levels: Int): Seq[NavigationItem] =
      if (levels == 0) Nil
      else for (section <- sections) yield {
        val title = section.title
        val children = forSections(path, section.content, levels - 1)
        NavigationLink(title, Target.create(fullPath(path, forceXhtml = true) + "#" + section.id), children)
      }

    if (depth == 0) Nil
    else {
      for (nav <- tree.content if hasContent(depth - 1)(nav)) yield nav match {
        case doc: RenderedDocument =>
          val title = doc.title.getOrElse(SpanSequence(doc.name)) 
          val children = forSections(doc.path, doc.sections, depth - 1)
          NavigationLink(title, Target.create(fullPath(doc.path, forceXhtml = true)), children)
        case subtree: RenderedTree =>
          val title = subtree.title.getOrElse(SpanSequence(subtree.name))
          val children = forTree(subtree, depth - 1)
          val targetDoc = subtree.titleDocument.orElse(subtree.content.collectFirst{ case d: RenderedDocument => d }).get
          val link = Target.create(fullPath(targetDoc.path, forceXhtml = true))
          if (depth == 1 || subtree.titleDocument.nonEmpty) NavigationLink(title, link, children)
          else NavigationHeader(title, children)
      }
    }
  }
  
}
