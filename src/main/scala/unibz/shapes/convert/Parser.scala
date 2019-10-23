package unibz.shapes.convert

import java.io.File
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.collect.{ImmutableList, ImmutableSet}
import es.weso.rdf.nodes.{IRI, RDFNode}
import es.weso.rdf.path.{AlternativePath, InversePath, PredicatePath, SHACLPath, SequencePath, ZeroOrMorePath}
import es.weso.schema.{Schemas, ShaclexSchema}
import es.weso.shacl._
import unibz.shapes.shape.impl._
import unibz.shapes.shape.{Schema => _, Shape => _, _}
import unibz.shapes.util.ImmutableCollectors

object Parser {

  final class RDFParserException(str: String) extends Exception(str)

  @throws(classOf[RDFParserException])
  private[this] def getIRI(node: RDFNode): String = {
    node.toIRI match {
      case Left(s) => throw new RDFParserException("node " + node + " is not an IRI")
      case Right(i) => i.toString()
    }
  }

  private[this] def translateTargetDef(t: Target): Option[String] = {
    t match {
      case TargetClass(node: RDFNode) => Some("?x  rdf:type/rdfs:subClassOf* " + getIRI(node))
      case TargetSubjectsOf(pred: IRI) => Some("?x  " + pred.toString() + " ?y")
      case TargetObjectsOf(pred: IRI) => Some("?y  " + pred.toString() + " ?x")
      case _ => None
    }
  }

  private[this] def translateTargetNodeDefs(s: Seq[TargetNode]): Option[String] =
    s.isEmpty match {
      case true => None
      case false => Some("VALUES (?x) {\n" +
        s
        .map(e => "(" + e.node.toString + ")")
        .reduce((s1, s2) => s1 + "\n" + s2) + "\n}"
    )
  }

  private[this] def getTargetQuery(shape: NodeShape): Optional[String] = {
    if (shape.targets.isEmpty)
      return Optional.empty()

    val (targetNodeDefs, otherTargetDefs) = shape.targets
      .partition(t => t.isInstanceOf[TargetNode])
    val s: Option[String] = translateTargetNodeDefs(targetNodeDefs
      .map(e => e.asInstanceOf[TargetNode])
    )
    val triplePatterns: Seq[Option[String]] = otherTargetDefs
      .map(t => translateTargetDef(t)) :+ s

    return Optional.of(
      "SELECT ?x WHERE {\n" +
        triplePatterns
          .flatten
          .reduce((s1, s2) => "{\n " + s1 + "\n} UNION {\n" + s2 + " \n}")
        + "\n}")
  }

  private[this] def isNamedNodeShape(s: Shape): Boolean = {
    if (s.isInstanceOf[NodeShape] && s.id.isIRI)
      return true
    false
  }

  private[this] def getUniqueNot(components: Seq[Component]): Option[Not] = {
    components
      .filter(c => c.isInstanceOf[Not]) match {
      case x if x.isEmpty =>
        None
      case x if x.size > 1 =>
        throw new RDFParserException("Multiple sh:not components within :\n" + components.toString)
      case x =>
        Some(x.head.asInstanceOf[Not])
    }
  }

  private[this] def getUniqueQual(components: Seq[Component]): Option[QualifiedValueShape] = {
    components
      .filter(c => c.isInstanceOf[QualifiedValueShape]) match {
      case x if x.isEmpty =>
        None
      case x if x.size > 1 =>
        throw new RDFParserException("Multiple sh:qualifiedValueShape components within :\n" + components.toString)
      case x =>
        Some(x.head.asInstanceOf[QualifiedValueShape])
    }
  }

