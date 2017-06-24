package utils

import org.slf4j.{LoggerFactory, MarkerFactory}
import play.api.{Logger, MarkerContext}
import utils.metrics.Instrumented

object Logging extends Instrumented {
  implicit val mc = MarkerContext(MarkerFactory.getMarker("scala"))

  private[this] val traceMeter = metrics.meter("log.trace")
  private[this] val debugMeter = metrics.meter("log.debug")
  private[this] val infoMeter = metrics.meter("log.info")
  private[this] val warnMeter = metrics.meter("log.warn")
  private[this] val errorMeter = metrics.meter("log.error")

  private[this] var callback: Option[(Int, String) => Unit] = None

  def setCallback(f: (Int, String) => Unit) = callback = Some(f)

  case class CustomLogger(name: String) extends Logger(LoggerFactory.getLogger(name)) {
    override def trace(message: => String)(implicit mc: play.api.MarkerContext) = {
      traceMeter.mark()
      super.trace(message)
    }
    override def trace(message: => String, error: => Throwable)(implicit mc: play.api.MarkerContext) = {
      traceMeter.mark()
      super.trace(message, error)
    }
    override def debug(message: => String)(implicit mc: play.api.MarkerContext) = {
      debugMeter.mark()
      super.debug(message)
    }
    override def debug(message: => String, error: => Throwable)(implicit mc: play.api.MarkerContext) = {
      debugMeter.mark()
      super.debug(message, error)
    }
    override def info(message: => String)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(1, message))
      infoMeter.mark()
      super.info(message)
    }
    override def info(message: => String, error: => Throwable)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(1, message))
      infoMeter.mark()
      super.info(message, error)
    }
    override def warn(message: => String)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(2, message))
      warnMeter.mark()
      super.warn(message)
    }
    override def warn(message: => String, error: => Throwable)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(2, message))
      warnMeter.mark()
      super.warn(message, error)
    }
    override def error(message: => String)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(3, message))
      errorMeter.mark()
      super.error(message)
    }
    override def error(message: => String, error: => Throwable)(implicit mc: play.api.MarkerContext) = {
      callback.foreach(_(3, message))
      errorMeter.mark()
      super.error(message, error)
    }
    def errorThenThrow(message: => String) = {
      this.error(message)
      throw new IllegalStateException(message)
    }
    def errorThenThrow(message: => String, error: => Throwable) = {
      this.error(message, error)
      throw error
    }
  }
}

trait Logging {
  protected[this] val log = Logging.CustomLogger(s"boilerplay.${this.getClass.getSimpleName.replace("$", "")}")
}
