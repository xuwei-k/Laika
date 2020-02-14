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

package laika.parse.code.languages

import cats.data.NonEmptyList
import laika.bundle.SyntaxHighlighter
import laika.parse.builders._
import laika.parse.code.{CodeCategory, CodeSpanParser}
import laika.parse.code.common.{Comment, Identifier, Keywords, NumberLiteral, StringLiteral}

/**
  * @author Jens Halm
  */
object SQLSyntax extends SyntaxHighlighter {

  val language: NonEmptyList[String] = NonEmptyList.of("sql")
  
  val singleQuoteEscape: CodeSpanParser = CodeSpanParser(CodeCategory.EscapeSequence)(literal("''"))
  val doubleQuoteEscape: CodeSpanParser = CodeSpanParser(CodeCategory.EscapeSequence)(literal("\"\""))
  
  private def caseInsensitiveKeywords(category: CodeCategory)(kws: String*): CodeSpanParser = {
    val upper = kws.map(_.toUpperCase) // mixed case rare enough in practice to get ignored here
    Keywords(category)(kws.head, kws.tail: _*) ++ 
    Keywords(category)(upper.head, upper.tail: _*)
  }

  val keywords: CodeSpanParser = caseInsensitiveKeywords(CodeCategory.Keyword)(
    "absolute", "action", "add", "all", "allocate", "alter", "and", "any", "are", "as", "asc", "assertion", "at", 
    "authorization", "avg", "begin", "between", "bit", "bit_length", "both", "by", "cascade", "cascaded", "case", 
    "cast", "catalog", "char", "character", "char_length", "character_length", "check", "close", "coalesce", 
    "collate", "collation", "column", "commit", "connect", "connection", "constraint", "constraints", "continue", 
    "convert", "corresponding", "count", "create", "cross", "current", "current_date", "current_time", 
    "current_timestamp", "current_user", "cursor", "date", "day", "deallocate", "dec", "decimal", "declare", 
    "default", "deferrable", "deferred", "delete", "desc", "describe", "descriptor", "diagnostics", "disconnect", 
    "distinct", "domain", "double", "drop", "else", "end", "end-exec", "escape", "except", "exception", "exec", 
    "execute", "exists", "external", "extract", "false", "fetch", "first", "float", "for", "foreign", "found", 
    "from", "full", "get", "global", "go", "goto", "grant", "group", "having", "hour", "identity", "immediate", 
    "in", "indicator", "initially", "inner", "input", "insensitive", "insert", "int", "integer", "intersect", 
    "interval", "into", "is", "isolation", "join", "key", "language", "last", "leading", "left", "level", "like", 
    "local", "lower", "match", "max", "min", "minute", "module", "month", "names", "national", "natural", "nchar", 
    "next", "no", "not", "null", "nullif", "numeric", "octet_length", "of", "on", "only", "open", "option", "or", 
    "order", "outer", "output", "overlaps", "pad", "partial", "position", "precision", "prepare", "preserve", 
    "primary", "prior", "privileges", "procedure", "public", "read", "real", "references", "relative", "restrict", 
    "revoke", "right", "rollback", "rows", "schema", "scroll", "second", "section", "select", "session", 
    "session_user", "set", "size", "smallint", "some", "space", "sql", "sqlcode", "sqlerror", "sqlstate", 
    "substring", "sum", "system_user", "table", "temporary", "then", "time", "timestamp", "timezone_hour", 
    "timezone_minute", "to", "trailing", "transaction", "translate", "translation", "trim", "true", "union", 
    "unique", "unknown", "update", "upper", "usage", "user", "using", "value", "values", "varchar", "varying", 
    "view", "when", "whenever", "where", "with", "work", "write", "year", "zone"
  )

  val dataTypes: CodeSpanParser = caseInsensitiveKeywords(CodeCategory.TypeName)(
    "array", "bigint", "binary", "bit", "blob", "bool", "boolean", "char", "character", "date", "dec", "decimal", 
    "float", "int", "int8", "integer", "interval", "number", "numeric", "real", "record", "serial", "serial8", 
    "smallint", "text", "time", "timestamp", "tinyint", "varchar", "varchar2", "varying", "void"
  )

  val boolean: CodeSpanParser = caseInsensitiveKeywords(CodeCategory.BooleanLiteral)("true", "false")
  val literalValue: CodeSpanParser = caseInsensitiveKeywords(CodeCategory.LiteralValue)("null", "unknown")
  
  val spanParsers: Seq[CodeSpanParser] = Seq(
    Comment.singleLine("--"),
    Comment.singleLine("#"),
    Comment.multiLine("/*", "*/"),
    StringLiteral.singleLine('\'').embed(singleQuoteEscape),
    StringLiteral.singleLine('"').embed(doubleQuoteEscape),
    StringLiteral.singleLine('`').withCategory(CodeCategory.Identifier),
    NumberLiteral.hex,
    NumberLiteral.decimalFloat,
    NumberLiteral.decimalInt,
    dataTypes,
    boolean,
    literalValue,
    keywords,
    Identifier.alphaNum.withIdPartChars('_')
  )

}
