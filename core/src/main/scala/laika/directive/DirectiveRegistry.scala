/*
 * Copyright 2013-2018 the original author or authors.
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

package laika.directive

import laika.bundle.ExtensionBundle

/** Registry for custom directives. Application code can define
  * any number of instances mixing in this trait and then pass
  * them to Parse, Render or Transform operations:
  *
  * {{{
  * object MyDirectives extends DirectiveRegistry {
  *   val spanDirectives = Seq(...)
  *   val blockDirectives = Seq(...)
  *   val templateDirectives = Seq(...)
  * }
  * object OtherDirectives extends DirectiveRegistry {
  *   [...]
  * }
  *
  * Transform
  *   .from(Markdown)
  *   .to(HTML)
  *   .using(MyDirectives, OtherDirectives)
  *   .fromFile("hello.md")
  *   .toFile("hello.html")
  * }}}
  *
  * @author Jens Halm
  */
trait DirectiveRegistry extends ExtensionBundle {

  /**  Registers the specified span directives.
    *
    *  Example:
    *
    *  {{{
    *  object MyDirectives extends DirectiveRegistry {
    *    val spanDirectives = Seq(
    *      Spans.create("ticket") {
    *        (defaultAttribute.as[String] ~ attribute("param").optional).map { case ticketNo ~ param =>
    *          val base = "http://tickets.service.com/"+ticketNo
    *          val url = base + (param map (p => "?param="+p) getOrElse "")
    *          ExternalLink(Seq(Text("Ticket "+ticketNo)), url, options = Styles("ticket"))
    *        }
    *      }
    *    )
    *    val blockDirectives = Seq()
    *    val templateDirectives = Seq()
    *  }
    *
    *  val transformer = Transformer.from(Markdown).to(HTML).using(MyDirectives).build
    *  }}}
    *
    *  The code above registers a span directive that detects markup like
    *  `@:ticket 2356.` and turns it into an external link node for the
    *  URL `http://tickets.service.com/2356`.
    *
    *  For more details on implementing Laika directives see [[laika.directive.BuilderContext.dsl]].
    */
  def spanDirectives: Seq[Spans.Directive]

  /**  Registers the specified block directives.
    *
    *  Example:
    *
    *  {{{
    *  case class Note (title: String, content: Seq[Block], options: Options = NoOpt)
    *                                                       extends Block with BlockContainer[Note]
    *  object MyDirectives extends DirectiveRegistry {
    *    val blockDirectives = Seq(
    *      Blocks.create("note") {
    *        (defaultAttribute.as[String] ~ body).map { case title ~ content => Note(title, content) }
    *      }
    *    )
    *    val spanDirectives = Seq()
    *    val templateDirectives = Seq()
    *  }
    *
    *  val transformer = Transformer.from(Markdown).to(HTML).using(MyDirectives).build
    *  }}}
    *
    *  For more details on implementing Laika directives see [[laika.directive.BuilderContext.dsl]].
    */
  def blockDirectives: Seq[Blocks.Directive]

  /**  Registers the specified template directives.
    *
    *  Example:
    *
    *  {{{
    *  object MyDirectives extends DirectiveRegistry {
    *    val templateDirectives = Seq(
    *      Templates.create("ticket") {
    *        (defaultAttribute.as[String] ~ attribute("param").optional).map { case ticketNo ~ param =>
    *          val base = "http://tickets.service.com/"+ticketNo
    *          val url = base + (param map (p => "&param="+p) getOrElse "")
    *          TemplateElement(ExternalLink(Seq(Text("Ticket "+ticketNo)), url, options = Styles("ticket")))
    *        }
    *      }
    *    )
    *    val blockDirectives = Seq()
    *    val spanDirectives = Seq()
    *  }
    *
    *  val transformer = Transformer.from(Markdown).to(HTML).using(MyDirectives).build
    *  }}}
    *
    *  The code above registers a template directive that detects markup like
    *  `@:ticket 2356.` and turns it into an external link node for the
    *  URL `http://tickets.service.com/2356`.
    *
    *  For more details on implementing Laika directives see [[laika.directive.BuilderContext.dsl]].
    */
  def templateDirectives: Seq[Templates.Directive]


  override def processExtension: PartialFunction[ExtensionBundle, ExtensionBundle] = {
    case ds: DirectiveSupport => ds.withDirectives(blockDirectives, spanDirectives, templateDirectives)
  }

}
