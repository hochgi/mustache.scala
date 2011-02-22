package org.monkey.mustache

/**
 * Dictionaries encapsulate variable mapping & lookup. We enforce
 * structure here: keys can only be data values or further mappings.
 */

object Dictionary {
  /**
   * Create an empty dictionary.
   */
  def apply() = new Dictionary(Map())
}

case class Dictionary private(
  private val mappings: Map[String, Value],
  private val parent: Option[Dictionary])
{
  def this(mappings: Map[String, Value]) = this(mappings, None)

  /**
   * Add data with the key @name@ to this dictionary.
   */
  def data(name: String, value: String) =
    copy(mappings = mappings + (name -> Data(value)))

  /**
   * Add a dictionary with the key @name@.
   */
  def dictionary(name: String, value: Dictionary) =
    mappings.get(name) match {
      case None | Some(Data(_)) =>
        copy(mappings = mappings + (name -> Dictionaries(Seq(value))))
      case Some(Dictionaries(dicts)) =>
        copy(mappings = mappings + (name -> Dictionaries(dicts ++ Seq(value))))
    }

  /**
   * Look up the dictionary value with key @name@. This looks up the
   * stack until a value is found, failing (with a None value) if the
   * search yields no mapping.
   */
  def apply(name: String): Option[Value] =
    mappings.get(name) match {
      case v@Some(_) => v
      case None => parent flatMap { _(name) }
    }

  /**
   * Push the lookup stack with the given @name@, returning the
   * resulting dictionaries. If the value with key @name@ doesn't
   * exist, the returned sequence is empty.
   */
  def push(name: String): Seq[Dictionary] = {
    val dictionaries = this(name) flatMap { _.getDictionaries }
    dictionaries.toSeq flatMap { _.dictionaries } map { dict =>
      dict.copy(parent = Some(this))
    }
  }
  
}

/**
 * The value hierarchy: values in @Dictionary@ are either @Data@,
 * which carry values, or a sequence of further dictionaries
 * encapsulated in @Dictionaries@
 */

sealed trait Value {
  def getData = this match {
    case v@Data(_) => Some(v)
    case Dictionaries(_) => None
  }
  
  def getDictionaries = this match {
    case Data(_) => None
    case d@Dictionaries(_) => Some(d)
  }
}

case class Data(data: String) extends Value
case class Dictionaries(dictionaries: Seq[Dictionary]) extends Value