package org.scalameter.execution.invocation

import java.lang.reflect.Method
import java.util.regex.Pattern
import org.objectweb.asm.Type
import scala.util.matching.Regex


/** Object that matches the methods whose invocations should be counted.
 *
 *  @param classMatcher matches full class name given in internal format ('/' instead of a '.' as a separator)
 *  @param methodMatcher matches a specific method
 */
case class InvocationCountMatcher(classMatcher: InvocationCountMatcher.ClassMatcher, methodMatcher: InvocationCountMatcher.MethodMatcher) {
  def classMatches(className: String): Boolean = classMatcher.matches(className)

  def methodMatches(methodName: String, methodDescriptor: String): Boolean = methodMatcher.matches(methodName, methodDescriptor)
}

object InvocationCountMatcher {
  /** Mixin used for selecting classes whose methods will be checked against [[MethodMatcher]] and if matched,
   *  counted by a method invocation counting measurer.
   */
  sealed trait ClassMatcher {
    /** Matches class name given in a standard format ('.' as a package separator).
     *
     *  @param className class name that is matched
     */
    def matches(className: String): Boolean
  }

  object ClassMatcher {
    /** Matches class with a class name given as a string. */
    case class ClassName(clazz: String) extends ClassMatcher {
      def matches(className: String): Boolean = className == clazz
    }

    object ClassName {
      def apply(clazz: Class[_]) = new ClassName(clazz.getName)
    }

    /** Matches class with a regex.
      *
      *  Note that package separation in `regex` should be done by escaping '.'
      *  {{{
      *    val pattern = "java\\.lang\\.String".r.pattern
      *  }}}
      */
    case class Regex(regex: Pattern) extends ClassMatcher {
      def matches(className: String): Boolean = regex.matcher(className).matches()
    }
  }
  
  /** Mixin used for selecting methods whose invocations should be counted by a method invocation counting measurer.
   *
   *  Note that selecting classes for which every method will be checked against `matches` method is done by [[ClassMatcher]].
   */
  sealed trait MethodMatcher {
    /**  Matches method given in a internal jvm format.
      *
      *  Note that only [[MethodMatcher.Full]] matcher actually matches `methodDescriptor`.
      *
      *  @param methodName method name that is matched
      *  @param methodDescriptor method descriptor in format specified by a JVM spec
      *
      *  @see https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3
      */
    def matches(methodName: String, methodDescriptor: String): Boolean
  }
  
  object MethodMatcher {
    /** Matches method with a method name given as a string. */
    case class MethodName(method: String) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean = methodName == method
    }

    /** Matches method with a regex. */
    case class Regex(regex: Pattern) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean = regex.matcher(methodName).matches()
    }

    /** Matches method with a [[java.lang.reflect.Method]].
      *
      *  That means that method name, method arguments and method return type are matched.
      */
    case class Full(method: Method) extends MethodMatcher {
      def matches(methodName: String, methodDescriptor: String): Boolean =
        methodName == method.getName && methodDescriptor == Type.getMethodDescriptor(method)
    }  
  }

  /** Matches allocations of a class. */
  def allocations(clazz: Class[_]) = 
    new InvocationCountMatcher(ClassMatcher.ClassName(clazz), MethodMatcher.MethodName("<init>"))

  /** Matches method name in a class. */
  def forName(className: String, methodName: String) =
    new InvocationCountMatcher(ClassMatcher.ClassName(className), MethodMatcher.MethodName(methodName))
  
  /** Matches method with a [[java.lang.reflect.Method]] in a class. */
  def forClass(clazz: Class[_], method: Method) =
    new InvocationCountMatcher(ClassMatcher.ClassName(clazz), MethodMatcher.Full(method))

  /** Matches method name with a regex in classes matched by a regex. */
  def forRegex(classRegex: Regex, methodRegex: Regex) =
    new InvocationCountMatcher(ClassMatcher.Regex(classRegex.pattern), MethodMatcher.Regex(methodRegex.pattern))
}