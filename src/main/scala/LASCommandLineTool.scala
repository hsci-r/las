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

object LASCommandLineTool {

  lazy val hfstlas = new HFSTLexicalAnalysisService
  lazy val combinedlas = new CombinedLexicalAnalysisService
  lazy val snowballlas = new SnowballLexicalAnalysisService
  lazy val compoundlas = new CompoundLexicalAnalysisService(combinedlas, snowballlas)

  object Action extends Enumeration {
    type Action = Value
    val Lemmatize, Analyze, Detect = Value
  }

  implicit val actionRead: scopt.Read[Action.Value] = scopt.Read.reads(Action withName _)

  case class Config(action: Action.Action = null, locale: Seq[String] = Seq(), files: Seq[String] = Seq())

  def writeFile(file: String, text: String): Unit = {
    val writer = new PrintWriter(new File(file))
    writer.write(text);
    writer.close()
  }

  def main(args: Array[String]) = {
    val parser = new scopt.OptionParser[Config]("las") {
      head("las", "1.0")
      cmd("lemmatize") action { (_, c) =>
        c.copy(action = Action.Lemmatize)
      } text (s"lemmatize (locales: ${compoundlas.getSupportedBaseformLocales.mkString(", ")})")
      cmd("analyze") action { (_, c) =>
        c.copy(action = Action.Analyze)
      } text (s"analyze (locales: ${combinedlas.getSupportedAnalyzeLocales.mkString(", ")})")
      cmd("identify") action { (_, c) =>
        c.copy(action = Action.Detect)
      } text (s"identify language (locales: ${(LanguageRecognizer.getAvailableLanguages ++ LanguageDetector.supportedLanguages ++ compoundlas.getSupportedBaseformLocales).toSet.mkString(", ")})")
      opt[Seq[String]]("locale") optional () action { (x, c) =>
        c.copy(locale = x)
      } text ("possible locales")
      arg[String]("<file>...") unbounded () optional () action { (x, c) =>
        c.copy(files = c.files :+ x)
      } text ("files to process (stdin if not given)")
      help("help") text("prints this usage text")
      checkConfig { c => if (c.action==null) failure("specify at least an action (lemmatize, analyze or identify)") else success }
    }
    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
      case Some(config) =>
        config.action match {
          case Action.Lemmatize => if (!config.files.isEmpty) {
            for (
              file <- config.files;
              text = Source.fromFile(file).mkString; out = lemmatize(text, config.locale); if out.isDefined
            ) writeFile(file + ".lemmatized", out.get);
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(lemmatize(text, config.locale).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
          case Action.Analyze => if (!config.files.isEmpty) {
            for (
              file <- config.files;
              text = Source.fromFile(file).mkString; out = analyze(text, config.locale); if out.isDefined
            ) writeFile(file + ".analysis", out.get);
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(analyze(text, config.locale).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
          case Action.Detect => if (!config.files.isEmpty) {
            for (
              file <- config.files;
              text = Source.fromFile(file).mkString; out = identify(text, config.locale); if out.isDefined
            ) writeFile(file + ".language", out.get);
          } else {
            var text = StdIn.readLine()
            while (text != null) {
              println(identify(text, config.locale).getOrElse("?"));
              text = StdIn.readLine()
            }
          }
        }
      case None =>
    }
    System.exit(0)
  }

  def lemmatize(text: String, locales: Seq[String]): Option[String] = {
    getBestLang(text, if (locales.isEmpty) compoundlas.getSupportedBaseformLocales.toSeq.map(_.toString) else locales) match {
      case Some(lang) => 
        val baseform = compoundlas.baseform(text, new Locale(lang)) 
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

  def analyze(text: String, locales: Seq[String]): Option[String] = {
    getBestLang(text, if (locales.isEmpty) combinedlas.getSupportedAnalyzeLocales.toSeq.map(_.toString) else locales) match {
      case Some(lang) => 
        val analysis = Json.toJson(combinedlas.analyze(text, new Locale(lang)).toList)
        if (locales.isEmpty) Some(Json.toJson(Map("locale" -> lang, "analysis" -> analysis).toString()).toString())
        else Some(analysis.toString())
      case None       => None
    }
  }

  def getBestLang(text: String, locales: Seq[String]): Option[String] = {
    if (locales.isEmpty) {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).map(r => Map(r.getLang() -> r.getIndex))
      val detector = LanguageDetector()
      detector.append(text)
      val ldResult = detector.getProbabilities().map(l => Map(l.lang -> l.prob))
      val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
        (lang.toString(),
          hfstlas.analyze(text, lang).foldRight((0, 0)) { (ar, count) =>
            if ((ar.getAnalysis.get(0).getParts().get(0).getTags.isEmpty || ar.getAnalysis.get(0).getParts().get(0).getTags.containsKey("PUNCT")) && ar.getAnalysis.get(0).getGlobalTags.isEmpty)
              (count._1, count._2 + 1)
            else (count._1 + 1, count._2 + 1)
          })).filter(_._2._1 != 0).toSeq.view.sortBy(_._2._1).reverse.map(p => (p._1, p._2._1.asInstanceOf[Double] / p._2._2))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2)._1)).getOrElse(None)
    } else {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text, locales: _*)).map(r => Map(r.getLang() -> r.getIndex))
      val detector = LanguageDetector()
      detector.setPriorMap(new HashMap(mapAsJavaMap(locales.map((_, new java.lang.Double(1.0))).toMap)))
      detector.append(text)
      val ldResult = detector.getProbabilities().map(l => Map(l.lang -> l.prob))
      val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
        (lang.toString(),
          hfstlas.analyze(text, lang).foldRight((0, 0)) { (ar, count) =>
            if ((ar.getAnalysis.get(0).getParts().get(0).getTags.isEmpty || ar.getAnalysis.get(0).getParts().get(0).getTags.containsKey("PUNCT")) && ar.getAnalysis.get(0).getGlobalTags.isEmpty)
              (count._1, count._2 + 1)
            else (count._1 + 1, count._2 + 1)
          })).filter(_._2._1 != 0).toSeq.view.sortBy(_._2._1).reverse.map(p => (p._1, p._2._1.asInstanceOf[Double] / p._2._2))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2)._1)).getOrElse(None)
    }
  }

  def identify(text: String, locales: Seq[String]): Option[String] = {
    if (!locales.isEmpty) {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text, locales: _*)).map(r => Map(r.getLang() -> r.getIndex))
      val detector = LanguageDetector()
      detector.setPriorMap(new HashMap(mapAsJavaMap(locales.map((_, new java.lang.Double(1.0))).toMap)))
      detector.append(text)
      val ldResult = detector.getProbabilities().map(l => Map(l.lang -> l.prob))
      val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
        (lang.toString(),
          hfstlas.analyze(text, lang).foldRight((0, 0)) { (ar, count) =>
            if ((ar.getAnalysis.get(0).getParts().get(0).getTags.isEmpty || ar.getAnalysis.get(0).getParts().get(0).getTags.containsKey("PUNCT")) && ar.getAnalysis.get(0).getGlobalTags.isEmpty)
              (count._1, count._2 + 1)
            else (count._1 + 1, count._2 + 1)
          })).filter(_._2._1 != 0).toSeq.view.sortBy(_._2._1).reverse.map(p => (p._1, p._2._1.asInstanceOf[Double] / p._2._2))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2))).getOrElse(None)
      bestGuess match {
        case Some(lang) => Some(Json.toJson(Map("locale" -> Json.toJson(lang._1), "certainty" -> Json.toJson(lang._2), "details" -> Json.toJson(Map("languageRecognizerResults" -> Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult))))).toString())
        case None       => None
      }
    } else {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).map(r => Map(r.getLang() -> r.getIndex))
      val detector = LanguageDetector()
      detector.append(text)
      val ldResult = detector.getProbabilities().map(l => Map(l.lang -> l.prob))
      val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
        (lang.toString(),
          hfstlas.analyze(text, lang).foldRight((0, 0)) { (ar, count) =>
            if ((ar.getAnalysis.get(0).getParts().get(0).getTags.isEmpty || ar.getAnalysis.get(0).getParts().get(0).getTags.containsKey("PUNCT")) && ar.getAnalysis.get(0).getGlobalTags.isEmpty)
              (count._1, count._2 + 1)
            else (count._1 + 1, count._2 + 1)
          })).filter(_._2._1 != 0).toSeq.view.sortBy(_._2._1).reverse.map(p => (p._1, p._2._1.asInstanceOf[Double] / p._2._2))
      val tc = hfstResultTmp.foldRight(0.0) { _._2 + _ }
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2 / tc))
      val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0) { (p, r) => r + p.valuesIterator.next } / 3.0).maxBy(_._2))).getOrElse(None)
      bestGuess match {
        case Some(lang) => Some(Json.toJson(Map("locale" -> Json.toJson(lang._1), "certainty" -> Json.toJson(lang._2), "details" -> Json.toJson(Map("languageRecognizerResults" -> Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult))))).toString())
        case None       => None
      }
    }
  }

}

object LanguageDetector extends LazyLogging {
  def apply() = com.cybozu.labs.langdetect.DetectorFactory.create()
  val supportedLanguages = Array("af", "am", "ar", "az", "be", "bg", "bn", "bo", "ca", "cs", "cy", "da", "de", "dv", "el", "en", "es", "et", "eu", "fa", "fi", "fo", "fr", "ga", "gn", "gu", "he", "hi", "hr", "hu", "hy", "id", "is", "it", "ja", "jv", "ka", "kk", "km", "kn", "ko", "ky", "lb", "lij", "ln", "lt", "lv", "mi", "mk", "ml", "mn", "mr", "mt", "my", "ne", "nl", "no", "os", "pa", "pl", "pnb", "pt", "qu", "ro", "si", "sk", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tk", "tl", "tr", "tt", "ug", "uk", "ur", "uz", "vi", "yi", "yo", "zh-cn", "zh-tw")
  try {
    com.cybozu.labs.langdetect.DetectorFactory.loadProfiles(supportedLanguages: _*)
  } catch {
    case e: Exception => logger.warn("Couldn't load language profiles", e)
  }
}
