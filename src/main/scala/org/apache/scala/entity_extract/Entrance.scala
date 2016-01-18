package org.apache.scala.entity_extract
import java.sql.Timestamp
import java.util.Properties

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{SQLContext, SaveMode}

/**
 * Created by wyy on 12/3/15.
 */
object Entrance {
  case class enm(id: Int, name: String, entity_type: Int, Hbase_id: String, topic_id: Int, text_src: Int, post_type: Int, offset_start: Int, offset_end: Int, sen_index: Int, sen_offset: Int, post_time: Timestamp, update_time: Timestamp, insert_time: Timestamp,entity_id:Int)

  def do_entityExtract (dburl:String, dbuser:String,dbpwd:String,sc:SparkContext,sqlContext:SQLContext) {
    //aaa.maina()
    val start_time:Timestamp = new Timestamp(System.currentTimeMillis())

    //val sparkConf = new SparkConf().setAppName("Entity") // An existing SparkContext.
    //val sc = new SparkContext(sparkConf)
//    val sc = new SparkContext("local","aaa")
//    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    val DBurl = dburl
    val DBuser = dbuser
    val DBpwd = dbpwd
    val post_table = "post"
    val postopic = "post_topic"
    val topic_table = "topic"
    val emtable = "entitymention"
    val entable = "entity"
    /*
        val DBurl = "jdbc:mysql://192.168.199.11:3306/net?useUnicode=true&characterEncoding=utf-8"
        val DBuser = "wyy"
        val DBpwd = "wyy"
        val post_table = "post"
        val postopic = "post_topic"
        val topic_table = "topic"
        val emtable = "entitymention2"
        val entable = "entity2"
    */
    val jdbcDF1 = sqlContext.read.format("jdbc").options(
      Map("url" -> DBurl, "user" -> DBuser, "password" -> DBpwd,
        "dbtable" -> post_table)).load()

    val jdbcDF2 = sqlContext.read.format("jdbc").options(
      Map("url" -> DBurl, "user" -> DBuser, "password" -> DBpwd,
        "dbtable" -> postopic)).load()

    val jdbcDF3 = sqlContext.read.format("jdbc").options(
      Map("url" -> DBurl, "user" -> DBuser, "password" -> DBpwd,
        "dbtable" -> topic_table)).load()


    val start_getEM:Timestamp = new Timestamp(System.currentTimeMillis())
    val emdf = GetEntityMention.getEntityMention(sqlContext,sc,jdbcDF1,jdbcDF2)
    val end_getEM:Timestamp = new Timestamp(System.currentTimeMillis())

    val start_EN:Timestamp = new Timestamp(System.currentTimeMillis())
    val endf = Merge.merge(sqlContext,sc,emdf,jdbcDF3,DBurl,DBuser,DBpwd,entable)
    val end_EN:Timestamp = new Timestamp(System.currentTimeMillis())

    val start_upEM:Timestamp = new Timestamp(System.currentTimeMillis())

    //val emdf = GetEntityMention.getEntityMention(sqlContext,sc)
    //val endf = Merge.merge(sqlContext,sc,emdf)
    println("----------------------join start---------------------------------")
    val entity_mention = emdf.join(endf,(emdf("topic_id") === endf("topic_id"))and(emdf("name") === endf("name")and(emdf("entity_type")===endf("entity_type"))), "left")
    println("----------------------join end---------------------------------")
    val data = entity_mention.map(row =>
      (row.getInt(row.fieldIndex("id")),
        row.getString(row.fieldIndex("name")),
        row.getInt(row.fieldIndex("entity_type")),
        row.getString(row.fieldIndex("Hbase_id")),
        row.getInt(row.fieldIndex("topic_id")),
        row.getInt(row.fieldIndex("text_src")),
        row.getInt(row.fieldIndex("post_type")),
        row.getInt(row.fieldIndex("offset_start")),
        row.getInt(row.fieldIndex("offset_end")),
        row.getInt(row.fieldIndex("sen_index")),
        row.getInt(row.fieldIndex("sen_offset")),
        row.getTimestamp(row.fieldIndex("post_time")),
        row.getTimestamp(row.fieldIndex("update_time")),
        row.getTimestamp(row.fieldIndex("insert_time")),
        row.getInt(row.fieldIndex("entity_id"))

        )
    )
    val cur_time: Timestamp = new Timestamp(System.currentTimeMillis())
    //data.foreach(c=>c._13 = cur_time)
    val enmrdd = data.map{
      case (id,name,page_type,hbaseid,topic_id, text_src, post_type, offset_start, offset_end,sen_index,sen_offset, post_time,update_time,insert_time,entity_id) =>
        (id,name,page_type,hbaseid,topic_id, text_src, post_type, offset_start, offset_end,sen_index,sen_offset, post_time,cur_time,insert_time,entity_id)
    }

    val c:Timestamp = new Timestamp(System.currentTimeMillis())
    println("---------EM Start-----------")
    import sqlContext.implicits._
    val enmDF = enmrdd.map{p=> enm(p._1,p._2,p._3,p._4,p._5,p._6,p._7,p._8,p._9,p._10,p._11,p._12,p._13,p._14,p._15)}.repartition(12).toDF()
    val property = new Properties()
    val url = DBurl
    property.put("user",DBuser)
    property.put("password", DBpwd)
    val c2:Timestamp = new Timestamp(System.currentTimeMillis())
    enmDF.write.mode(SaveMode.Append).jdbc(url, emtable, property)
    println("---------EM End-----------")

    val end_upEM:Timestamp = new Timestamp(System.currentTimeMillis())
    val end_time:Timestamp = new Timestamp(System.currentTimeMillis())

    println("getEM start at: "+start_getEM)
    println("getEM end at: "+end_getEM)
    println("EN start at: "+start_EN)
    println("EN end at: "+end_EN)
    println("update EM start at: "+start_upEM)
    println("map start at: "+cur_time)
    println("map end at: "+c)
    println("toDF end at: "+c2)
    println("update EM end at: "+end_upEM)
    println("Start at: "+start_time)
    println("End at: "+end_time)
    //sqlContext
    /*
        val emDF = sqlContext.read.format("jdbc").options(
          Map("url" -> "jdbc:mysql://202.117.16.118:3306/net","user"->"wyy","password"->"wyy",
            "dbtable" -> "entitymention")).load()

        val eDF = sqlContext.read.format("jdbc").options(
          Map("url" -> "jdbc:mysql://202.117.16.118:3306/net","user"->"wyy","password"->"wyy",
            "dbtable" -> "entity")).load()

        val jdbcDF = emDF.join(eDF,(emDF("name")===eDF("name"))and(emDF("topic_id")===eDF("topic_id")),"left")
        val colArray=Array("value","start_time","end_time","entity_id","type","topic_id","entity.insert_time")
        val result = jdbcDF.dropDuplicates(colArray)

        val data = result.map(row =>
          (row.getString(row.fieldIndex("id")),
            row.getInt(row.fieldIndex("data_type")),
            row.getString(row.fieldIndex("title")),
            row.getString(row.fieldIndex("content")),
            row.getInt(row.fieldIndex("topic_id")),
            row.getTimestamp(row.fieldIndex("post_time"))
            )
        )

        //emDF.registerTempTable("entitymention")
        //eDF.registerTempTable("entity")
        //sqlContext.sql("UPDATE entitymention, entity SET entitymention.entity_id=entity.entity_id,entitymention.update_time=NOW() WHERE (entitymention.name LIKE entity.name) AND (entitymention.topic_id=entity.topic_id)")
    */

  }

}
