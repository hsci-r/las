import fi.seco.lexical.hfst.HFSTLexicalAnalysisService
import fi.seco.lexical.combined.CombinedLexicalAnalysisService
import fi.seco.lexical.SnowballLexicalAnalysisService
import fi.seco.lexical.CompoundLexicalAnalysisService
import com.typesafe.scalalogging.LazyLogging
import fi.seco.lexical.LanguageRecognizer
import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._
import scala.util.Try
import java.util.Locale
import java.util.HashMap
import java.io.File
import scala.io.StdIn
import scala.io.Source
import java.io.PrintWriter
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService.WordToResults
import play.api.libs.json.Writes
import java.util.Collections
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.text.CommonTextObjectFactories

object LASCommandLineTool {

  lazy val hfstlas = new HFSTLexicalAnalysisService
  lazy val combinedlas = new CombinedLexicalAnalysisService
  lazy val snowballlas = new SnowballLexicalAnalysisService
  lazy val compoundlas = new CompoundLexicalAnalysisService(combinedlas, snowballlas)
  
  object LanguageDetector extends LazyLogging {
    lazy val languageProfiles = new LanguageProfileReader().readAllBuiltIn()
    lazy val supportedLanguages = languageProfiles.map(_.getLocale.toString())
    lazy val detector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles).build()
    lazy val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText()
    def apply(text: String) = detector.getProbabilities(textObjectFactory.forText(text))
  }
  
  object ProcessBy extends Enumeration {
    type ProcessBy = Value
    val File, Paragraph, Line = Value
  }
   
  object Action extends Enumeration {
    type Action = Value
    val Inflect, Lemmatize, Analyze, Detect, Recognize = Value
  }

  implicit val actionRead: scopt.Read[Action.Value] = scopt.Read.reads(Action withName _)

  case class Config(action: Action.Action = null, locale: Seq[String] = Seq(), forms: Seq[String] = Seq(), segmentBaseforms: Boolean = false, processBy: ProcessBy.ProcessBy = ProcessBy.File, guess: Boolean = true, segmentGuessed: Boolean = true, maxEditDistance: Int = 0, pretty: Boolean = true, files: Seq[String] = Seq())

  def main(args: Array[String]) = {
    val parser = new scopt.OptionParser[Config]("las") {
      head("las", "1.4.5")
      cmd("lemmatize") action { (_, c) =>
        c.copy(action = Action.Lemmatize)
      } text (s"(locales: ${compoundlas.getSupportedBaseformLocales.mkString(", ")})")
      cmd("analyze") action { (_, c) =>
        c.copy(action = Action.Analyze)
      } text (s"(locales: ${combinedlas.getSupportedAnalyzeLocales.mkString(", ")})")
      cmd("inflect") action { (_, c) =>
        c.copy(action = Action.Inflect)
      } text (s"(locales: ${combinedlas.getSupportedInflectionLocales.mkString(", ")})")
      cmd("recognize") action { (_, c) =>
        c.copy(action = Action.Recognize)
      } text (s"report word recognition rate (locales: ${combinedlas.getSupportedAnalyzeLocales.mkString(", ")}")
      cmd("identify") action { (_, c) =>
        c.copy(action = Action.Detect)
      } text (s"identify language (locales: ${(LanguageRecognizer.getAvailableLanguages ++ LanguageDetector.supportedLanguages ++ compoundlas.getSupportedBaseformLocales).toSet.mkString(", ")})")
      opt[Seq[String]]("locale") optional () action { (x, c) =>
        c.copy(locale = x)
      } text ("possible locales")
      opt[Seq[String]]("forms") optional () action { (x, c) =>
        c.copy(forms = x)
      } text ("inclection forms for inflect/analyze")
      opt[Unit]("segment") action { (_, c) =>
        c.copy(segmentBaseforms = true)
      } text ("segment baseforms?")
      opt[Unit]("no-guess") action { (_, c) =>
        c.copy(guess = false)
      } text ("Don't guess baseforms for unknown words?")
      opt[Unit]("no-segment-guessed") action { (_, c) =>
        c.copy(segmentGuessed = false)
      } text ("Don't guess segmentation information for guessed words (speeds up processing significantly)?")
      opt[String]("process-by") action { (x, c) =>
        c.copy(processBy = ProcessBy.withName(x.charAt(0).toUpper + x.substring(1).toLowerCase))
      } text ("Analysis unit when processing files (file, paragraph, line)?")
      opt[Int]("max-edit-distance") action { (x, c) =>
        c.copy(maxEditDistance = x)
      } text ("Maximum edit distance for error-correcting unidentified words (default 0)?")
      opt[Unit]("no-pretty") action { (_, c) =>
        c.copy(pretty = true)
      } text ("Don't pretty print json?")
      arg[String]("<file>...") unbounded () optional () action { (x, c) =>
        c.copy(files = c.files :+ x)
      } text ("files to process (stdin if not given)")
      help("help") text("prints this usage text")
      checkConfig { c => if (c.action==null) failure("specify at least an action (lemmatize, analyze, inflect or identify)") else success }
    }
    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
      case Some(config) =>
        config.action match {
          case Action.Lemmatize => if (!config.files.isEmpty) for (file <- config.files) {
            val writer = new PrintWriter(new File(file+".lemmatized"))
            val paragraphs = config.processBy match {
              case ProcessBy.File => Seq(Source.fromFile(file).mkString)
              case ProcessBy.Paragraph => Source.fromFile(file).mkString.split("\\s*\n\\s*\n").toSeq
              case ProcessBy.Line => Source.fromFile(file).mkString.split("\n").toSeq
            }
            var i = 0
            for (paragraph <- paragraphs) {
              val lemma = lemmatize(paragraph, config.locale,config.segmentBaseforms,config.guess,config.maxEditDistance).getOrElse(paragraph)
              writer.write(lemma)
              i += 1
              if (i!=paragraphs.length) {
                writer.write("\n")
                if (config.processBy == ProcessBy.Paragraph) writer.write("\n")
              }            }
            writer.close()
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(lemmatize(text, config.locale,config.segmentBaseforms,config.guess,config.maxEditDistance).getOrElse(text));
              text = StdIn.readLine()
            }
          }
          case Action.Analyze => if (!config.files.isEmpty) for (file <- config.files) {
            val writer = new PrintWriter(new File(file+".analysis"))
            val paragraphs = config.processBy match {
              case ProcessBy.File => Seq(Source.fromFile(file).mkString)
              case ProcessBy.Paragraph => Source.fromFile(file).mkString.split("\\s*\n\\s*\n").toSeq
              case ProcessBy.Line => Source.fromFile(file).mkString.split("\n").toSeq
            }
            var i = 0
            if (config.processBy!=ProcessBy.File) writer.write('[')
            for (paragraph <- paragraphs) {
              val analysis = analyze(paragraph, config.locale,config.forms,config.segmentBaseforms,config.guess,config.segmentGuessed,config.maxEditDistance,config.pretty).getOrElse("{}")
              writer.write(analysis)
              i += 1
              if (i!=paragraphs.length) {
                writer.write(",")
                if (config.pretty) writer.write('\n');
              }
            }
            if (config.processBy!=ProcessBy.File) writer.write(']')
            writer.close()
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(analyze(text, config.locale,config.forms,config.segmentBaseforms,config.guess,config.segmentGuessed,config.maxEditDistance,config.pretty).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
          case Action.Inflect => if (!config.files.isEmpty) for (file <- config.files) {
            val writer = new PrintWriter(new File(file+".inflected"))
            val paragraphs = config.processBy match {
              case ProcessBy.File => Seq(Source.fromFile(file).mkString)
              case ProcessBy.Paragraph => Source.fromFile(file).mkString.split("\\s*\n\\s*\n").toSeq
              case ProcessBy.Line => Source.fromFile(file).mkString.split("\n").toSeq
            }
            var i = 0
            for (paragraph <- paragraphs) inflect(paragraph, config.locale,config.forms,config.segmentBaseforms,config.guess,config.maxEditDistance).foreach(u => {
              writer.write(u)
              i += 1
              if (i!=paragraphs.length) {
                writer.write("\n")
                if (config.processBy == ProcessBy.Paragraph) writer.write("\n")
              }
            })
            writer.close()
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(inflect(text, config.locale,config.forms,config.segmentBaseforms,config.guess,config.maxEditDistance).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
          case Action.Detect => if (!config.files.isEmpty) for (file <- config.files) {
            val writer = new PrintWriter(new File(file+".language"))
            val paragraphs = config.processBy match {
              case ProcessBy.File => Seq(Source.fromFile(file).mkString)
              case ProcessBy.Paragraph => Source.fromFile(file).mkString.split("\\s*\n\\s*\n").toSeq
              case ProcessBy.Line => Source.fromFile(file).mkString.split("\n").toSeq
            }
            var i = 0 
            if (config.processBy!=ProcessBy.File) writer.write('[')
            for (paragraph <- paragraphs) {
              val analysis = identify(paragraph, config.locale,config.pretty).getOrElse("{}")
              writer.write(analysis)
              i += 1
              if (i!=paragraphs.length) {
                writer.write(",")
                if (config.pretty) writer.write('\n');
              }
            }
            if (config.processBy!=ProcessBy.File) writer.write(']')
            writer.close()
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(identify(text, config.locale,config.pretty).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
          case Action.Recognize => if (!config.files.isEmpty) for (file <- config.files) {
            val writer = new PrintWriter(new File(file+".recognition"))
            val paragraphs = config.processBy match {
              case ProcessBy.File => Seq(Source.fromFile(file).mkString)
              case ProcessBy.Paragraph => Source.fromFile(file).mkString.split("\\s*\n\\s*\n").toSeq
              case ProcessBy.Line => Source.fromFile(file).mkString.split("\n").toSeq
            }
            var i = 0
            if (config.processBy!=ProcessBy.File) writer.write('[')
            for (paragraph <- paragraphs) {
              val analysis = recognize(paragraph, config.locale,config.pretty)
              writer.write(analysis)
              i += 1
              if (i!=paragraphs.length) {
                writer.write(",")
                if (config.pretty) writer.write('\n');
              }
            }
            if (config.processBy!=ProcessBy.File) writer.write(']')
            writer.close()
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(recognize(text, config.locale,config.pretty));
              text = StdIn.readLine()
            }
          }
        }
      case None =>
    }
    System.exit(0)
  }

  def lemmatize(text: String, locales: Seq[String], segments : Boolean, guess: Boolean, maxEditDistance: Int): Option[String] = {
    (if (locales.length==1) Some(locales(0)) else getBestLang(text, if (locales.isEmpty) compoundlas.getSupportedBaseformLocales.toSeq.map(_.toString) else locales)) match {
      case Some(lang) => 
        val baseform = compoundlas.baseform(text, new Locale(lang),segments,guess,maxEditDistance) 
        if (locales.isEmpty) Some(Json.toJson(Map("locale" -> lang, "baseform" -> baseform)).toString())
        else Some(baseform)
      case None       => None
    }
  }
  
    implicit val WordPartWrites = new Writes[HFSTLexicalAnalysisService.Result.WordPart] {
    def writes(r : HFSTLexicalAnalysisService.Result.WordPart) : JsValue = {
      Json.obj(
        "lemma" -> r.getLemma,
        "tags" -> Json.toJson(r.getTags.toMap.mapValues(iterableAsScalaIterable(_)))
      )
    }
  }

  implicit val ResultWrites = new Writes[HFSTLexicalAnalysisService.Result] {
    def writes(r : HFSTLexicalAnalysisService.Result) : JsValue = {
      Json.obj(
        "weight" -> r.getWeight,
        "wordParts" -> Json.toJson(r.getParts.map(Json.toJson(_))),
        "globalTags" -> Json.toJson(r.getGlobalTags.toMap.mapValues(iterableAsScalaIterable(_)))
      )
    }
  }

  implicit val wordToResultsWrites = new Writes[WordToResults] {
    def writes(r: WordToResults) : JsValue = {
      Json.obj(
         "word" -> r.getWord,
         "analysis" -> Json.toJson(r.getAnalysis.map(Json.toJson(_)))
      )
    }
  }

  def analyze(text: String, locales: Seq[String],forms:Seq[String], segments:Boolean, guess:Boolean, segmentGuessed:Boolean, maxEditDistance: Int, pretty:Boolean): Option[String] = {
    (if (locales.length==1) Some(locales(0)) else getBestLang(text, if (locales.isEmpty) combinedlas.getSupportedAnalyzeLocales.toSeq.map(_.toString) else locales)) match {
      case Some(lang) => 
        val analysis = Json.toJson(combinedlas.analyze(text, new Locale(lang),forms,segments,guess,segmentGuessed,maxEditDistance).toList)
        if (pretty) {
          if (locales.isEmpty) Some(Json.prettyPrint(Json.toJson(Map("locale" -> Json.toJson(lang), "analysis" -> analysis))))
          else Some(Json.prettyPrint(analysis))
        }
        else {
          if (locales.isEmpty) Some(Json.toJson(Map("locale" -> Json.toJson(lang), "analysis" -> analysis)).toString())
          else Some(analysis.toString())
        }
      case None       => None
    }
  }
  
  def recognize(text: String, locales: Seq[String], pretty:Boolean): String = {
    (if (locales.length==1) Some(locales(0)) else getBestLang(text, if (locales.isEmpty) combinedlas.getSupportedAnalyzeLocales.toSeq.map(_.toString) else locales)) match {
      case Some(lang) =>
        val analysis = combinedlas.recognize(text, new Locale(lang))
        val ret = Json.toJson(Map("locale" -> Json.toJson(lang), "recognized" -> Json.toJson(analysis.getRecognized), "unrecognized" -> Json.toJson(analysis.getUnrecognized), "rate" -> Json.toJson(analysis.getRate)))
        if (pretty) Json.prettyPrint(ret)
        else
          ret.toString()
      case None => "{ \"locale\": \"?\", \"recognized\": \"0\", \"unrecognized\": \""+(text.split("\\s+").length)+"\", \"rate\": \"0.0\" }"
    }
  }

  def inflect(text: String, locales: Seq[String],forms:Seq[String],segments:Boolean,guess:Boolean,maxEditDistance:Int): Option[String] = {
    (if (locales.length==1) Some(locales(0)) else getBestLang(text, if (locales.isEmpty) compoundlas.getSupportedInflectionLocales.toSeq.map(_.toString) else locales)) match {
      case Some(lang) => 
        val baseform = compoundlas.inflect(text, forms, segments, true, guess, maxEditDistance, new Locale(lang)) 
        if (locales.isEmpty) Some(Json.toJson(Map("locale" -> lang, "inflection" -> baseform)).toString())
        else Some(baseform)
      case None       => None
    }
  }

  def getBestLang(text: String, locales: Seq[String]): Option[String] = {
    if (locales.isEmpty) {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).map(r => Map(r.getLang() -> r.getIndex))
      val ldResult = Try(LanguageDetector(text).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
      val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2.getRate!=0.0).toSeq.sortBy(_._2.getRate).reverse.map(p => (p._1,p._2.getRate*p._2.getRate))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2)._1)).getOrElse(None)
    } else {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text, locales: _*)).map(r => Map(r.getLang() -> r.getIndex))
      val ldResult = Try(LanguageDetector(text).filter(d => locales.contains(d.getLocale.toString)).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
      val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2.getRate!=0.0).toSeq.sortBy(_._2.getRate).reverse.map(p => (p._1,p._2.getRate*p._2.getRate))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2)._1)).getOrElse(None)
    }
  }

  def identify(text: String, locales: Seq[String], pretty: Boolean): Option[String] = {
    val ret = if (!locales.isEmpty) {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text, locales: _*)).map(r => Map(r.getLang() -> r.getIndex))
      val ldResult = Try(LanguageDetector(text).filter(d => locales.contains(d.getLocale.toString)).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
      val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2.getRate!=0.0).toSeq.sortBy(_._2.getRate).reverse.map(p => (p._1,p._2.getRate*p._2.getRate))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2))).getOrElse(None)
      bestGuess match {
        case Some(lang) => Some(Json.toJson(Map("locale" -> Json.toJson(lang._1), "certainty" -> Json.toJson(lang._2), "details" -> Json.toJson(Map("languageRecognizerResults" -> Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult))))))
        case None       => None
      }
    } else {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).map(r => Map(r.getLang() -> r.getIndex))
      val ldResult = Try(LanguageDetector(text).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
      val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2.getRate!=0.0).toSeq.sortBy(_._2.getRate).reverse.map(p => (p._1,p._2.getRate*p._2.getRate))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2))).getOrElse(None)
      bestGuess match {
        case Some(lang) => Some(Json.toJson(Map("locale" -> Json.toJson(lang._1), "certainty" -> Json.toJson(lang._2), "details" -> Json.toJson(Map("languageRecognizerResults" -> Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult))))))
        case None       => None
      }
    }
    ret.map(ret => if (pretty) Json.prettyPrint(ret) else ret.toString())
  }

}
