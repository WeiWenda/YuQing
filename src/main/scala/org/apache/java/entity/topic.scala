package org.apache.java.entity

import org.apache.java.jdbc.MySqlData

import scala.collection.immutable.List

/**
 * Created by david on 11/23/15.
 */
class topic(val topic_keywords: List[(Int, String)],val topic_id: Int) extends Serializable{
  var topic_name: List[(Int, String)] = {
    if (topic_keywords.size<2)
      List(topic_keywords(0))
    else
      List(topic_keywords(0), topic_keywords(1))
  }
  var summary: String = null
  var language_type: Int = 0
  var doc_related: List[String] = List[String]()
  def showall(): Unit = {
    println("topic_id:" + topic_id)
    println("topic_keywords:" + topic_keywords)
    println("topic_name:" + topic_name)
    println("summary:" + summary)
    println("language_type:" + language_type)
    println("doc_num:" + doc_related.size)
  }

  def intoDB(mySqlData: MySqlData): Unit = {
    val name_str= topic_name.map {
      _._2
    }.mkString(",")
    val description_str = topic_keywords.map { case (count, word) => word + ":" + count.toString }.mkString(",")
    val sql = "INSERT INTO topic(topic_id, topic_name, topic_keywords,language_type,summary,doc_num)  VALUE('"+topic_id +"','"+name_str+"','"+description_str + "','" + language_type + "','" +summary+"','"+doc_related.size+ "')"
    mySqlData.connectOutputDatabaseTxt()
    try {
      mySqlData.stmt.executeBatch()
      mySqlData.stmt.executeUpdate(sql)
    } catch {
      case e: Exception =>
        e.printStackTrace
        showall()
      //      case e => e.printStackTrace
      //      case _: Throwable =>
      //        println("error")
    }
//      for(id<-doc_related){
//        val sql = "INSERT INTO post_bursttopic(post_id, burst_id) VALUE('" + id + "','" + burst_id + "')"
//        try {
//          mySqlData.stmt.executeBatch()
//          mySqlData.stmt.executeUpdate(sql)
//        } catch {
//          case e: Exception =>
//            e.printStackTrace
//        }
//      }
    mySqlData.closeConnStmt()
  }
}

object topic {
  private var last_topic_id = 0
  def last_topic_id_(maxid:Int): Unit ={
    last_topic_id=maxid
  }

  def apply(description: List[(Int, String)]) = {
    last_topic_id = last_topic_id + 1
    new topic(description, last_topic_id)

  }
}

