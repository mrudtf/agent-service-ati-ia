// -*- mode: Scala;-*- 
// Filename:    Slog.scala 
// Authors:     lgm                                                    
// Creation:    Wed Sep  8 11:17:09 2010 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.protegra_ati.agentservices.store.util

import net.lag.configgy._

import scala.collection.mutable.HashMap

import org.apache.log4j.{PropertyConfigurator, Level, Logger}

object Severity extends Enumeration()
{
  type Severity = Value
  val Fatal, Error, Warning, Info, Debug, Trace = Value
}
//logging/tracing too expensive to leave on by default
object LogConfiguration {
  def SeverityFromString(level: String): Severity.Value =
  {
    level.toLowerCase() match {
      case "fatal" => {
        Severity.Fatal
      }
      case "error" => {
        Severity.Error
      }
      case "warning" => {
        Severity.Warning
      }
      case "info" => {
        Severity.Info
      }
      case "debug" => {
        Severity.Debug
      }
      case "trace" => {
        Severity.Trace
      }
      case _ => {
        Severity.Fatal
      }
    }
  }
  def SeverityFromOption(level: Option[ String ]): Severity.Value =
  {
    level match {
      case Some(x) => {
        SeverityFromString(x)
      }
      case None => {
        Severity.Fatal
      }
    }
  }

  lazy val config = {
    Configgy.configure("log_agentservices.conf")
    Configgy.config
  }
  var traceLevel = SeverityFromOption(config.getString("traceLevel"))
  var logLevel = SeverityFromOption(config.getString("logLevel"))

  lazy val logger = {
    PropertyConfigurator.configure("log_agentservices.properties")
    Logger.getLogger(this.getClass.getName)
  }
}

trait Reporting
{
  import LogConfiguration._    

  def prettyPrintElisions() : HashMap[String,String] =
    {
      val map = new HashMap[String,String]()
      //map += ( "{" -> "" )
      //map += ( "}" -> "" )
      map += ( "&amp;" -> "&" )
      map += ( "vamp;" -> "&" )
      map += ("amp;" -> "&" )
      map += ( "&quot;" -> "\"" )
      map += ( "vquot;" -> "\"" )
      map += ( "quot;" -> "\"" )
      map += ( "_-" -> "" )
      //map += ( ":" -> "" )
      map += ( "@class" -> "" )
      map += ( "com.biosimilarity.lift." -> "kvdb:" )
      map += ( "com.protegra_ati.agentservices.store." -> "agent-services:" )
      map += ( "MonadicTermTypes" -> "" )
      map += ( "AgentTS$TheMTT$" -> "" )
      map += ( "Groundvstring,$" -> "" )
      map += ( "Groundstring,$" -> "" )
      map += ( ",outer" -> "" )
      map += ( "&lt;" -> "<" )
      map += ( "&gt;" -> ">" )
      map
    }       

  def prettyPrint(value: String): String =
  {
    ( value /: prettyPrintElisions )(
      { ( acc, e ) => { acc.replace( e._1, e._2 ) } }
    ) 
  }

  def header(level: Severity.Value): String =
  {
    "=" + level.toString.toUpperCase + " REPORT==== Thread " + Thread.currentThread.getName + " ==="
  }

  def wrap[ A ](fact: A): String =
  {
    //keep it readable on println but still send it all to the log
    val value = prettyPrint(fact.toString)
    val max = if ( value.length < 512 ) value.length else 512
    value.substring(0, max)
  }

  def enabled(reportLevel: Severity.Value, configLevel: Severity.Value): Boolean =
  {
    //use id to compare ints in order of declaration
    reportLevel.id <= configLevel.id
  }

  def report[ A ](fact: A): Unit =
  {
    report(fact, Severity.Debug)
  }

  def report[ A ](fact: A, level: Severity.Value) =
  {
    trace(fact, level)
    log(fact, level)
  }

  def trace[ A ](fact: A): Unit =
  {
    trace(fact, Severity.Debug)
  }

  def trace[ A ](fact: A, level: Severity.Value) =
  {
    if ( enabled(level, traceLevel) ) {
      //todo: worth adding severity to output <report> tag?
      level match {
        case _ => {
          println(header(level) + "\n" + wrap(fact).toString() + "\n")
        }
      }
    }
  }

  def log[ A ](fact: A): Unit =
  {
    log(fact, Severity.Debug)
  }

  def log[ A ](fact: A, level: Severity.Value) =
  {
    if ( enabled(level, logLevel) ) {
      level match {
        case Severity.Fatal => {
          logger.log(Level.FATAL, fact toString)
        }
        case Severity.Error => {
	  logger.log(Level.ERROR, fact toString)
        }
        case Severity.Warning => {
          logger.log(Level.WARN, fact toString)
        }
        case Severity.Info => {
	  logger.log(Level.INFO, fact toString)
        }
        case Severity.Debug => {
          logger.log(Level.DEBUG, fact toString)
        }
        case Severity.Trace => {
	  logger.log(Level.TRACE, fact toString)
        }
        case _ => {
          logger.log(Level.DEBUG, fact toString)
        }
      }
    }
  }

}