  private[this] def getMinCard(s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Option[Int] = {
    val minSeq = s.components
      .filter(c => c.isInstanceOf[MinCount])
    if (minSeq.size > 1)
      throw new RDFParserException("Only one sh:minCount is expected\n" + s.toString)
    if (minSeq.size == 1)
      return Some(minSeq.head.asInstanceOf[MinCount].value)
    getUniqueQual(s.components) match {
      case Some(q: QualifiedValueShape) => q.qualifiedMinCount
      case None => None
    }
  }

  private[this] def getMaxCard(s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Option[Int] = {
    val maxSeq = s.components
      .filter(c => c.isInstanceOf[MaxCount])
    if (maxSeq.size > 1)
      throw new RDFParserException("Only one sh:maxCount is expected\n" + s.toString)
    if (maxSeq.size == 1)
      return Some(maxSeq.head.asInstanceOf[MaxCount].value)
    getUniqueQual(s.components) match {
      case Some(q: QualifiedValueShape) => q.qualifiedMaxCount
      case None => None
    }
  }

  private[this] def isForAll(s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Boolean =
    getUniqueQual(s.components) match {
      case Some(s: QualifiedValueShape) => false
      case None => true
    }

  private[this] def getLocalConstraintComponents(components: Seq[Component], anonNodeShapes: Map[RefNode, NodeShape], isPos: Boolean = true)
  : (Optional[String], Optional[String], Optional[String], Boolean) = {
    var datatype: Optional[String] = Optional.empty()
    var constant: Optional[String] = Optional.empty()
    var shapeRef: Optional[String] = Optional.empty()
    getUniqueNot(components) match {
      case Some(not: Not) =>
        getLocalConstraintComponents(
          anonNodeShapes(not.shape).components,
          anonNodeShapes,
          isPos = false
        )
      case None =>
        getUniqueQual(components) match {
          case Some(s: QualifiedValueShape) =>
            getLocalConstraintComponents(
              anonNodeShapes(s.shape).components,
              anonNodeShapes
            )
          case None =>
            components.foreach {
              case Datatype(value: IRI) => datatype = Optional.of(value.toString())
              case HasValue(value: Value) => constant = Optional.of(value.rdfNode.toString)
              case NodeComponent(shape: RefNode) => shapeRef = Optional.of(shape.showId)
              case _ =>
            }
            (datatype, constant, shapeRef, isPos)
        }
    }
  }


  private[this] def getMinConstraint(id: String, s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Option[MinOnlyConstraint] =
    getMinCard(s, anonNodeShapes) match {
      case None => None
      case Some(card) =>
        val (datatype, constant, shaperef, isPos) = getLocalConstraintComponents(s.components, anonNodeShapes)
        Some(new MinOnlyConstraintImpl(
          id,
          s.path.predicate.getOrElse(
            throw new RDFParserException("Property paths not supported yet:\n" + s.path.toString)
          ).toString(),
          card,
          datatype,
          constant,
          shaperef,
          isPos
        ))
    }

  private[this] def getMaxConstraints(id: String, s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Iterable[MaxOnlyConstraint] = {
    getMaxConstraint(id, s, anonNodeShapes) ++ getForAllConstraint(id, s, anonNodeShapes)
  }

  private[this] def getMaxConstraint(id: String, s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Option[MaxOnlyConstraint] =
    getMaxCard(s, anonNodeShapes) match {
      case None => None
      //explicit max constraint
      case Some(card) =>
        val (datatype, constant, shaperef, isPos) = getLocalConstraintComponents(s.components, anonNodeShapes)
        Some(new MaxOnlyConstraintImpl(
          id,
          s.path.predicate.getOrElse(
            throw new RDFParserException("Property paths not supported yet:\n" + s.path.toString)
          ).toString(),
          card,
          datatype,
          constant,
          shaperef,
          isPos
        ))
    }

  def getPathString(path: SHACLPath): String =
    path match {
      case _: PredicatePath =>
        path.predicate.getOrElse(
          throw new RDFParserException("A PredicatePath is expected to have a predicate")
        ).toString()
      case _: InversePath =>
        "^"+getPathString(path.asInstanceOf[InversePath].path)
      case _: ZeroOrMorePath =>
        getPathString(path.asInstanceOf[ZeroOrMorePath].path)+"*"
      case _:  SequencePath =>
        path.asInstanceOf[SequencePath].paths.map(x => getPathString(x)).mkString("/")
      case _: AlternativePath  =>
        path.asInstanceOf[AlternativePath].paths.map(x => getPathString(x)).mkString("|")
      case _ =>
        throw new RDFParserException("Unexpected property path:\n" + path.toString)
    }


  private[this] def getForAllConstraint(id: String, s: PropertyShape, anonNodeShapes: Map[RefNode, NodeShape]): Option[MaxOnlyConstraint] =
    isForAll(s, anonNodeShapes) match {
      case false => None
      case true =>
        val (datatype, constant, shaperef, isPos) = getLocalConstraintComponents(s.components, anonNodeShapes, false)
        if(datatype.isPresent || constant.isPresent || shaperef.isPresent)
          return Some(new MaxOnlyConstraintImpl(
          id,
            getPathString(s.path),
//            s.path.predicate.getOrElse(
//              throw new RDFParserException("Property paths not supported yet:\n" + s.path.toString)
//            ).toString(),
            0,
            datatype,
            constant,
            shaperef,
            isPos
          ))
        None
    }


  private[this] def getLocalConstraint(id: String, components: Seq[Component], anonNodeShapes: Map[RefNode, NodeShape]): Option[LocalConstraint] = {
    val (datatype, constant, shaperef, isPos) = getLocalConstraintComponents(components, anonNodeShapes)
    if(datatype.isPresent || constant.isPresent || shaperef.isPresent)
    Some(new LocalConstraintImpl(
      id,
      datatype,
      constant,
      shaperef,
      isPos
    ))
    else
      None
  }


  private[this] def unsupported(comp: Component): Option[Component] =
    comp match {
      case _: ClassComponent |
           _: NodeKind |
           _: MinExclusive |
           _: MinInclusive |
           _: MaxExclusive |
           _: MaxInclusive |
           _: MinLength |
           _: MaxLength |
           _: Pattern |
           _: UniqueLang |
           _: Disjoint |
           _: LessThan |
           _: Closed |
           _: LessThanOrEquals |
           _: LanguageIn |
           _: And |
           _: Xone =>
        Some(comp)
      case _ =>
        None
    }


  private[this] def unexpected(comp: Component): Option[Component] =
    comp match {
      case _: Or =>
        Some(comp)
      case _ =>
        None
    }

  private[this] def getDisjunct(id: String, localConstraints: Seq[Component], propertyShapes: Seq[PropertyShape], anonNodeShapes: Map[RefNode, NodeShape]): ConstraintConjunction = {
    val i = new AtomicInteger()
    val allComponents = localConstraints ++ propertyShapes.flatMap(s => s.components)
    val unexpectedComponents = allComponents
      .flatMap(c => unexpected(c))
    if (unexpectedComponents.nonEmpty) {
      throw new RDFParserException("Unexpected components:" + unexpectedComponents.toString)
    }

    val unsupportedComponents = allComponents
      .flatMap(c => unsupported(c))
    if (unsupportedComponents.nonEmpty) {
      throw new RDFParserException("These components are not supported yet:\n" + unsupportedComponents.toString)
    }

    new ConstraintConjunctionImpl(
      id,
      wrapAsList(propertyShapes
        .flatMap(s => getMinConstraint(
          id + "_c" + i.incrementAndGet(),
          s,
          anonNodeShapes
        ))),
      wrapAsList(propertyShapes
        .flatMap(s => getMaxConstraints(
          id + "_c" + i.incrementAndGet(),
          s,
          anonNodeShapes
        ))),
      wrapAsList(getLocalConstraint(
        id + "_c" + i.incrementAndGet(),
        localConstraints,
        anonNodeShapes
      )))
  }

  private[this] def wrapAsList[T](l: Seq[T]): ImmutableList[T] = {
    scala.collection.JavaConverters.seqAsJavaList(l).stream()
      .collect(ImmutableCollectors.toList())
  }

  private[this] def wrapAsList[T](c: T): ImmutableList[T] = {
    wrapAsList(List(c))
  }

  private[this] def wrapAsList[T](o: Option[T]): ImmutableList[T] = {
    o match {
      case Some(c: T) => wrapAsList(c)
      case None => ImmutableList.of()
    }
  }

  private[this] def wrapAsSet[T](l: Seq[T]): ImmutableSet[T] = {
    scala.collection.JavaConverters.seqAsJavaList(l).stream()
      .collect(ImmutableCollectors.toSet())
  }

  private[this] def wrapAsSet[T](c: T): ImmutableSet[T] = {
    wrapAsSet(List(c))
  }

  private[this] def getName(shape: NodeShape): String = {
    shape.id.toIRI.getOrElse(
      throw new RDFParserException("This shape is expected to have a name" + shape.toString)
    ).toString
  }

  private[this] def getDisjuncts(id: String, nodeShape: NodeShape, anonNodeShapes: Map[RefNode, NodeShape], propertyShapes: Map[RefNode, PropertyShape]): Seq[ConstraintConjunction] = {
    val comps = nodeShape.components
    if (comps.size == 1)
      comps.head match {
        // the shape is a disjunction: recursive call
        case or: Or =>
          val i = new AtomicInteger()
          return or.shapes
            .map(r => anonNodeShapes(r))
            .flatMap(s => getDisjuncts(
              id + "_d" + i.incrementAndGet(),
              s,
              anonNodeShapes,
              propertyShapes
            ))
      }
    // the shape is composed of constraints: only one disjunct
    Seq(getDisjunct(
      id,
      comps,
      nodeShape.propertyShapes.map(r => propertyShapes(r)),
      anonNodeShapes
    ))
  }


  private[this] def getNodeShape(shape: NodeShape, namedNodeShapes: Map[RefNode, NodeShape], anonNodeShapes: Map[RefNode, NodeShape], propertyShapes: Map[RefNode, PropertyShape]): unibz.shapes.shape.Shape = {
    val tmp = getTargetQuery(shape)
    val tmp2 = wrapAsSet(getDisjuncts(
      getName(shape),
      shape,
      anonNodeShapes,
      propertyShapes
    ))
    new ShapeImpl(
      shape.id.toString,
      getTargetQuery(shape),
      wrapAsSet(getDisjuncts(
        getName(shape),
        shape,
        anonNodeShapes,
        propertyShapes
      )))
  }

  private[this] def convertSchema(schema: ShaclexSchema): unibz.shapes.shape.Schema = {

    // Partition the shapes into:
    // - names node shapes
    // - anonymous node shapes
    // - property shapes
    val (nodeShapes, propertyShapes) = schema.schema.shapesMap
      .partition(t => t._2.isInstanceOf[NodeShape])

    val (namedNodeShapes, anonNodeShapes) = nodeShapes
      .partition(t => t._2.id.isIRI)

    val convertedShapes = for {
      value <- namedNodeShapes.values
    } yield getNodeShape(
      value.asInstanceOf[NodeShape],
      namedNodeShapes
        .map(e => e._1 -> e._2.asInstanceOf[NodeShape]),
      anonNodeShapes
        .map(e => e._1 -> e._2.asInstanceOf[NodeShape]),
      propertyShapes
        .map(e => e._1 -> e._2.asInstanceOf[PropertyShape])
    )
    new SchemaImpl(wrapAsSet(convertedShapes.toSeq))
  }

  def parse(schemaString: String): unibz.shapes.shape.Schema = {

    val schema: ShaclexSchema = Schemas.fromString(
      schemaString,
      "TURTLE",
      "shaclex"
    ) match {
      case Right(schema: ShaclexSchema) => schema
      case _ => throw new RuntimeException("The schema cannot be parsed")
    }
    convertSchema(schema)
  }

  def parse(file: File): unibz.shapes.shape.Schema = {

    val schema: ShaclexSchema = Schemas.fromFile(
      file,
      "TURTLE",
      "shaclex"
    ) match {
      case Right(schema: ShaclexSchema) => schema
      case _ => throw new RuntimeException("The schema cannot be parsed")
    }
    convertSchema(schema)
  }
}
