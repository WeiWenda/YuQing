package org.apache.scala.entity_extract
/**
  * Created by wyy on 11/20/15.
  */

import java.sql.{PreparedStatement, Timestamp, DriverManager, Connection}
import java.util.Properties
import com.hankcs.hanlp.seg.CRF.CRFSegment
import com.hankcs.hanlp.seg.Segment
import com.hankcs.hanlp.tokenizer.IndexTokenizer
import com.mysql.jdbc.Driver
import org.apache.spark.sql.{DataFrame, SaveMode, SQLContext}
import org.apache.spark.sql.functions._
import org.apache.spark.SparkContext
import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.corpus.tag.Nature

import scala.collection.mutable.ArrayBuffer

object GetEntityMention {

  //var topic: Iterable[(String, String, Timestamp, Int,Int)] = _

  case class enm(id: Int, name: String, entity_type: Int, Hbase_id: String, topic_id: Int, text_src: Int, post_type: Int, offset_start: Int, offset_end: Int, sen_index: Int, sen_offset: Int, post_time: Timestamp, update_time: Timestamp, insert_time: Timestamp)

  def getEntityMention(sqlContext: SQLContext, sc: SparkContext, postDF: DataFrame,post_topicDF: DataFrame):DataFrame={
    /*
    val jdbcDF1 = sqlContext.read.format("jdbc").options(
      Map("url" -> "jdbc:mysql://192.168.199.11:3306/net", "user" -> "wyy", "password" -> "wyy",
        "dbtable" -> "post")).load()

    val jdbcDF2 = sqlContext.read.format("jdbc").options(
      Map("url" -> "jdbc:mysql://192.168.199.11:3306/net", "user" -> "wyy", "password" -> "wyy",
        "dbtable" -> "post_topic")).load()
    */
    //val jdbcDF4 = sqlContext.read.format("jdbc").options(
    //  Map("url" -> "jdbc:mysql://192.168.199.11:3306/net", "user" -> "wyy", "password" -> "wyy",
    //    "dbtable" -> "topic")).load()
    //val topic_list = jdbcDF4.map(row=>row.getInt(row.fieldIndex("topic_id"))).collect().toList
    /*
        var conn:Connection = null
        val d :Driver = null
        var pstmt:PreparedStatement = null
        val url="jdbc:mysql://202.117.16.118:3306/net?useUnicode=true&characterEncoding=utf-8"
        val user="wyy"
        val password="wyy"
        conn = DriverManager.getConnection(url, user, password)
    */
    val post_topicDFF= post_topicDF.select("post_id", "topic_id","similarity")
    val jdbcDF3 = postDF.join(post_topicDFF, postDF("id") === post_topicDFF("post_id"), "left")
    val jdbcDF = jdbcDF3.filter((jdbcDF3("language_type") === 0)and(jdbcDF3("choice")===1))
    //val re = jdbcDF.join(jdbcDF4,jdbcDF("topic_id")===jdbcDF4("topic_id"),"inner")

    val data = jdbcDF.map(row =>
      (row.getString(row.fieldIndex("id")),
        row.getInt(row.fieldIndex("data_type")),
        row.getString(row.fieldIndex("title")),
        row.getString(row.fieldIndex("content")),
        row.getInt(row.fieldIndex("topic_id")),
        row.getTimestamp(row.fieldIndex("post_time"))
        )
    )

    var entitymention_list: ArrayBuffer[EntityMention] = new ArrayBuffer[EntityMention]()
    var em_id = 1

    data.collect().foreach(
      c=>{
        var tilte_sen = c._3.split("[，.?;!,。？；！]+")
//        split("[,.，。！？......?!;；]")// "。", "！", "？", "，", "；", ",", ".", "!", "?", ";"
        var base_offset = 0

        for(j <- 0 to tilte_sen.length-1){
          var segment:Segment = new CRFSegment()//.newSegment()
          segment.enableOffset(true)
          segment.enableTranslatedNameRecognize(true)
          segment.enableAllNamedEntityRecognize(true)
          val title_terms = segment.seg(tilte_sen(j))
          for (i <- 0 to title_terms.size()-1) {
            if((title_terms.get(i).nature==Nature.nr)||(title_terms.get(i).nature==Nature.ns)||(title_terms.get(i).nature==Nature.nt)) {
              var entity_mention:EntityMention = new EntityMention()
              entity_mention.id = em_id//.localValue
              entity_mention.name = title_terms.get(i).word
              entity_mention.Hbase_id = c._1
              entity_mention.text_src = c._2
              entity_mention.topic_id = c._5
              entity_mention.post_time = c._6
              entity_mention.post_type = 1
              var cur_time: Timestamp = new Timestamp(System.currentTimeMillis())
              entity_mention.insert_time = cur_time
              if(title_terms.get(i).nature==Nature.nr) entity_mention.entity_type = 1
              else if (title_terms.get(i).nature==Nature.nt) entity_mention.entity_type = 2
              else entity_mention.entity_type = 3
              entity_mention.sen_index = j
              entity_mention.offset_start = base_offset + title_terms.get(i).offset
              entity_mention.offset_end = entity_mention.offset_start + title_terms.get(i).length()
              entity_mention.sen_offset = title_terms.get(i).offset

              em_id += 1
              entitymention_list += entity_mention
              //InsertDB.insertEntityMention(entity_mention)
              //println(entity_mention.name+" "+entity_mention.entity_type+" "+entity_mention.post_time+" "+entity_mention.post_type)
            }
          }
          base_offset = base_offset + tilte_sen(j).length + 1
        }

        var con_sen = c._4.split("[，.?;!,。？；！]+")
//        var con_sen = c._4.split("[,.，。！？......?!;；]")
        base_offset = 0
        for(j <- 0 to con_sen.length-1){
          var segment:Segment = new CRFSegment()//.newSegment()
          segment.enableOffset(true)
          segment.enableTranslatedNameRecognize(true)
          segment.enableAllNamedEntityRecognize(true)
          val title_terms = segment.seg(con_sen(j))
          for (i <- 0 to title_terms.size()-1) {
            if((title_terms.get(i).nature==Nature.nr)||(title_terms.get(i).nature==Nature.ns)||(title_terms.get(i).nature==Nature.nt)) {
              var entity_mention:EntityMention = new EntityMention()
              entity_mention.id = em_id//.localValue
              entity_mention.name = title_terms.get(i).word
              entity_mention.Hbase_id = c._1
              entity_mention.text_src = c._2
              entity_mention.topic_id = c._5
              entity_mention.post_time = c._6
              entity_mention.post_type = 2
              var cur_time: Timestamp = new Timestamp(System.currentTimeMillis())
              entity_mention.insert_time = cur_time
              if(title_terms.get(i).nature==Nature.nr) entity_mention.entity_type = 1
              else if (title_terms.get(i).nature==Nature.nt) entity_mention.entity_type = 2
              else entity_mention.entity_type = 3
              entity_mention.sen_index = j
              entity_mention.offset_start = base_offset + title_terms.get(i).offset
              entity_mention.offset_end = entity_mention.offset_start + title_terms.get(i).length()
              entity_mention.sen_offset = title_terms.get(i).offset

              em_id += 1
              entitymention_list += entity_mention
              //InsertDB.insertEntityMention(entity_mention)
              //println(entity_mention.name+" "+entity_mention.entity_type+" "+entity_mention.post_time+" "+entity_mention.post_type)
            }
          }
          base_offset = base_offset + con_sen(j).length + 1
        }
      }
    )

    println(em_id+"-------EM----------")
    //println(entitymention_list)
    //println(q)
    //return entitymention_list

    import sqlContext.implicits._
    val enmrdd = sc.parallelize(entitymention_list,24)
    val enmDF = enmrdd.map{p=> enm(p.id,p.name,p.entity_type,p.Hbase_id,p.topic_id,p.text_src,p.post_type,p.offset_start,p.offset_end,p.sen_index,p.sen_offset,p.post_time,p.update_time,p.insert_time)}.toDF()
    //val property = new Properties()
    //val url = "jdbc:mysql://202.117.16.118:3306/net?useUnicode=true&characterEncoding=utf-8"
    //property.put("user","wyy")
    //property.put("password", "wyy")
    //enmDF.write.mode(SaveMode.Append).jdbc(url, "entitymention", property)

    return enmDF
  }

}
