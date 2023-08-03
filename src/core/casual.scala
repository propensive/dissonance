/*
    Dissonance, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package dissonance

import rudiments.*
import fulminate.*
import anticipation.*

case class CasualDiffError(issue: CasualDiffError.Issue, line: Int)
extends Error(msg"the diff could not be read because $issue at line $line")

object CasualDiffError:
  enum Issue:
    case BadLineStart(content: Text)
    case DoesNotMatch(content: Text)
  
  given AsMessage[Issue] =
    case Issue.BadLineStart(content) =>
      msg"the line $content did not begin with either ${"'+ '".tt}, ${"'- '".tt} or ${"'  '".tt}"
    
    case Issue.DoesNotMatch(content) =>
      msg"the line $content could not be found in the document"

case class Replace(context: List[Text], original: List[Text], replacement: List[Text])

object CasualDiff:
  def parse(stream: LazyList[Text]): CasualDiff throws CasualDiffError =
    def recur
        (stream: LazyList[Text], context: List[Text], original: List[Text], replacement: List[Text],
            done: List[Replace], lineNo: Int)
        : List[Replace] =
      
      stream match
        case head #:: tail =>
          if head.s.startsWith("  ") then
            if !original.isEmpty || !replacement.isEmpty
            then
              val replace = Replace(context.reverse, original.reverse, replacement.reverse)
              recur(tail, List(head.s.drop(2).tt), Nil, Nil, replace :: done, lineNo + 1)
            else recur(tail, head.s.drop(2).tt :: context, Nil, Nil, done, lineNo + 1)
          else if head.s.startsWith("+ ") then
            recur(tail, context, original, head.s.drop(2).tt :: replacement, done, lineNo + 1)
          else if head.s.startsWith("- ") then
            if !replacement.isEmpty
            then
              val replace = Replace(context.reverse, original.reverse, replacement.reverse)
              recur(tail, Nil, List(head.s.drop(2).tt), Nil, replace :: done, lineNo + 1)
            else recur(tail, Nil, head.s.drop(2).tt :: original, Nil, done, lineNo + 1)
          else throw CasualDiffError(CasualDiffError.Issue.BadLineStart(head), lineNo)
        
        case _ =>
          (Replace(context.reverse, original.reverse, replacement.reverse) :: done).reverse

    CasualDiff(recur(stream, Nil, Nil, Nil, Nil, 1))

case class CasualDiff(replacements: List[Replace]):
  def applyTo(original: Iterable[Text]): LazyList[Text] throws CasualDiffError =
    def recur
        (stream: LazyList[Text], focus: List[Text], todo: List[Replace], lineNo: Int)
        : LazyList[Text] =
      todo match
        case Nil =>
          LazyList()
          
        case Replace(Nil, original, replacement) :: tail => focus match
          case Nil => tail match
            case Replace(context, next, _) :: tail =>
              val lineNo2 = lineNo + original.length + replacement.length
              replacement.to(LazyList) #::: recur(stream, next, todo.tail, lineNo2)
            
            case Nil =>
              replacement.to(LazyList) #::: stream
  
          case line :: rest => stream match
            case head #:: tail =>
              if head == line then recur(tail, rest, todo, lineNo)
              else original.dropRight(focus.length).to(LazyList) #::: head #::
                  recur(tail, original, todo, lineNo)
            
            case _ =>
              val lineNo2 = lineNo + original.length - focus.length
              throw CasualDiffError(CasualDiffError.Issue.DoesNotMatch(line), lineNo2)
        
        case Replace(line :: rest, original, replacement) :: todoTail => stream match
          case head #:: tail =>
            head #:: {
              if head != line then recur(tail, focus, todo, lineNo + 1)
              else recur(tail, focus, Replace(rest, original, replacement) :: todoTail, lineNo + 1)
            }
    
    if replacements.isEmpty then original.to(LazyList)
    else recur(original.to(LazyList), replacements.head.original, replacements, 1)
  
  def serialize: LazyList[Text] = replacements.to(LazyList).flatMap:
    case Replace(context, original, replacement) =>
      (context.to(LazyList).map("  "+_) #::: original.to(LazyList).map("- "+_) #:::
          replacement.to(LazyList).map("+ "+_)).map(_.tt)
